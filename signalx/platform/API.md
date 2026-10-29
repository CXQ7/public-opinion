# 模块对接（AI层）

本服务提供两个入口：

- `/`：网页端（手动输入 + 切换模型）
- `/api/sentiment`：给爬虫/后端模块调用的稳定 JSON API（推荐对接这个）
- `/api/model`：读取/设置当前默认模型（网页端切换会同步到这里，爬虫可不传 `ai_index` 跟随默认值）
- `/api/board`：爬虫实时看板（最近调用记录；仅用于观测/调试）

## 1) 接口：POST /api/sentiment

### 请求（JSON）

方式 A：单条标题（推荐简单场景）
```json
{
  "title": "Public Opinion",
  "ai_index": 1
}
```

方式 B：多条标题（批处理，推荐爬虫入库前批量打标）
```json
{
  "titles": [
    "Public Opinion举办春季招聘会",
    "某公司被处罚",
    "中国经济增长预期上调"
  ],
  "ai_index": 1
}
```

字段说明：
- `ai_index`：选择模型/供应商（与网页下拉一致，可选；不传则使用服务端默认模型）
  - `0` = deepseek
  - `1` = openrouter
- `title`：单条标题字符串
- `titles`：标题字符串数组（与返回 `results` 一一对应）

### 响应（JSON）

成功（HTTP 200）：
```json
{
  "ok": true,
  "provider": "openrouter",
  "model": "openrouter/auto",
  "results": ["NEUTRAL", "NEGATIVE", "POSITIVE"]
}
```

失败（HTTP 4xx/5xx/502）：
```json
{
  "ok": false,
  "error": "错误原因",
  "provider": "openrouter",
  "model": "openrouter/auto",
  "base_url": "https://openrouter.ai/api/v1"
}
```

## 2) 与网页端的关系

- 网页端仍然使用 `/ask`，主要用于演示与手动测试。
- 爬虫/后端模块对接请使用 `/api/sentiment`，因为它返回结构化 JSON，避免解析聊天字符串。

## 3) 接口：GET/POST /api/model

### GET /api/model

返回当前默认模型（HTTP 200）：
```json
{
  "ok": true,
  "ai_index": 0,
  "provider": "deepseek",
  "model": "deepseek-chat",
  "options": [
    {"ai_index": 0, "provider": "deepseek", "model": "deepseek-chat"},
    {"ai_index": 1, "provider": "openrouter", "model": "openrouter/auto"}
  ]
}
```

### POST /api/model

设置默认模型：
```json
{"ai_index": 1}
```

说明：网页端下拉切换会自动 POST 这个接口；如果爬虫调用 `/api/sentiment` 不传 `ai_index`，就会跟随这里的默认值。

## 4) 接口：GET /api/board

用于“爬虫实时看板”，返回最近 N 条 `/api/sentiment` 的请求/响应记录。

请求：
- `GET /api/board?limit=30`

响应（HTTP 200）：
```json
{
  "ok": true,
  "items": [
    {
      "ts": "2026-03-20T02:16:24.241+00:00",
      "route": "/api/sentiment",
      "ai_index": 0,
      "provider": "deepseek",
      "model": "deepseek-chat",
      "ok": true,
      "titles": ["标题1", "标题2"],
      "results": ["NEUTRAL", "POSITIVE"],
      "error": null
    }
  ]
}
```

说明：
- `ts` 为服务端记录的调用时间（UTC 的 ISO8601 字符串）；前端可按本地时区显示。

## 5) 调用示例

### curl

单条：
```bash
curl -X POST http://127.0.0.1:5000/api/sentiment \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Public Opinion\",\"ai_index\":1}"
```

批量：
```bash
curl -X POST http://127.0.0.1:5000/api/sentiment \
  -H "Content-Type: application/json" \
  -d "{\"titles\":[\"标题1\",\"标题2\"],\"ai_index\":1}"
```

### Python requests（爬虫侧）
```python
import requests

payload = {
    "titles": ["标题1", "标题2"],
    "ai_index": 1,
}
resp = requests.post("http://127.0.0.1:5000/api/sentiment", json=payload, timeout=30)
resp.raise_for_status()
print(resp.json())
```

## 6) 约束与约定

- `results` 必须与输入标题数量一致，否则服务会返回错误并带上 `raw/parsed`（用于排查模型未按要求输出 JSON 的情况）。
- 建议爬虫侧做重试（网络/限流/超时），并在失败时记录 `error` 与 `provider/model`。

补充：
- `POST /api/model` 设置成功后，会返回最新的默认模型状态（与 `GET /api/model` 同结构）。
