# vector-repository

基于 Spring Boot 3、Spring AI、Elasticsearch 和 Redis 的知识库问答后端项目，当前已实现流式对话、会话管理、聊天历史归档、短期聊天记忆、文档向量化上传、RAG 检索增强、历史摘要压缩与摘要再压缩。

## 功能概览

- 流式聊天：支持 `data`、`reasoning`、`stop`、`interrupted`、`error` 事件
- 会话管理：创建、查询、重命名、删除、分页查询历史、回滚最后一轮
- 聊天记忆：基于 `MessageWindowChatMemory` 的短期上下文记忆
- 历史归档：完整保存用户与助手消息，支持后续分页和摘要压缩
- 对话停止：支持前端手动停止，并区分首 token 前/后的中断处理
- 历史摘要：按阈值压缩新增未压缩历史，正常完成后异步触发
- 摘要再压缩：摘要条数达到阈值后，自动压缩较早摘要，控制摘要总量
- 知识库上传：支持 `txt`、`pdf`、`ppt`、`pptx`
- RAG 检索：按 `knowledgeBaseId` 过滤检索，支持 `topK` 和 `similarityThreshold`
- 向量存储：基于 Elasticsearch
- 会话/历史存储：基于 Redis

## 技术栈

- Java 17
- Spring Boot 3.5.x
- Spring AI 1.1.x
- OpenAI 兼容聊天模型
- Ollama 本地 embedding 模型
- Elasticsearch
- Redis
- Apache PDFBox
- Apache POI

## 当前目录结构

```text
src/main/java/io/github/halcyonsong
├─ chat
│  ├─ common
│  │  ├─ config
│  │  ├─ constants
│  │  └─ enums
│  ├─ session
│  │  ├─ controller
│  │  ├─ pojo
│  │  └─ service
│  ├─ stream
│  │  ├─ controller
│  │  ├─ pojo
│  │  └─ service
│  └─ summary
│     ├─ pojo
│     └─ service
├─ common
│  ├─ aspect
│  ├─ exception
│  └─ result
└─ knowledge
   ├─ common
   ├─ controller
   ├─ enums
   ├─ pojo
   └─ service
```

## 配置文件

项目当前按职责拆分配置：

- `src/main/resources/application.yaml`
- `src/main/resources/application-api.yaml`
- `src/main/resources/application-es.yaml`
- `src/main/resources/application-redis.yaml`
- `src/main/resources/application-fileupload.yaml`
- `src/main/resources/application-log.yaml`
- `src/main/resources/application-prompt.yaml`

主要配置职责如下：

- `application.yaml`：应用名、端口、profile 聚合、优雅停机
- `application-api.yaml`：聊天模型与 embedding 模型接入配置
- `application-es.yaml`：Elasticsearch 与向量存储配置
- `application-redis.yaml`：Redis 配置
- `application-fileupload.yaml`：文件上传大小限制
- `application-log.yaml`：日志配置
- `application-prompt.yaml`：聊天提示词、摘要提示词、摘要再压缩提示词

## 快速开始

### 1. 环境准备

启动以下依赖：

1. Redis
2. Elasticsearch
3. 聊天模型 API 服务
4. 如果 embedding 走本地 Ollama，再启动 Ollama 并拉取 embedding 模型

### 2. 检查配置

最少需要确认：

- `application-api.yaml`
- `application-es.yaml`
- `application-redis.yaml`
- `application-prompt.yaml`

其中 `application-api.yaml` 建议只保留占位内容，通过环境变量或本地私有配置覆盖。

### 3. 启动项目

```powershell
mvn spring-boot:run
```

或：

```powershell
mvn clean package
java -jar target/vector-repository-0.0.1-SNAPSHOT.jar
```

## 本地依赖检查

### Redis

```powershell
redis-cli ping
```

期望返回：

```text
PONG
```

### Elasticsearch

```powershell
curl http://127.0.0.1:9200
```

### Ollama

```powershell
ollama list
ollama ps
ollama pull qwen3-embedding:0.6b
```

## 核心接口

### 聊天

- `POST /ai/chat/stream`：流式聊天
- `POST /ai/chat/stop`：停止当前会话生成

### 会话

- `POST /ai/session/create`：创建会话
- `GET /ai/session/list`：查询会话列表
- `GET /ai/session/get`：查询单个会话
- `DELETE /ai/session/delete`：删除会话
- `POST /ai/session/rename`：重命名会话
- `GET /ai/session/history`：分页查询会话历史
- `POST /ai/session/rollback`：回滚最后一轮对话

### 知识库

- `POST /ai/kb/upload`：上传文档并异步向量化
- `GET /ai/kb/task`：查询上传任务状态
- `GET /ai/kb/list`：查询知识库列表
- `DELETE /ai/kb/delete`：删除知识库

## 聊天链路说明

### 流式对话链路

1. 前端请求进入 `ChatController`
2. `ChatServiceImpl` 规范化参数并组装系统提示词
3. 根据 `sessionId` 拼接历史摘要上下文
4. 根据请求决定是否启用知识库检索增强
5. 先写入用户历史消息
6. 启动流式聊天请求
7. 流式输出结束后统一进入生命周期处理
8. 正常完成时异步触发历史摘要压缩

### 摘要链路

1. 正常完成一轮对话后，异步检查是否达到摘要压缩阈值
2. 若未压缩历史达到阈值，则压缩为一条新摘要
3. 摘要条数达到上限后，再压缩较早摘要，减少摘要总量
4. 下一轮对话开始时，把当前摘要列表拼接进系统提示词
5. 若会话回滚，则同步重对齐摘要列表与摘要游标

## 文件上传说明

默认限制见 `application-fileupload.yaml`：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

当前支持：

- `txt`
- `pdf`
- `ppt`
- `pptx`

## 部署文档

从零部署说明见：

- [DEPLOYMENT.md](DEPLOYMENT.md)

## 安全说明

- 不要提交真实 API Key
- `src/main/resources/application-api.yaml` 建议始终保留占位配置
- 如果历史上曾提交过真实密钥，应立即轮换
- 不要提交 `target/`、`logs/`、`.runtime/`、`frontend/node_modules/`、`frontend/dist/`

## 当前建议的联调顺序

1. 启动 Redis 和 Elasticsearch
2. 配置聊天模型 API
3. 若需本地 embedding，启动 Ollama
4. 创建会话并测试普通流式聊天
5. 测试停止对话
6. 测试知识库上传与检索
7. 测试历史摘要压缩与回滚后的摘要对齐
