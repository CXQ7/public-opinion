import os
import random
import requests
import pandas as pd
from xhs_crawler.xhs_utils import *
import datetime as datetime1
from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# 不知道什么时候会过期, 过期后需要更换新的登录态COOKIE
XHS_COOKIE = __import__("os").environ.get("XHS_COOKIE", "")

HOT_COOKIE = __import__("os").environ.get("HOT_COOKIE", "")

class xhs_infos:
    def __init__(self, hoursBefore, debug = False, output_as_file=False):
        """对象初始化"""
        self.threads = []
        self.threads_count = 0
        self.write_count = 0
        self.debug = debug
        self.output_as_file = output_as_file
        self.latest = datetime.today() - datetime1.timedelta(hours=hoursBefore)
        if self.debug:
            print("最后一次的爬取时间:", self.latest, '\n')


    def crawl_data(self):
        keywords = []
        try:
            keywords = self.get_keyword()
            if self.debug:
                print("获取关键词成功:", keywords)
        except:
            if self.debug:
                print("获取关键词失败，关闭无界面运行，手动验证！")
        df = pd.DataFrame()
        for keyword in keywords:
            if self.debug:
                print(f"开始爬取关键词: {keyword}")
            df = self.get_info(keyword, 3, 20)
            if self.debug:
                print(f"关键词 {keyword} 爬取完成\n\n")
            time.sleep(random.uniform(1, 3))
        return df


    def get_keyword(self):
        url = "https://tophub.today/n/L4MdA5ldxD"
        keywords = []
        try_cnt = 0
        while True:
            try:
                # pool = ProxyPool()
                # options = pool.get_selenium_proxy_options("tophub")
                options = Options()
                # options.add_argument('-headless')  #无界面运行
                options.add_argument('headers')  #添加请求头
                options.add_argument('-ignore-certificate-errors')
                options.add_argument('-ignore -ssl-errors')
                options.add_experimental_option('excludeSwitches', ['enable-automation'])  #设置开发者模式启动
                # service = Service("C:\\Users\\ASUS\\.cache\\selenium\\chromedriver\\win64\\140.0.7339.207\\chromedriver.exe")
                # driver = webdriver.Chrome(options = options, service=service)
                driver = webdriver.Chrome(options = options)
                driver.get(url)
                WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.TAG_NAME, "table"))
                )
                rows = driver.find_elements(By.XPATH, '//*[@id="page"]/div[3]/div/div[2]/div[2]/div[2]/div/div[1]/table/tbody/tr')
                for row in rows:
                    title = row.find_element(By.XPATH, './td[2]/a').text
                    keywords.append(title)
                print(f"第{try_cnt}次尝试成功")
                break
            except Exception as e:
                if self.debug:
                    print(f"Selenium 抓取失败: {e}")
                try_cnt += 1
        return keywords


    def get_info(self, keyword: str = "奶龙", page: int = 1, page_size: int = 20):
        for page_i in range(1, page + 1):
            url = "https://edith.xiaohongshu.com/api/sns/web/v1/search/notes"
            headers = {
                "accept": "application/json, text/plain, */*",
                "accept-encoding": "gzip, deflate, br, zstd",
                "accept-language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
                "content-length": "176",
                "content-type": "application/json;charset=UTF-8",
                "dnt": "1",
                "origin": "https://www.xiaohongshu.com",
                "priority": "u=1, i",
                "referer": "https://www.xiaohongshu.com/",
                "sec-ch-ua": "\"Not(A:Brand\";v=\"99\", \"Microsoft Edge\";v=\"133\", \"Chromium\";v=\"133\"",
                "sec-ch-ua-mobile": "?0",
                "sec-ch-ua-platform": "\"Windows\"",
                "sec-fetch-dest": "empty",
                "sec-fetch-mode": "cors",
                "sec-fetch-site": "same-site",
                "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
                "x-b3-traceid": "a3182781eee4b68c",
                "x-mns": "unload",
                "x-s": "XYW_eyJzaWduU3ZuIjoiNTYiLCJzaWduVHlwZSI6IngyIiwiYXBwSWQiOiJ4aHMtcGMtd2ViIiwic2lnblZlcnNpb24iOiIxIiwicGF5bG9hZCI6Ijk4YjNjNDU2ZmI3Mzc2YzE2OTMxN2FiNzdlZDJkYjdjZmNkODliMDFhMTNiZDY1M2MwMDY1MzhlN2VkMGVhYWMxZjA4M2YwY2RmYzlhODk3NjAwNjU1OTZiYzU5NDg0OGI0ZGUzMGZhMTQwMmQ1ODljMTllZmU3NTFmNTg3MjcxNmU3ZTY3OTE3YzVmNjY3YzQzMTQ1M2FhNDQxY2EwYjgwYmFjNDAyNmQyMDEyMzZmMjQ2ODYyYTY1NzcwMWVhNTgwNDEzY2NhNmU3MWU4OGNmMGQzNGY0ZDM3NjcyYWZjNTBjMjRhMmM0OGI0NmFkN2NiYjc0ZGE1ZTU3MmIxMjVlODA4MjcyZDc1MTU3MWQ1ODFkNjdjZWY3YTU4ZTBlODlhMDBhOGY2MWZmNTNmODM0MzhkZjQwN2Y4NzdmMzZhNDYwYjg1Yjc0MTA1MDliZjE5YmVmNzA4M2VkMmJjNTAzNmU3MDBiNDgyNGM5MDRmNzAzMTJiMGJjOGFhZDQ5NjYyMmUwMzk3ZjY1YzQ1NWIwMmQyOTk1Yzg1NTc1OTlmIn0=",
                # "x-s-common": "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",
                "x-t": str(int(time.time() * 1000)),
                "x-xray-traceid": "ca9eb2bef107ac78753803d3b9740ee0",

                "cookie": XHS_COOKIE
            }


            body = {
                "ext_flags": [],
                "image_formats": ["jpg", "webp", "avif"],
                "keyword": keyword,
                "note_type": 0,
                "page": page_i,
                "page_size": page_size,
                "search_id": "2egrms4id30fhlgrojnaj",
                "sort": "time_descending"
            }

            try_cnt = 0
            while try_cnt < 3:
                response = requests.post(url, headers=headers, json=body)
                if response.status_code != 200:
                    if self.debug:
                        print(f"请求失败，状态码: {response.status_code}")
                    return
                print(response.text)
                try:
                    json_data = response.json()
                    for item in json_data['data']['items']:
                        if item['model_type'] == 'rec_query':
                            if self.debug: print('不支持rec_query类型, skip')
                            continue
                        if item['model_type'] == 'note' or item['model_type'] == 'normal':
                            t = {}
                            try:
                                t["source"] = '小红书'
                                card = item['note_card']
                                if 'display_title' in card:
                                    t["title"] = card['display_title']
                                else: t["title"] = None
                                t["url"] = f'https://www.xiaohongshu.com/explore/{item["id"]}?xsec_token={item["xsec_token"]}'

                                t["publsh_time"] = id_to_time(item['id'])
                                if not t["publsh_time"]:
                                    continue  # 跳过无效时间
                                publsh_time = t["publsh_time"]
                                try:
                                    if isinstance(publsh_time, datetime):
                                        new = publsh_time
                                    else:
                                        new = datetime.strptime(publsh_time, "%Y-%m-%d %H:%M:%S")
                                except Exception as e:
                                    if self.debug: print(f"时间解析错误, publsh_time={publsh_time}, exception: {e}")
                                    continue
                                if new < self.latest:
                                    if self.debug:
                                        print(f"temp = {t}")
                                        print(f"发布时间 {new} 早于设定时间 {self.latest}, 跳过")
                                    continue

                                t["author"] = card['user']['nickname']
                                t["content"] = t["title"]
                                t['like_count'] = card['interact_info']['liked_count']
                                t['share_count'] = card['interact_info']['shared_count']
                                t["comment_count"] = card['interact_info']['comment_count']
                                t['read_count'] = max(t["comment_count"], t["like_count"], t["share_count"])
                                self.threads.append(t)
                                if self.debug:
                                    print(t)
                                    print("Succeeded in crawling one thread!\n")
                                self.threads_count += 1
                            except Exception as e:
                                if self.debug: print(f"解析错误, item = {item}, exception: {e}")
                        else:
                            if self.debug: print(f"item 非note类型, 类型为{item['model_type']}, item = {item}")
                    time.sleep(random.uniform(1, 3))
                    return self.deal_info()
                except:
                    time.sleep(random.uniform(1, 3))
                    try_cnt += 1


    def deal_info(self):
        """处理信息"""
        result_headers = [
            "source", "title", "url", "publish_time", "author", "content", "like_count", "share_count", "comment_count", "read_count"
        ] # 要写入结果文件的表头
        result_data = [t.values() for t in self.threads[self.write_count:]]
        df = pd.DataFrame(data=result_data, columns=result_headers)
        if self.output_as_file:
            file_path =  os.path.split(os.path.realpath(__file__))[0] + os.sep + 'crawler_result.csv'
            df.to_csv(file_path, encoding="utf_8_sig", index_label='event_id')
        if self.debug:
            df.info()
        return df


if __name__ == "__main__":
    x = xhs_infos(6, True, True)
    x.crawl_data()
