import requests
import json
from common.proxy import get_proxy

url = 'https://www.infzm.com/search'

def judge_fn(response):
    print("Response status code:", response.status_code)
    print("Response text:", response.text[:500])
    # 判断是否为 JSON 格式
    return response.status_code == 200 and response.text.strip().startswith('{')

headers = {
    'Accept': 'application/json, text/plain, */*',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1',
    'Accept-Encoding': 'gzip, deflate, br, zstd',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6',
    'Connection': 'keep-alive',
}

def search(keyword: str = "尹锡悦", page: int = 0):
    params = {
        'k': keyword,
        'page': page,
        'format': 'json',
    }

    # res = get_proxy(source="south_weekend", url=url, params=params, headers=headers, verify=False, judge_fn=judge_fn)
    res = requests.get(url, params=params, headers=headers, verify=False)
    # print("实际响应内容:", res.text)  # 调试用
    if res.status_code != 200:
        print("请求失败, 状态码:", res.status_code, "明细: ", res.text)
    else:
        try:
            json_data = json.loads(res.text)
            return json_data.get('data', {}).get('list', [])
        except Exception as e:
            print("JSON解析失败:", e)
            print("原始内容:", res.text[:500])
            return []

    return []
