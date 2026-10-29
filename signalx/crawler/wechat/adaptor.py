import requests
import time
import re
import logging
import random
from common.proxy import post_proxy
from urllib.parse import urlencode
from bs4 import BeautifulSoup
# from transformer import Transformer

from wechat.transformer import WechatTransformer

def judge_fn(response):
    print("Response status code:", response.status_code)
    print("Response text:", response.text[:5000])
    return len(response.text) > 5000


class WechatAdaptor:
    """微信公众号适配器：负责与微信公众号平台交互"""
    def __init__(self, public_account_list: list = ["厦信青年"], debug = False):
        if debug:
            # 配置日志
            logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
            self.logger = logging.getLogger(__name__)
        self.fakeid = {}
        self.articles = []
        self.public_account_list = public_account_list
        # 请求头设置
        # self.headers = {
        #     "Cookie": "__CONFIGURE_LOCALLY__"
        # }
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1",
            "Referer": "https://mp.weixin.qq.com/cgi-bin/appmsg?t=media/appmsg_edit_v2&action=edit&isNew=1&type=10&token=__CONFIGURE_LOCALLY__&lang=zh_CN&timestamp=1773220422769",
            "Cookie": "__CONFIGURE_LOCALLY__",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8"
        }
         # 获取token
        # response = post_proxy(source="wechat", url="https://mp.weixin.qq.com/", headers=self.headers, judge_fn=judge_fn, verify=False,timeout=(10, 20))

        response = requests.get("https://mp.weixin.qq.com/", headers=self.headers, verify=False, timeout=10)

        try_cnt = 0
        while try_cnt < 10:
            token_match = re.search(r'token=(\d+)', response.text)
            if not token_match:
                if debug: self.logger.error("无法获取token")
                try_cnt += 1
                time.sleep(5)
                continue
            self.token = token_match.group(1)
            if debug: self.logger.info("获取token成功")
            break
        if try_cnt >= 10:
            if debug: self.logger.error("获取token失败")
        self.transformer = WechatTransformer()  # 添加transformer实例
        self.set_config(public_account_list, debug)


    def set_config(self, public_account_list: list = [], debug: bool = False):
        '''设置公众号列表'''
        for public_account in public_account_list:
            try:
                # 搜索公众号
                query = {
                    "action": "search_biz",
                    "token": self.token,
                    "lang": "zh_CN",
                    "f": "json",
                    "ajax": "1",
                    "random": str(int(time.time() * 1000)),
                    "query": public_account,
                    "begin": "0",
                    "count": "5"
                }
                query_string = urlencode(query)
                full_url = f"https://mp.weixin.qq.com/cgi-bin/searchbiz?{query_string}"
                response = post_proxy(
                    source="wechat",
                    url=full_url,
                    headers=self.headers,
                    verify=False,
                    timeout=(10, 20),
                    judge_fn=judge_fn
                )
                # response = requests.get(
                #     full_url,
                #     headers=self.headers,
                #     verify=False,
                #     timeout=10
                # )

                data = response.json()
                if not data.get("list"):
                    if debug: self.logger.error("未找到公众号")
                    continue
                self.fakeid[public_account] = data["list"][0]["fakeid"]
                if debug: self.logger.info("获取公众号ID成功")

            except Exception as e:
                if debug: self.logger.error(f"发生错误: {str(e)}")


    def get_articles(self, keyword: str = "", page: int = 3, debug: bool = False):
        for public_account, fakeid in self.fakeid.items():
            if debug: self.logger.info(f"开始搜索公众号: {public_account}")
            batch_size = 2
            begin = page * batch_size
            query = {
                "action": "list_ex",
                "begin": str(begin),
                "count": str(batch_size),
                "fakeid": fakeid,
                "type": "9",
                "query": keyword,
                "token": self.token,
                "lang": "zh_CN",
                "f": "json",
                "ajax": "1"
            }
            query_string = urlencode(query)
            full_url = f"https://mp.weixin.qq.com/cgi-bin/appmsg?{query_string}"

            response = requests.get(
                full_url,
                headers=self.headers,
                verify=False,
                timeout=10
            )
            data = response.json()
            if not data.get("app_msg_list"):
                return []
            for item in data["app_msg_list"]:
                try:
                    # 获取文章详情
                    article_url = item["link"]
                    # article_response = post_proxy(source="wechat", url=article_url, headers=self.headers, verify=False, judge_fn=judge_fn)
                    # article_response = post_proxy(
                    #     source="wechat",
                    #     url=full_url,
                    #     headers=self.headers,
                    #     verify=False,
                    #     judge_fn=judge_fn
                    # )
                    #
                    article_response = requests.get(
                        article_url,
                        headers=self.headers,
                        verify=False,
                        timeout=10
                    )
                    article_response.encoding = 'utf-8'
                    article_content = article_response.text

                    item["title"] = item["title"].replace("<em>", "").replace("</em>", "")
                    # 使用transformer处理内容
                    if keyword in article_content:
                        processed_content = self.transformer.extract_text_content(article_content)
                        article = {
                            "title": item["title"],
                            "content": processed_content,
                            "create_time": item["create_time"],
                            "link": item["link"],
                            "author": item.get("author", public_account)
                        }
                        self.articles.append(article)
                        if debug: self.logger.info(f"找到包含关键词的文章: {item['title']}")

                    time.sleep(0.5)
                    # time.sleep(0.5 + random.uniform(0, 0.5))
                except Exception as e:
                    if debug: self.logger.error(f"获取文章详情失败: {str(e)}")


    def search(self, keyword: str = "寒假", page: int = 0, debug: bool = False):#在此设置目标
        try:
            if debug: self.logger.info("开始爬取文章...")
            self.get_articles(keyword, page, debug)
            if self.articles:
                if debug: self.logger.info(f"共获取到 {len(self.articles)} 篇文章")
                if debug: self.logger.info(f"示例文章: {self.articles[0]['title']}")
                # 转换为统一格式
                return self.transformer.batch_transform(self.articles)
            else:
                if debug: self.logger.warning("未获取到文章")
            return []
        except Exception as e:
            self.set_config(self.public_account_list, debug)
            if debug: self.logger.error(f"程序运行出错: {str(e)}")
            if debug: self.logger.exception("详细错误信息：")
            return []


if __name__ == "__main__":
    wechat = WechatAdaptor(debug=True)
    results = wechat.search(debug=True)
    for item in results:
        print(item)
