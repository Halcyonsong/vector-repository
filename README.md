# vector-repository

基于 Spring Boot 3 与 Spring AI 的知识库问答后端，当前已落地的核心能力包括：流式对话、会话管理、Redis 聊天记忆、聊天历史归档、知识库文档上传与向量化、基于 Elasticsearch 的检索增强问答。

## 技术栈

- Java 17
- Spring Boot 3.5.x
- Spring AI 1.1.x
- OpenAI 兼容聊天模型
- Ollama 本地向量模型
- Elasticsearch 向量存储
- Redis 会话与历史存储
- PDFBox / Apache POI 文档解析

## 当前能力

- 流式聊天：支持文本输出、思考事件、停止事件、中断事件、错误事件
- 会话管理：创建、查询、重命名、删除、历史消息查询
- 会话记忆：基于 `MessageWindowChatMemory` 的短期上下文记忆
- 历史归档：完整保存用户与助手消息，便于后续分页或滑动窗口扩展
- 知识库上传：支持 `txt`、`pdf`、`ppt`、`pptx`
- RAG 检索：按 `knowledgeBaseId` 过滤检索，支持 `topK` 与 `similarityThreshold`
- 全局异常处理：统一包装普通业务异常响应
- Web 日志：普通控制器请求日志记录，流式接口可单独处理

## 目录结构

```text
src/main/java/io/github/halcyonsong
├─ chat
│  ├─ config
│  ├─ controller
│  ├─ enums
│  ├─ pojo
│  ├─ service
│  └─ support
├─ common
│  ├─ aspect
│  ├─ config
│  ├─ enums
│  ├─ exception
│  └─ result
└─ knowledge
   ├─ config
   ├─ constants
   ├─ controller
   ├─ enums
   ├─ service
   └─ util
```

## 配置说明

主配置文件：

- `src/main/resources/application.yaml`
- `src/main/resources/application-api.yaml`
- `src/main/resources/application-es.yaml`
- `src/main/resources/application-redis.yaml`
- `src/main/resources/application-fileupload.yaml`
- `src/main/resources/application-log.yaml`

`application-api.yaml` 已改为占位写法，推荐通过环境变量注入：

```powershell
$env:OPENAI_BASE_URL="https://api.siliconflow.cn"
$env:OPENAI_API_KEY="your-api-key"
$env:OPENAI_CHAT_MODEL="deepseek-v4-pro"
$env:OLLAMA_BASE_URL="http://127.0.0.1:11434"
$env:OLLAMA_EMBEDDING_MODEL="qwen3-embedding:0.6b"
$env:APP_CHAT_SYSTEM_PROMPT="you are a helpful assistant"
```

## 本地启动前提

1. 启动 Redis
2. 启动 Elasticsearch
3. 如果向量化走本地模型，启动 Ollama，并确保已拉取 embedding 模型
4. 配置聊天模型 API Key

常用 Ollama 命令：

```powershell
ollama list
ollama ps
ollama pull qwen3-embedding:0.6b
ollama stop qwen3-embedding:0.6b
```

## 启动方式

```powershell
mvn spring-boot:run
```

或先打包再运行：

```powershell
mvn clean package
java -jar target/vector-repository-0.0.1-SNAPSHOT.jar
```

## 主要接口

### 聊天

- `POST /ai/chat/stream`：流式聊天
- `POST /ai/chat/stop`：手动中断会话

### 会话

- `POST /ai/session/create`：创建会话
- `GET /ai/session/list`：查询会话列表
- `GET /ai/session/get`：查询单个会话
- `DELETE /ai/session/delete`：删除会话
- `POST /ai/session/rename`：重命名会话
- `GET /ai/session/history`：查询会话历史消息

### 知识库

- `POST /ai/kb/upload`：上传文档并向量化
- `DELETE /ai/kb/delete`：删除知识库
- `GET /ai/kb/list`：查询知识库列表

## 提交前建议

- 不要提交真实 API Key
- 不要提交 `target/`、`logs/`、`frontend/node_modules/`、`frontend/dist/`
- 如果确认项目要开始版本管理，先检查当前 `.git` 目录是否有效；若 `git status` 仍报错，需要重新执行 `git init`
