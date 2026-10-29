import json
import os
import time
from dataclasses import asdict, dataclass
from datetime import datetime
from typing import Any, Generator

import requests


DEFAULT_SERVER = os.getenv("OPENCLAW_SERVER", "http://127.0.0.1:18789")
DEFAULT_TOKEN = os.getenv(
    "OPENCLAW_TOKEN",
    "c745279ab90ab63bff7ee38befdbde8473b542b056c36d21",
)
DEFAULT_MODEL = os.getenv("OPENCLAW_MODEL", "auto")
RETRYABLE_STATUS_CODES = {429, 500, 502, 503, 504}


@dataclass
class OpenClawTaskRequest:
    name: str
    url: str
    crawl_logic: str
    interval: int = 60
    start_time: str | None = None
    end_time: str | None = None

    def validate(self) -> None:
        if not self.name.strip():
            raise ValueError("name cannot be empty")
        if not self.url.startswith(("http://", "https://")):
            raise ValueError("url must start with http:// or https://")
        if not self.crawl_logic.strip():
            raise ValueError("crawl_logic cannot be empty")
        if self.interval <= 0:
            raise ValueError("interval must be greater than 0")
        if self.start_time:
            datetime.fromisoformat(self.start_time)
        if self.end_time:
            datetime.fromisoformat(self.end_time)

    def to_business_payload(self) -> dict[str, Any]:
        self.validate()
        return {
            "name": self.name,
            "url": self.url,
            "crawlLogic": self.crawl_logic,
            "interval": self.interval,
            "startTime": self.start_time,
            "endTime": self.end_time,
        }


class OpenClawClient:
    def __init__(
        self,
        server: str = DEFAULT_SERVER,
        token: str = DEFAULT_TOKEN,
        model: str = DEFAULT_MODEL,
        timeout: int = 180, # 将 HTTP 客户端请求 OpenClaw API 的超时放宽至 3 分钟
        max_retries: int = 3,
        retry_delay: float = 1.5,
    ) -> None:
        self.server = server.rstrip("/")
        self.token = token
        self.model = model
        self.timeout = timeout
        self.max_retries = max_retries
        self.retry_delay = retry_delay

    def _headers(self) -> dict[str, str]:
        return {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.token}",
        }

    def _url(self) -> str:
        return f"{self.server}/v1/chat/completions"

    def _build_messages(
        self, task: OpenClawTaskRequest
    ) -> list[dict[str, str]]:
        business_payload = task.to_business_payload()
        business_json = json.dumps(business_payload, ensure_ascii=False, indent=2)

        prompt = (
            "你是 OpenClaw 抓取执行助手。\n"
            "请根据下面的任务配置理解并用你的技能抓取目标，并返回抓取结果。\n"
            "【强制要求】：\n"
            "1. 必须返回 JSON 数组格式，禁止输出任何其他解释性文字或 Markdown 代码块包裹（如不要输出 ```json）。\n"
            "2. 每个数组元素必须包含以下字段：title, content, url, source。\n"
            "3. 如果无法获取内容，请为该字段填充空字符串或默认值，不要省略字段。\n"
            "任务配置如下：\n"
            f"{business_json}"
        )
        return [{"role": "user", "content": prompt}]

    def _request_payload(
        self, task: OpenClawTaskRequest, stream: bool = False
    ) -> dict[str, Any]:
        return {
            "model": self.model,
            "messages": self._build_messages(task),
            "temperature": 0.7,
            "max_tokens": 2048,
            "stream": stream,
        }

    def _should_retry(self, response: requests.Response | None) -> bool:
        return response is not None and response.status_code in RETRYABLE_STATUS_CODES

    def _format_http_error(
        self, response: requests.Response, attempt: int
    ) -> str:
        return (
            f"HTTP {response.status_code}\n"
            f"Attempt: {attempt}/{self.max_retries}\n"
            f"URL: {response.request.url}\n"
            f"Response: {response.text}"
        )

    def _sleep_before_retry(self, attempt: int) -> None:
        time.sleep(self.retry_delay * attempt)

    def ask_task(self, task: OpenClawTaskRequest) -> str:
        payload = self._request_payload(task, stream=False)
        last_error = None

        for attempt in range(1, self.max_retries + 1):
            try:
                response = requests.post(
                    self._url(),
                    headers=self._headers(),
                    json=payload,
                    timeout=self.timeout,
                )
                response.raise_for_status()
                data = response.json()
                return data["choices"][0]["message"]["content"]
            except requests.exceptions.HTTPError as e:
                response = e.response
                last_error = self._format_http_error(response, attempt)
                if attempt < self.max_retries and self._should_retry(response):
                    self._sleep_before_retry(attempt)
                    continue
                return last_error
            except requests.exceptions.RequestException as e:
                last_error = (
                    f"Request error on attempt {attempt}/{self.max_retries}: {e}"
                )
                if attempt < self.max_retries:
                    self._sleep_before_retry(attempt)
                    continue
                return last_error
            except Exception as e:
                return f"Error: {e}"

        return last_error or "Error: request failed"

    def ask_task_stream(
        self, task: OpenClawTaskRequest
    ) -> Generator[str, None, None]:
        payload = self._request_payload(task, stream=True)
        last_error = None

        for attempt in range(1, self.max_retries + 1):
            try:
                response = requests.post(
                    self._url(),
                    headers=self._headers(),
                    json=payload,
                    timeout=self.timeout,
                    stream=True,
                )
                response.raise_for_status()

                for line in response.iter_lines():
                    if not line:
                        continue

                    text = line.decode("utf-8").strip()
                    if not text.startswith("data: "):
                        continue

                    chunk_text = text[len("data: ") :]
                    if chunk_text == "[DONE]":
                        return

                    try:
                        chunk = json.loads(chunk_text)
                        delta = chunk["choices"][0].get("delta", {})
                        content = delta.get("content", "")
                        if content:
                            yield content
                    except (json.JSONDecodeError, KeyError, IndexError):
                        continue
                return
            except requests.exceptions.HTTPError as e:
                response = e.response
                last_error = self._format_http_error(response, attempt)
                if attempt < self.max_retries and self._should_retry(response):
                    self._sleep_before_retry(attempt)
                    continue
                yield last_error
                return
            except requests.exceptions.RequestException as e:
                last_error = (
                    f"Request error on attempt {attempt}/{self.max_retries}: {e}"
                )
                if attempt < self.max_retries:
                    self._sleep_before_retry(attempt)
                    continue
                yield last_error
                return
            except Exception as e:
                yield f"Error: {e}"
                return


def build_openclaw_task(data: dict[str, Any]) -> OpenClawTaskRequest:
    return OpenClawTaskRequest(
        name=data["name"],
        url=data["url"],
        crawl_logic=data["crawlLogic"],
        interval=data.get("interval", 60),
        start_time=data.get("startTime"),
        end_time=data.get("endTime"),
    )


def ask_openclaw(task_data: dict[str, Any]) -> str:
    client = OpenClawClient()
    task = build_openclaw_task(task_data)
    return client.ask_task(task)


def ask_openclaw_stream(
    task_data: dict[str, Any]
) -> Generator[str, None, None]:
    client = OpenClawClient()
    task = build_openclaw_task(task_data)
    yield from client.ask_task_stream(task)


if __name__ == "__main__":
    demo_task = {
        "name": "示例网站信息学院招生信息采集",
        "url": "https://localhost/zsxx.htm",
        "crawlLogic": "抓取首页新闻列表标题、发布时间，并进入详情页提取正文内容。",
        "interval": 60,
        "startTime": "2026-03-22T09:00:00",
        "endTime": "2026-12-31T23:59:59",
    }

    client = OpenClawClient()
    task = build_openclaw_task(demo_task)

    print("任务入参：")
    print(json.dumps(asdict(task), ensure_ascii=False, indent=2))

    print("-" * 40)
    print("普通调用结果：")
    print(client.ask_task(task))

    print("-" * 40)
    print("流式调用结果：", end="", flush=True)
    for chunk in client.ask_task_stream(task):
        print(chunk, end="", flush=True)
    print("\n[输出结束]")
