# Core Redis 队列对接开发指南

## 概述

`core.redis.queue` 模块提供了一套完整的基于 Redis List 的异步消息队列系统，供 `record` 和 `report` 模块对接。
本地开发redis请看 https://bcnni9qq7fx2.feishu.cn/wiki/VpZ4wzLazidY0OkNN1zcdICCnnb

**核心能力：**
- 异步消息入队与消费（替代高频数据库轮询）
- 自动重试（默认最多 3 次）
- 死信队列（DLQ）兜底
- AOP 幂等保护（防止消息被重复处理）
---

## 一、Redis Key 结构

```
signalx:dev:queue:{topic}   # 主队列 (Redis List)，存储消息 ID
signalx:dev:msg:{messageId} # 详情池 (Redis String)，存储完整消息体，7 天 TTL
signalx:dev:dlq:{topic}     # 死信队列 (Redis List)，存储失败消息
signalx:idempotent:{msgId}  # 幂等锁 (Redis String)，默认 24 小时 TTL
```

---

## 二、Topic 命名规范

| 模块 | 推荐 Topic 名称 | 用途说明 |
|------|----------------|----------|
| record | `news.crawl` | 爬虫抓取新闻任务 |
| record | `news.index` | 新闻建立索引任务 |
| report | `report.generate` | 报告生成任务 |
| report | `report.sync` | 报告状态同步任务 |

> Topic 名称全部小写，用 `.` 分隔，与 Redis Key 拼接后形如 `signalx:dev:queue:report.generate`。

---

## 三、核心 API

### 3.1 `QueueMessage<T>` — 消息协议体

所有消息必须封装在此类中，**不允许直接向 Redis 写裸数据**。

```java
// 快速创建消息（自动填充 messageId、timestamp、traceId）
QueueMessage<Long> message = QueueMessage.create("report.generate", reportId);

// 如需自定义最大重试次数（默认 3 次）
message.setMaxRetry(5);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | `String` | 自动生成的 UUID，用于幂等和追踪 |
| `topic` | `String` | 队列主题 |
| `payload` | `T` | 业务数据，建议传 ID（如 `Long reportId`），不传大对象 |
| `retryCount` | `int` | 当前已重试次数，框架自动维护，业务勿修改 |
| `maxRetry` | `int` | 最大重试次数，默认 3 |
| `traceId` | `String` | 链路追踪 ID，用于日志关联 |

### 3.2 `RedisQueue` — 队列操作接口

注入方式：
```java
@Autowired
private RedisQueue redisQueue;
```

#### `push` — 生产者入队

```java
// 生产者（任务触发处调用）
QueueMessage<Long> msg = QueueMessage.create("report.generate", reportId);
redisQueue.push(msg);
```

#### `pop` — 消费者出队

```java
// 消费者（定时任务或监听器中调用）
Optional<QueueMessage<?>> optional = redisQueue.pop("report.generate");
optional.ifPresent(msg -> {
    Long reportId = (Long) msg.getPayload();
    // ... 执行业务逻辑
});
```

#### `ack` — 确认成功（必须调用）

业务处理成功后**必须调用**，否则详情池数据永远不会清理，造成 Redis 内存泄漏。

```java
redisQueue.ack(msg.getMessageId());
```

#### `nack` — 确认失败（触发重试）

业务处理抛出异常时调用，框架自动决定是重试还是进入 DLQ。

```java
try {
    // 业务逻辑
    redisQueue.ack(msg.getMessageId());
} catch (Exception e) {
    redisQueue.nack(msg, e);
}
```

**重试语义**（maxRetry=3 时）：

| nack 第几次 | retryCount 变为 | 结果 |
|------------|----------------|------|
| 第 1 次 | 1 | 重新入队等待消费 |
| 第 2 次 | 2 | 重新入队等待消费 |
| 第 3 次 | 3 | 进入死信队列，触发 `[DLQ-ALARM]` 日志 |

---

## 四、`@Idempotent` 幂等注解

在消费者方法上加此注解，可防止同一条消息被重复处理（例如服务重启、网络重试导致的重复消费）。

```java
@Idempotent(expireSeconds = 3600)  // 1 小时内相同 messageId 不会被重复执行
public void processReportTask(QueueMessage<Long> message) {
    // 业务逻辑
}
```

**原理**：拦截器在执行前向 Redis 写入 `SETNX signalx:idempotent:{messageId}`。如果已存在，则跳过执行并静默返回。

> **注意**：方法的第一个参数必须是 `QueueMessage<?>` 类型，拦截器从中提取 `messageId`。

---

## 五、完整对接模板

### 5.1 Report 模块对接示例

**生产者**（触发报告生成的地方）：
```java
// 在 ReportService 或 Controller 中，任务提交时入队
QueueMessage<Long> msg = QueueMessage.create("report.generate", reportId);
redisQueue.push(msg);
log.info("报告生成任务已入队 | reportId: {}, msgId: {}", reportId, msg.getMessageId());
```

**消费者**（替代原有的 5 秒轮询定时任务）：
```java
@Scheduled(fixedDelay = 1000)  // 每秒轮询一次
public void consumeReportTask() {
    Optional<QueueMessage<?>> optional = redisQueue.pop("report.generate");
    if (optional.isEmpty()) return;

    QueueMessage<?> msg = optional.get();
    try {
        processReportTask(msg);
        redisQueue.ack(msg.getMessageId());
    } catch (Exception e) {
        log.error("报告生成失败 | msgId: {}", msg.getMessageId(), e);
        redisQueue.nack(msg, e);
    }
}

@Idempotent(expireSeconds = 3600)
public void processReportTask(QueueMessage<?> message) {
    Long reportId = (Long) message.getPayload();
    // 具体业务逻辑...
}
```

### 5.2 Record 模块对接示例

**生产者**（爬虫任务分发时入队）：
```java
QueueMessage<Long> msg = QueueMessage.create("news.crawl", taskId);
redisQueue.push(msg);
```

**消费者**：
```java
@Scheduled(fixedDelay = 500)
public void consumeNewsCrawlTask() {
    Optional<QueueMessage<?>> optional = redisQueue.pop("news.crawl");
    if (optional.isEmpty()) return;

    QueueMessage<?> msg = optional.get();
    try {
        processCrawlTask(msg);
        redisQueue.ack(msg.getMessageId());
    } catch (Exception e) {
        redisQueue.nack(msg, e);
    }
}
```

---

## 六、运维 API

### 查看死信积压数量
```http
GET /api/v1/admin/queue/dlq/stats?topic=report.generate
```
**响应示例：**
```json
{ "code": 200, "data": 5, "msg": "success" }
```

### 手动重放死信
将死信队列中所有消息重置 retryCount 后重新入主队列：
```http
POST /api/v1/admin/queue/dlq/replay?topic=report.generate
```
**响应示例：**
```json
{ "code": 200, "data": "手动重播已完成，共处理 5 个死信消息", "msg": "success" }
```

---

## 七、配置项（application.yml）

```yaml
signalx:
  queue:
    default-max-retry: 3     # 默认最大重试次数
    idempotent-ttl: 86400    # 幂等锁 TTL（秒），默认 24 小时
    retry-interval: 5        # 重试间隔基础值（秒）
    enable-dlq-alert: true   # 是否开启死信日志告警
```

---

## 八、注意事项

1. **Payload 传 ID，不传大对象**：`payload` 建议只传 `Long` 类型的业务 ID（如 `reportId`、`taskId`），消费者拿到 ID 后自行从数据库查询完整数据，避免大对象序列化开销。

2. **ack/nack 必须二选一**：消费者处理完消息后，成功调 `ack`，异常调 `nack`，两者缺一不可。忘记调用 `ack` 会导致 Redis 详情池数据堆积，忘记调用 `nack` 会导致消息静默丢失。

3. **@Idempotent 不是银弹**：幂等注解依赖 Redis 锁，如果 Redis 重启或锁过期，存在极小概率的重复执行风险。对于财务类、不可逆操作，业务层应额外做数据库唯一键防重。

4. **消费者 fixedDelay 建议值**：
   - 高优先级任务（如报告生成）：500ms ~ 1000ms
   - 普通任务：1000ms ~ 3000ms
   - 低优先级任务（如日志归档）：5000ms 以上

---

## 九、Report 模块优化方案

### 修改点
1. **移除事务中的 HTTP 调用**：
   - **问题**：事务中包含 HTTP 调用，导致数据库连接长时间占用，可能引发连接池耗尽。
   - **方案**：将 HTTP 调用拆分到单独的任务中，通过 Redis 队列异步处理。
   - **原因**：减少数据库连接占用时间，提高系统并发能力。

2. **统一轮询逻辑**：
   - **问题**：存在两种轮询方式，可能导致重复操作。
   - **方案**：使用 Redis 队列的 `report.poll`，由一个任务统一处理轮询。
   - **原因**：避免重复操作，简化逻辑。

3. **使用 Redis 队列替代线程池**：
   - **问题**：线程池任务占用时间过长，可能导致线程池资源不足。
   - **方案**：通过 Redis 队列分段处理任务，避免线程池阻塞。
   - **原因**：提高线程池利用率，避免任务堆积。

---

## 十、Record 模块优化方案

### 修改点
1. **将调度器改为任务分派器**：
   - **问题**：调度器直接执行任务，导致调度线程被阻塞。
   - **方案**：调度器只将任务推送到 Redis 队列 `news.crawl`，任务由消费者并发处理。
   - **原因**：减少调度线程阻塞时间，提高任务处理效率。

2. **增加任务失败的重试机制**：
   - **问题**：任务失败后没有自动重试，可能导致任务丢失。
   - **方案**：使用 Redis 队列的 `nack` 机制，支持自动重试，失败后进入死信队列（DLQ）。
   - **原因**：提高任务可靠性，避免任务丢失。

---

### 10.3 `@Transactional` 传播注意事项

`aggregateTaskUnboundNews` 内部调用了 `newsRepository.save(news)`（含 DB 写操作），**Consumer 方法本身不应加 `@Transactional`**，由 `aggregateTaskUnboundNews` 方法内部自行管理事务边界，确保事务在 HTTP 调用 `crawlerRunner.run()` 之前已提交完毕。

---

## 十一、两模块改造对比总览

| 维度 | Report 模块 | Record 模块 |
|------|------------|------------|
| 核心问题 | `@Transactional` + 外部 HTTP 调用 / 双重轮询竞态 / 线程池耗尽 | 调度线程串行阻塞 / 无精细重试 |
| Topic 数量 | 2 个（`report.generate` + `report.poll`） | 1 个（`news.crawl`） |
| 特殊处理 | `report.poll` "running" 状态需用 push+ack 代替 nack | `aggregateTaskUnboundNews` 事务需在 HTTP 调用前提交 |
| `@Idempotent` 作用 | 防止 `processCompletedReport` 重复执行（COS 重传 / 邮件重发） | 防止同一 taskId 在单次调度周期内被重复消费 |
| 原有代码去向 | 删除 `CompletableFuture`；`syncPendingReports` 降级为孤儿兜底（5 分钟轮询一次） | `crawlerRunner.run` 调用移入 Consumer，调度器只做 push |
| 实施优先级 | ★★★ 优先（HikariPool 连接泄漏影响全局稳定性） | ★★ 次之（当前失败仅丢 warn 日志，影响较局部） |
