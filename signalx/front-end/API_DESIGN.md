# SignalX-backend API 设计方案 v2.0

## 1. 基础约定 (Global Standard)

- **接口前缀**: `/api/v1`
- **通信协议**: HTTP/1.1 or HTTP/2
- **数据格式**: JSON
- **认证方式**:
  - Header: `Authorization: Bearer <token-string>` (标准 Bearer Token)
- **统一响应结构**:
  ```json
  {
    "code": 0,          // 0: 成功, 非0: 失败 (如 4001, 5000)
    "msg": "success",   // 错误提示信息
    "data": { ... }     // 业务数据
  }
  ```
- **分页参数 (List API)**:
  - `page`: 当前页码 (从 1 开始)
  - `size`: 每页条数 (默认 10)
  - **分页响应**: 在 `data` 中返回 `{ "list": [], "total": 100 }`。

---

## 2. 模块 API 清单

### 2.1 用户管理 (User Management)

> **Controller**: `UserController`

| 接口路径                  | Method | 输入参数 (Params/Body)                                                    | 输出参数 (data)                                                                     | 用途         |
| :------------------------ | :----- | :------------------------------------------------------------------------ | :---------------------------------------------------------------------------------- | :----------- |
| **`/api/v1/user/login`**  | POST   | `{ "username": "__CONFIGURE_LOCALLY__", "password": "__CONFIGURE_LOCALLY__" }`                                | `{ "token": "__CONFIGURE_LOCALLY__", "id": "...", "username": "__CONFIGURE_LOCALLY__", "email": "...", "role": "..." }` | 用户登录     |
| **`/api/v1/user/list`**   | GET    | Query: `userId` (验证权限), `page`, `size`                                | `{ "list": [{"id", "username", "email", "role", "status"}], "total": int }`         | 获取用户列表 |
| **`/api/v1/user/add`**    | POST   | `{ "username": "__CONFIGURE_LOCALLY__", "password": "__CONFIGURE_LOCALLY__", "email": "...", "role": "..." }` | `{ "id": "..." }`                                                                   | 新增用户     |
| **`/api/v1/user/update`** | POST   | `{ "id": "...", "username": "__CONFIGURE_LOCALLY__", "email": "(opt)", "role": "(opt)" }` | `true` (Boolean)                                                                    | 更新用户信息 |
| **`/api/v1/user/delete`** | POST   | `{ "id": "..." }`                                                         | `true` (Boolean)                                                                    | 删除用户     |

---

### 2.2 监测任务管理 (Monitoring Tasks)

> **Controller**: `TaskController`

| 接口路径                           | Method | 输入参数 (Params/Body)                                                                         | 输出参数 (data)                                                                                                          | 用途                       |
| :--------------------------------- | :----- | :--------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------- | :------------------------- |
| **`/api/v1/task/create`**          | POST   | `{ "userId": "(opt)", "keywords": [], "startTime": "...", "endTime": "...", "interval": int }` | `{ "taskId": "..." }`                                                                                                    | 创建新的监测任务 (Config)  |
| **`/api/v1/task/list`**            | GET    | Query: `userId`, `page`, `size`                                                                | `{ "list": [{"id", "userId", "keywords[]", "startTime", "endTime", "interval", "status", "createTime"}], "total": int }` | 获取监测任务列表           |
| **`/api/v1/task/{taskId}/update`** | POST   | `{ "keywords": [], "startTime": "...", "endTime": "...", "interval": int }`                    | `true`                                                                                                                   | 更新指定任务的配置         |
| **`/api/v1/task/{taskId}/switch`** | POST   | `{ "enable": true/false }`                                                                     | `{ "status": "ACTIVE/INACTIVE" }`                                                                                        | 启停指定监测任务           |
| **`/api/v1/task/{taskId}/delete`** | POST   | Path: `taskId`                                                                                 | `true`                                                                                                                   | 删除监测任务               |
| **`/api/v1/analysis/slices`**      | GET    | Query: `taskId`, `userId`, `page`, `size`                                                      | `{ "list": [{"id", "taskId", "title", "heat", "sentiment", "summary", "eventTime"}], "total": int }`                     | 获取指定任务下的切片(事件) |

---

### 2.3 Openclaw 定时任务管理 (Openclaw Tasks)

> **Controller**: `OpenclawTaskController`

| 接口路径                                     | Method | 输入参数 (Params/Body)                                                                                       | 输出参数 (data)                                 | 用途                          |
| :------------------------------------------- | :----- | :----------------------------------------------------------------------------------------------------------- | :---------------------------------------------- | :---------------------------- |
| **`/api/v1/openclaw-task/create`**           | POST   | `{ "name": "...", "url": "...", "crawlLogic": "...", "startTime": "...", "endTime": "...", "interval": int }` | `Long` (taskId)                                 | 创建 Openclaw 定时任务        |
| **`/api/v1/openclaw-task/list`**             | GET    | Query: `page`, `size`                                                                                        | `{ "list": [OpenclawTaskVO], "total": long }`   | 获取用户的 Openclaw 任务列表 |
| **`/api/v1/openclaw-task/{taskId}/update`**  | POST   | `{ "name": "...", "url": "...", "crawlLogic": "...", "interval": int }`                                     | `true`                                          | 更新 Openclaw 任务配置        |
| **`/api/v1/openclaw-task/{taskId}/switch`**  | POST   | `{ "enable": true/false }`                                                                                   | `{ "status": "ACTIVE/INACTIVE" }`               | 启停 Openclaw 任务            |
| **`/api/v1/openclaw-task/{taskId}/delete`**  | POST   | Path: `taskId`                                                                                               | `true`                                          | 删除 Openclaw 任务            |

---

### 2.4 资讯数据 (News / Record)

> **Controller**: `RecordController`

| 接口路径                | Method | 输入参数 (Params/Body)                                | 输出参数 (data)                                                                                                            | 用途         |
| :---------------------- | :----- | :---------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------- | :----------- |
| **`/api/v1/news/list`** | GET    | Query: `eventId`, `taskId`, `keyword`, `page`, `size` | `{ "list": [{"id", "taskId", "eventId", "title", "url", "content", "publishTime", "source", "sentiment"}], "total": int }` | 获取资讯列表 |

---

### 2.5 报告管理 (Report)

> **Controller**: `ReportController` & `AIController`

| 接口路径                  | Method | 输入参数 (Params/Body)          | 输出参数 (data)                                                                                                        | 用途                     |
| :------------------------ | :----- | :------------------------------ | :--------------------------------------------------------------------------------------------------------------------- | :----------------------- |
| **`/api/v1/report/list`** | GET    | Query: `taskId`, `page`, `size` | `{ "list": [{"id", "taskId", "status", "createTime", "name"}], "total": int }`                                         | 获取指定任务下的报告历史 |
| **`/api/v1/report/{id}`** | GET    | Path: `id` (reportId)           | `{ "id": "...", "name": "...", "summary": "Markdown String...", "taskId": "...", "createTime": "...", "status": int }` | 获取单份报告详情         |
| **`/api/v1/report/{id}`** | DELETE | Path: `id` (reportId)           | `true` (Boolean)                                                                                                       | 删除报告                 |

---

### 2.6 AI 智能问答 (AI Q&A - DeepSeek)

> **Controller**: `AIController`
> **模型**: DeepSeek

| 接口路径                    | Method | 输入参数 (Params/Body)                     | 输出参数 (data)                                                                                                    | 用途                                                                     |
| :-------------------------- | :----- | :----------------------------------------- | :----------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------- |
| **`/api/v1/qa/ask/report`** | POST   | `{ "question": "...", "reportId": "..." }` | `{ "answer": "...", "reasoningContent": "...", "promptTokens": int, "completionTokens": int, "totalTokens": int }` | **基于报告问答**<br>Context: Report 的 Markdown 文本                     |
| **`/api/v1/qa/ask/event`**  | POST   | `{ "question": "...", "eventId": "..." }`  | `{ "answer": "...", "reasoningContent": "...", "promptTokens": int, "completionTokens": int, "totalTokens": int }` | **基于事件链分析**<br>Context: slice 基本信息 + 该 slice 关联的所有 news |

---

## 3. 核心业务流程 (Core Workflows)

### 3.1 舆情自动化生产 (Publicity Data Production)

- **定位**: 数据的采集、清洗与结构化。
- **触发**: 中台定时任务调度爬虫（`publicity_crawler`）或用户新增监控任务（`task`）。
- **流程**:
  1. **增量爬取**: 爬虫根据 `task` 中的关键词组，结合 `source_keyword_offset` 进度表，从抖音、微博、头条等多渠道采集资讯。
  2. **数据标准化**: 原生数据转换为统一的 `news` 实体，包含来源、链接、发布时间等。
  3. **LLM 切片聚类 (Slicing)**:
     - 调用 DeepSeek 等大模型对资讯进行语义聚类。
     - 自动生成 **切片 (Slice)**：包含事件标题、事件摘要、核心时间点。
  4. **量化评估**:
     - **热度 (Heat)**: 综合各渠道传播量计算事件热度。
     - **情感 (Sentiment)**: 判别每条资讯及整个切片的情感倾向（POSITIVE/NEUTRAL/NEGATIVE）。
- **持久化**: 结果存入 `news` 与 `slice` 表。

### 3.2 深度综述报告生成 (Report Synthesis)

- **定位**: 数据的二次加工与洞察生成。
- **触发**: 用户主动触发 或 `task` 计划到达推送时间。
- **流程**:
  1. **数据聚合**: 定位任务关联的所有 `slice` 及高热度 `news` 摘要。
  2. **大模型合成**:
     - 透传数据至 Coze 工作流。
     - 执行趋势分析（历史热度对比）、风险研判（负面情感聚集点）。
  3. **Markdown 渲染**: 生成包含趋势图表占位、结构化章节的深度报告。
- **持久化**: 存入 `report` 表，分配 `coze_run_id` 追踪状态。

### 3.3 交互式智能分析 (AI Interactive Q&A)

- **定位**: 满足深度的个性化内容消费。
- **触发**: 用户在客户端对报告、切片进行提问（DeepSeek 驱动）。
- **Context 检索方式**:
  - **报告模式**: 以 `report.summary` (Markdown) 作为检索上下文。
  - **切片/事件模式**: 检索 `slice` 摘要 + 该切片下所有 `news.content` 的聚合文本。
- **数据跟踪**: 所有对话记录存入 `qa_log`，记录 `reasoning_content`（思维链）和 Token 消耗。

---

### 技术栈

#### 前端技术栈

- **框架**: React + TypeScript
- **构建工具**: Vite
- **状态管理**: Redux
- **样式**: Tailwind CSS
- **测试**: Vitest
- **组件库**: Ant Design

#### 后端技术栈

- **框架**: Spring Boot (Java)
- **任务调度**: Quartz + Redis
- **数据库**: MySQL
- **文件存储**: 腾讯云 COS
- **认证与安全**: OAuth2.0 + JWT
- **API设计**: RESTful API

#### 爬虫技术栈

- **语言**: Python
- **框架**: FastAPI
- **分布式调度**: Celery + Redis
- **动态代理**: proxypool
- **数据清洗**: Pandas

#### AI技术栈

- **模型**: DeepSeek
- **架构**: RAG (检索增强生成)
- **推理链**: CoT (Chain of Thought)
- **多模态支持**: 文本、图片、视频

#### DevOps技术栈

- **容器化**: Docker
- **CI/CD**: GitHub Actions
- **反向代理**: Nginx
- **监控与告警**: Prometheus + Grafana
