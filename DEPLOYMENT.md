# Deployment Guide

本文档用于从零搭建并部署 `vector-repository` 后端服务。

## 1. 目标环境

建议环境：

- Windows 10/11 或 Linux
- Java 17
- Maven 3.9+
- Redis 7+
- Elasticsearch 8+
- 可用的 OpenAI 兼容聊天模型服务
- 可选：Ollama，本地 embedding 使用

## 2. 获取项目

```powershell
git clone <your-repository-url>
cd vector-repository
```

## 3. 准备基础依赖

### Redis

默认配置见 `src/main/resources/application-redis.yaml`：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 5s
```

启动后可检查：

```powershell
redis-cli ping
```

### Elasticsearch

默认配置见 `src/main/resources/application-es.yaml`：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

启动后可检查：

```powershell
curl http://127.0.0.1:9200
```

### 聊天模型 API

编辑或覆盖 `src/main/resources/application-api.yaml` 中的占位项，推荐使用环境变量：
当前项目目录没有提交的 `application-api.yaml` 请先自行创建到 `src/main/resources/` 目录下。

```yaml
spring:
  ai:
    model:
      chat: openai
      embedding: ollama

    openai:
      # OpenAI 兼容服务地址，例如阿里云百炼、硅基流动、自建兼容网关等
      base-url: ${OPENAI_BASE_URL:https://your-openai-compatible-base-url}
      # 请通过环境变量或本地私有配置覆盖，禁止提交真实密钥
      api-key: ${OPENAI_API_KEY:replace-with-your-api-key}

      chat:
        options:
          model: ${OPENAI_CHAT_MODEL:replace-with-your-chat-model}

      embedding:
        options:
          # 如果你的 embedding 也走 OpenAI 兼容接口，可改为对应模型名
          model: ${OPENAI_EMBEDDING_MODEL:text-embedding-v4}

    ollama:
      base-url: ${OLLAMA_BASE_URL:http://127.0.0.1:11434}
      embedding:
        options:
          model: ${OLLAMA_EMBEDDING_MODEL:qwen3-embedding:0.6b}

```

```powershell
$env:OPENAI_BASE_URL="https://your-openai-compatible-base-url"
$env:OPENAI_API_KEY="your-api-key"
$env:OPENAI_CHAT_MODEL="your-chat-model"
$env:OPENAI_EMBEDDING_MODEL="text-embedding-v4"
$env:OLLAMA_BASE_URL="http://127.0.0.1:11434"
$env:OLLAMA_EMBEDDING_MODEL="qwen3-embedding:0.6b"
```

### Ollama（可选）

如果 embedding 走本地 Ollama，先安装并启动 Ollama，然后拉取模型：

```powershell
ollama pull qwen3-embedding:0.6b
ollama list
ollama ps
```
### 或者直接添加在 `application-api.yaml` 中配置


## 4. 检查提示词配置

当前聊天、摘要、摘要再压缩提示词在 `src/main/resources/application-prompt.yaml` 中配置。

至少要保证这几个键存在：

- `app.chat.chat-system-prompt`
- `app.summary.summary-system-prompt`
- `app.merge.summary-merge-prompt`

如果缺失，聊天摘要链路会在运行时报配置异常。

## 5. 检查上传文件限制

默认配置：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

如果要放宽限制，请修改 `application-fileupload.yaml`。

## 6. 本地启动

### 方式一：直接运行

```powershell
mvn spring-boot:run
```

### 方式二：打包后运行

```powershell
mvn clean package
java -jar target/vector-repository-0.0.1-SNAPSHOT.jar
```

## 7. 启动后检查项

### 基础检查

- 端口是否成功监听：默认 `8080`
- 日志中是否出现 Spring Boot 启动完成信息
- Redis 是否可连接
- Elasticsearch 是否可连接
- 聊天模型 API 是否可连通

### 功能检查建议

1. 创建会话
2. 发起一次普通流式聊天
3. 测试停止对话
4. 上传一份知识库文档
5. 测试启用知识库后的聊天
6. 测试会话历史查询
7. 测试回滚最后一轮
8. 测试多轮对话后摘要压缩是否触发

## 8. 生产部署建议

### 配置管理

- 不要把真实密钥写进仓库
- 生产环境优先用环境变量、配置中心或外部挂载配置
- 建议将 `application-api.yaml` 保持为占位模板

### 进程管理

可以用以下任一方式管理进程：

- Windows 服务
- NSSM
- systemd
- Docker
- Kubernetes

### 日志与目录

建议关注这些目录：

- `logs/`
- `.runtime/`
- `target/`

建议把运行时临时目录和日志目录都加入清理策略。

### 资源建议

- Redis：会话、历史、摘要存储依赖 Redis，建议稳定可用
- Elasticsearch：向量检索依赖 ES，建议单独监控
- Ollama：若本地 embedding 模型较慢，优先评估是否改成远程 embedding API

## 9. 常见问题

### 聊天接口返回系统异常

优先检查：

- 上游聊天模型是否可访问
- API Key 是否正确
- 请求是否被手动停止后又被误判为异常

### 上传文档失败

优先检查：

- 文件大小是否超限
- Elasticsearch 是否可写
- embedding 模型是否正常响应
- 上传任务是否卡在异步处理阶段

### 项目无法关闭或重启卡住

优先检查：

- 是否仍有未结束的流式请求
- 是否有异步线程未释放
- 外部模型调用是否长时间阻塞

## 10. 后续可扩展方向

- 接入配置中心统一管理模型配置
- 把摘要链路做成可观测任务
- 为知识库上传增加进度查询和失败重试
- 为聊天链路增加监控指标和追踪
