import threading
import uvicorn
import json
import os
import uuid
import datetime
import redis
from fastapi import FastAPI, Body, BackgroundTasks
from api_scheduler import ApiScheduler
from openclaw_client import OpenClawClient, OpenClawTaskRequest

class HttpApi:
    def __init__(self):
        self.app = FastAPI()
        self.scheduler = ApiScheduler()
        self.openclaw_client = OpenClawClient()

        # Redis 配置 (从环境变量获取或默认本地)
        self.redis_client = redis.Redis(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=int(os.getenv("REDIS_PORT", 6379)),
            decode_responses=False # 保持原始字节以配合 Java 序列化
        )
        self.env_prefix = "signalx:dev:"

    def _push_to_redis(self, topic: str, payload: dict):
        """将结果封装为 Java 可识别的 QueueMessage 格式并推送到 Redis"""
        message_id = str(uuid.uuid4())

        raw_payload_str = json.dumps(payload)

        msg_obj = {
            "@class": "cn.edu.xmu.signalx.core.redis.queue.QueueMessage",
            "messageId": message_id,
            "topic": topic,
            "payload": raw_payload_str, # 使用字符串规避深度类型校验
            "retryCount": 0,
            "maxRetry": 3,
            "timestamp": datetime.datetime.now().isoformat()
        }

        msg_json = json.dumps(msg_obj)
        queue_key = f"{self.env_prefix}queue:{topic}"
        msg_key = f"{self.env_prefix}msg:{message_id}"

        self.redis_client.setex(msg_key, datetime.timedelta(days=7), msg_json)

        json_message_id = json.dumps(message_id)
        self.redis_client.rpush(queue_key, json_message_id)

        print(f"Pushed to Redis queue [{topic}] | msgId: {message_id}")

    def setup_routes(self):
        @self.app.post("/api/v1/crawler/start")
        @self.app.get("/api/v1/crawler/start")
        async def start_crawl():
            threading.Thread(target=self._run_task).start()
            return {"status": "Crawler task started", "message": "Executing start_once in background"}

        @self.app.post("/api/v1/openclaw/crawl")
        async def openclaw_crawl(background_tasks: BackgroundTasks, payload: dict = Body(...)):
            """
            OpenClaw 异步抓取接口
            """
            task_id = payload.get("taskId")
            if not task_id:
                return {"code": 1, "message": "taskId is required for async callback"}

            task_req = OpenClawTaskRequest(
                name=payload.get("name", "AutoTask"),
                url=payload.get("url"),
                crawl_logic=payload.get("crawlLogic"),
                interval=payload.get("interval", 60)
            )

            # 定义异步处理逻辑
            def do_crawl_async():
                print(f"Async Crawl Start | taskId: {task_id}")
                try:
                    result = self.openclaw_client.ask_task(task_req)
                    try:
                        news_list = json.loads(result)
                    except:
                        news_list = [{"title": task_req.name, "content": result, "url": task_req.url, "source": "OpenClaw"}]

                    # 回传结果到 Redis
                    callback_payload = {
                        "taskId": task_id,
                        "type": "CRAWL_RESULT",
                        "data": news_list
                    }
                    self._push_to_redis("openclaw.callback", callback_payload)
                except Exception as e:
                    print(f"Async Crawl Error | taskId: {task_id}, error: {e}")

            background_tasks.add_task(do_crawl_async)
            return {"code": 0, "message": "Crawl task submitted successfully"}

        @self.app.post("/api/v1/openclaw/report")
        async def openclaw_report(background_tasks: BackgroundTasks, payload: dict = Body(...)):
            """
            OpenClaw 异步报表生成接口
            """
            task_id = payload.get("taskId")
            raw_content = payload.get("rawContent")
            logic = payload.get("crawlLogic")

            if not task_id:
                return {"code": 1, "message": "taskId is required for async callback"}

            report_prompt = (
                f"请根据以下抓取到的原始数据生成一份详细的个性化报表：\n\n"
                f"原始数据：\n{raw_content}\n\n"
                f"用户初始抓取需求为：{logic}\n\n"
                f"【强制要求】：\n"
                f"1. 必须使用 Markdown 格式。\n"
                f"2. 输出结构必须包含：# 标题, ## 摘要, ## 核心数据点, ## 结论。\n"
                f"3. 必须包含至少一个 Mermaid.js 图表。\n"
                f"4. 严禁输出 Markdown 以外的任何解释性文字。\n"
                f"5. 如果数据中有涉及趋势的内容，请使用 Mermaid 的图表展示。"
            )

            def do_report_async():
                print(f"Async Report Start | taskId: {task_id}")
                try:
                    task_req = OpenClawTaskRequest(name="ReportGen", url="http://internal", crawl_logic=report_prompt)
                    report_md = self.openclaw_client.ask_task(task_req)

                    # 回传结果到 Redis
                    callback_payload = {
                        "taskId": task_id,
                        "type": "REPORT_RESULT",
                        "data": report_md
                    }
                    self._push_to_redis("openclaw.callback", callback_payload)
                except Exception as e:
                    print(f"Async Report Error | taskId: {task_id}, error: {e}")

            background_tasks.add_task(do_report_async)
            return {"code": 0, "message": "Report task submitted successfully"}

        @self.app.get("/api/v1/health")
        async def health_check():
            return {"status": "ok"}

    def _run_task(self):
        print("HTTP Trigger: Starting crawler task...")
        try:
            self.scheduler.start_once()
            print("HTTP Trigger: Crawler task finished.")
        except Exception as e:
            print(f"HTTP Trigger: Error during crawler task: {e}")

    def run(self, host="0.0.0.0", port=8000):
        print(f"Starting HTTP API server on {host}:{port}")
        self.setup_routes() # 确保在 run 中也调用，防止遗漏
        uvicorn.run(self.app, host=host, port=port)

if __name__ == "__main__":
    # 确保安装了 fastapi 和 uvicorn
    # pip install fastapi uvicorn
    api_server = HttpApi()
    api_server.setup_routes() # 显式初始化路由
    api_server.run(host="0.0.0.0", port=8000)
