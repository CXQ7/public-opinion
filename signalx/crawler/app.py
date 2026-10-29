import uvicorn
from http_api import HttpApi

if __name__ == "__main__":
    # 启动 HTTP API 服务器
    api_server = HttpApi()
    api_server.run(host="127.0.0.1", port=8000)
