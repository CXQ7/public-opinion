import requests
from common.constant import *
from repository.proxy_usage_repo import proxy_usage_repo
from loguru import logger

PROXY_POOL_CLIENT_URL = f'http://{PROXY_POOL_HOST}:{PROXY_POOL_PORT}/random'

class ProxyPool:
    def __init__(self):
        self.proxy = ""
        self.proxy_dict = {}  # requests库需要使用这个格式

    def _refresh_proxy(self, source):
        """从代理池获取一个新的代理"""
        # 每次刷新都重置，避免使用过期代理
        self.proxy = ""
        self.proxy_dict = {}
        try:
            response = requests.get(PROXY_POOL_CLIENT_URL, params={
                'source': source,
            }, timeout=PROXY_DEFAULT_TIMEOUT)
            if response.status_code == 200 and response.text and response.text.strip():
                self.proxy = response.text.strip()
                self.proxy_dict = {
                    'http': f'http://{self.proxy}',
                    'https': f'http://{self.proxy}',
                }
        except requests.exceptions.RequestException as e:
            logger.warning(f"连接代理服务器失败: {e}")

    def proxy_retry(self, source, fn, judge_fn) -> requests.Response:
        for i in range(PROXY_REQUEST_RETRY):
            try:
                self._refresh_proxy(source)
                response = fn()
                ok = False
                try:
                    ok = judge_fn(response)
                except TypeError:
                    # 兼容旧的 judge_fn: lambda: True
                    ok = True

                if response.status_code != 200 or not ok:
                    print("response code != 200, response = ", response)
                    print(response.status_code)
                    print(response.text)
                    raise Exception("代理请求失败")

                print(f"{i} 次尝试成功, 代理为: {self.proxy}")
                if self.proxy:
                    proxy_usage_repo.increase_success(source, self.proxy)
                return response
            except Exception as e:
                if self.proxy:
                    proxy_usage_repo.increase_fail(source, self.proxy)
                print(f"{i} 次尝试失败, 代理为: {self.proxy}, exception: {e}")
        raise Exception(f"{PROXY_REQUEST_RETRY} 次尝试代理请求失败, 彻底失败")

# 通过代理发送请求, 自动重试, 默认值10次
def post_proxy(source, url, headers=None, body=None, cookies=None, verify=False, judge_fn=lambda resp: True) -> requests.Response:
    pool = ProxyPool()

    def request_func():
        return requests.post(url, headers=headers, json=body, cookies=cookies, verify=verify,
                             proxies=pool.proxy_dict, timeout=PROXY_DEFAULT_TIMEOUT)

    return pool.proxy_retry(source=source, fn=request_func, judge_fn=judge_fn)

def get_proxy(source, url, params=None, headers=None, body=None, cookies=None, verify=False, judge_fn=lambda resp: True) -> requests.Response:
    pool = ProxyPool()

    def request_func():
        return requests.get(url, headers=headers, params=params, json=body, cookies=cookies, verify=verify,
                             proxies=pool.proxy_dict, timeout=PROXY_DEFAULT_TIMEOUT)

    return pool.proxy_retry(source=source, fn=request_func, judge_fn=judge_fn)
