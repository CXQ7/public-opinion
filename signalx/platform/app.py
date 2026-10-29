from flask import Flask, render_template, request, jsonify
import json
from threading import Lock
from collections import deque
from datetime import datetime, timezone
from openai import OpenAI

app = Flask(__name__)


_DEFAULT_AI_INDEX_LOCK = Lock()
_DEFAULT_AI_INDEX = 0


_BOARD_LOCK = Lock()
_BOARD_EVENTS = deque(maxlen=200)


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _board_add(event: dict) -> None:
    with _BOARD_LOCK:
        _BOARD_EVENTS.append(event)


def _board_list(limit: int = 50):
    with _BOARD_LOCK:
        items = list(_BOARD_EVENTS)
    if limit <= 0:
        limit = 50
    if limit > 200:
        limit = 200
    return items[-limit:]


def _normalize_ai_index(value, default: int = 0) -> int:
    try:
        idx = int(value)
    except Exception:
        idx = default
    if idx < 0 or idx >= len(AI_CONFIGS):
        idx = default
    return idx


def get_default_ai_index() -> int:
    with _DEFAULT_AI_INDEX_LOCK:
        return _DEFAULT_AI_INDEX


def set_default_ai_index(ai_index: int) -> int:
    global _DEFAULT_AI_INDEX
    ai_index = _normalize_ai_index(ai_index, default=0)
    with _DEFAULT_AI_INDEX_LOCK:
        _DEFAULT_AI_INDEX = ai_index
        return _DEFAULT_AI_INDEX


AI_CONFIGS = (
    {
        "name": "deepseek",
        "api_key": "__CONFIGURE_LOCALLY__",
        "base_url": "https://api.deepseek.com",
        "model": "deepseek-chat"
    },
    {
        "name": "openrouter",
        "api_key": "__CONFIGURE_LOCALLY__",
        # 这里必须是 API 根路径（不要写 /chat/completions）
        "base_url": "https://openrouter.ai/api/v1",
        # OpenRouter 模型 ID：建议先用 openrouter/auto，确认可用后再换成控制台里的具体模型 ID
        "model": "openrouter/auto",
        # OpenRouter 推荐带上站点信息（可选，但建议保留）
        "headers": {
            "HTTP-Referer": "http://127.0.0.1:5000",
            # 注意：HTTP Header 需要 ASCII，不能写中文，否则会触发 ascii codec 错误
            "X-Title": "Sentiment-Analysis-UI"
        }
    }
)
def call_ai_service(title_list_str, ai_index):
    # 边界检查与安全兜底
    ai_index = _normalize_ai_index(ai_index, default=0)
    config = AI_CONFIGS[ai_index]

    api_key = (config.get("api_key") or "").strip()
    if not api_key:
        return f"调用 {config.get('name', 'AI')} 失败: 未配置 API Key"

    try:
        base_url = (config.get("base_url") or "").rstrip("/")
        # 容错：如果误填成完整接口，则回退到根路径
        if base_url.endswith("/chat/completions"):
            base_url = base_url[: -len("/chat/completions")]
        client = OpenAI(
            api_key=api_key,
            base_url=base_url,
            default_headers=config.get("headers")
        )

        # 兼容历史：前端一直传的是字符串（手输标题）；这里统一组装为 list
        titles = [title_list_str]
        titles_json = json.dumps(titles, ensure_ascii=False)
        prompt = (
            "你是一个情感分析专家。请对下列新闻标题进行情感分类，只能选择'POSITIVE'（积极）、'NEGATIVE'（消极）、'NEUTRAL'（中性）三种标签之一。"
            "请严格返回如下格式的JSON：{\"results\": [\"POSITIVE\", \"NEGATIVE\", ...]}，results长度与输入标题列表一致。"
            f"输入标题列表(JSON数组)：{titles_json}"
        )

        response = client.chat.completions.create(
            model=config["model"],
            messages=[
                {"role": "user", "content": prompt}
            ],
            temperature=0,
        )
        # 提取回复内容
        bot_message = response.choices[0].message.content
        print(bot_message)
        return f"[{config['name']}]: {bot_message}"
    except Exception as e:
        # 捕获具体的 API 错误（如 Key 无效、网络超时等）
        error_msg = str(e)
        print(f"API 调用错误: {error_msg}")
        return (
            f"调用 {config['name']} 失败: {error_msg} "
            f"(base_url={base_url}, model={config.get('model')})"
        )


def _extract_json_object(text: str):
    """从模型输出中尽量提取一个 JSON 对象字符串。"""
    if not text:
        return None
    text = text.strip()
    # 常见情况：直接就是 JSON
    if text.startswith("{") and text.endswith("}"):
        return text
    # 常见情况：包在 ```json ... ```
    if "```" in text:
        parts = text.split("```")
        for part in parts:
            part = part.strip()
            if part.startswith("json"):
                part = part[4:].strip()
            if part.startswith("{") and part.endswith("}"):
                return part
    # 兜底：截取首尾大括号之间
    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1 and end > start:
        return text[start : end + 1]
    return None


def analyze_sentiment(titles, ai_index: int):
    """程序接口：输入标题列表，输出结构化 results。"""
    if not isinstance(titles, list) or not titles or not all(isinstance(t, str) and t.strip() for t in titles):
        return {"ok": False, "error": "titles 必须是非空字符串数组"}, 400

    ai_index = _normalize_ai_index(ai_index, default=0)
    config = AI_CONFIGS[ai_index]

    api_key = (config.get("api_key") or "").strip()
    if not api_key:
        return {"ok": False, "error": "未配置 API Key", "provider": config.get("name")}, 500

    base_url = (config.get("base_url") or "").rstrip("/")
    if base_url.endswith("/chat/completions"):
        base_url = base_url[: -len("/chat/completions")]

    client = OpenAI(
        api_key=api_key,
        base_url=base_url,
        default_headers=config.get("headers"),
    )

    titles_json = json.dumps(titles, ensure_ascii=False)
    prompt = (
        "你是一个情感分析专家。请对下列新闻标题进行情感分类，只能选择'POSITIVE'（积极）、'NEGATIVE'（消极）、'NEUTRAL'（中性）三种标签之一。"
        "请严格返回如下格式的JSON：{\"results\": [\"POSITIVE\", \"NEGATIVE\", ...]}，results长度与输入标题列表一致。"
        f"输入标题列表(JSON数组)：{titles_json}"
    )

    try:
        response = client.chat.completions.create(
            model=config["model"],
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
        )
        raw = response.choices[0].message.content or ""
        json_text = _extract_json_object(raw)
        if not json_text:
            return {
                "ok": False,
                "error": "模型输出无法解析为 JSON",
                "provider": config.get("name"),
                "model": config.get("model"),
                "raw": raw,
            }, 502

        payload = json.loads(json_text)
        results = payload.get("results")
        if not isinstance(results, list) or len(results) != len(titles):
            return {
                "ok": False,
                "error": "results 格式不正确或长度不匹配",
                "provider": config.get("name"),
                "model": config.get("model"),
                "raw": raw,
                "parsed": payload,
            }, 502

        return {
            "ok": True,
            "provider": config.get("name"),
            "model": config.get("model"),
            "results": results,
        }, 200
    except Exception as e:
        return {
            "ok": False,
            "error": str(e),
            "provider": config.get("name"),
            "model": config.get("model"),
            "base_url": base_url,
        }, 502

# === 路由 ===
@app.route("/")
def index():
    return render_template("index.html")

@app.route("/ask", methods=["POST"])
def ask():
    data = request.get_json(silent=True) or {}

    user_message = data.get("message", "")
    ai_index = data.get("ai_index")
    if ai_index is None:
        ai_index = get_default_ai_index()

    if not user_message:
        return jsonify({"response": "消息不能为空"}), 400

    bot_response = call_ai_service(user_message, ai_index)

    return jsonify({"response": bot_response})


@app.route("/api/sentiment", methods=["POST"])
def api_sentiment():
    """给爬虫/后端模块调用的稳定接口：返回结构化 JSON。"""
    data = request.get_json(silent=True) or {}

    ai_index = data.get("ai_index")
    if ai_index is None:
        ai_index = get_default_ai_index()
    else:
        ai_index = _normalize_ai_index(ai_index, default=get_default_ai_index())

    # 支持 title(单条) 或 titles(数组)
    if "titles" in data:
        titles = data.get("titles")
    else:
        title = data.get("title") or data.get("message")
        titles = [title] if isinstance(title, str) else []

    result, status = analyze_sentiment(titles, ai_index)

    # 记录到看板（只记录该稳定接口的请求/响应，便于追踪爬虫侧实时输入/输出）
    try:
        _board_add(
            {
                "ts": _utc_now_iso(),
                "route": "/api/sentiment",
                "ai_index": _normalize_ai_index(ai_index, default=get_default_ai_index()),
                "provider": result.get("provider") if isinstance(result, dict) else None,
                "model": result.get("model") if isinstance(result, dict) else None,
                "ok": result.get("ok") if isinstance(result, dict) else False,
                "titles": titles,
                "results": result.get("results") if isinstance(result, dict) else None,
                "error": result.get("error") if isinstance(result, dict) else None,
            }
        )
    except Exception:
        # 看板不应影响主流程
        pass

    return jsonify(result), status


@app.route("/api/board", methods=["GET"])
def api_board():
    """爬虫实时看板：返回最近 N 条 /api/sentiment 调用记录。"""
    limit = request.args.get("limit", 50)
    try:
        limit = int(limit)
    except Exception:
        limit = 50
    return jsonify({"ok": True, "items": _board_list(limit=limit)}), 200


@app.route("/api/model", methods=["GET", "POST"])
def api_model():
    """读取/设置当前默认 AI 模型索引。

    - GET: 返回当前默认 ai_index + 可选项
    - POST: {"ai_index": 1} 设置默认模型
    """
    if request.method == "POST":
        data = request.get_json(silent=True) or {}
        set_default_ai_index(data.get("ai_index", 0))

    idx = get_default_ai_index()
    config = AI_CONFIGS[idx]
    options = [
        {"ai_index": i, "provider": c.get("name"), "model": c.get("model")}
        for i, c in enumerate(AI_CONFIGS)
    ]
    return jsonify(
        {
            "ok": True,
            "ai_index": idx,
            "provider": config.get("name"),
            "model": config.get("model"),
            "options": options,
        }
    )


if __name__ == "__main__":
    # call_ai_service("国台办霸气回应“外部干涉”：不要低估中国能力",1) #测试能否调用
    app.run(debug=True, port=5000)
