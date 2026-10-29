import os
import sys
import time
import random
import requests
import pandas as pd
from lxml import html
import datetime as datetime1
import re
from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
sys.path.append(os.path.dirname(os.path.abspath(__file__)) + "/..")
from urllib.parse import quote, unquote


HOT_COOKIE = __import__("os").environ.get("HOT_COOKIE", "")

class toutiao_infos:
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
            data = self.get_info(keyword, 1)
            df = self.parse_search_results(data)
            if self.debug:
                print(f"关键词 {keyword} 爬取完成\n\n")
            time.sleep(random.uniform(1, 3))
        return df


    def get_keyword(self):
        url = "https://tophub.today/n/x9ozB4KoXb"
        keywords = []
        try_cnt = 0
        while True:
            try:
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

    def get_info(self, keyword: str = "奶龙", page: int = 1):
        url = "https://so.toutiao.com/search"

        cookies = {
        "tt_webid": "7463840914033772073",
        "_ga": "GA1.1.805733647.1737810923",
        "_tea_utm_cache_4916": "undefined",
        "_S_DPR": "1.25",
        "_S_IPAD": "0",
        "s_v_web_id": "verify_m6px7b1o_AnD0SIp7_jKed_489U_BvmO_UrU9buVKvl8N",
        "notRedShot": "1",
        "__ac_nonce": "067a1f69100222cf948c1",
        "__ac_signature": "_02B4Z6wo00f0113FVuAAAIDCgp6lrA0d8ytd5VJAALDbde",
        # "__ac_referer": "https://so.toutiao.com/search?dvpf=pc&source=input&keyword=%E7%89%B9%E6%9C%97%E6%99%AE&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num=0&min_time={min_time}&max_time={max_time}",
        "__ac_referer": f"https://so.toutiao.com/search?dvpf=pc&source=input&keyword={quote(keyword)}&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num={page}",
        "ttwid": "1%7CcwW8kzZ307M3Eljar0nndoeqBhQT-SE3A-appJN-MqI%7C1738661363%7Cc1526e75a23a6120e4e4724e4a921843efbb6c89f1325825ad72a468856357dc",
        "_ga_QEHZPBE5HH": "GS1.1.1738660939.3.1.1738661392.0.0.0",
        "_S_WIN_WH": "298_738"
        }

        headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            # "Accept-Encoding": "gzip, deflate, br, zstd",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
            "Cache-Control": "max-age=0",
            "Connection": "keep-alive",
            "Dnt": "1",
            "Host": "so.toutiao.com",
            # "Referer": "https://so.toutiao.com/search?dvpf=pc&source=input&keyword=11111111&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num=0&min_time={min_time}&max_time={max_time}",
            "Referer": f"https://so.toutiao.com/search?dvpf=pc&source=input&keyword={quote(keyword)}&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num={page}",
            "Sec-Ch-Ua": "\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"",
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": "\"Windows\"",
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "same-origin",
            "Sec-Fetch-User": "?1",
            "Upgrade-Insecure-Requests": "1",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1",
        }

        params = {
            "dvpf": "pc",
            "source": "input",
            # "keyword": "特朗普",
            "keyword": keyword,
            "pd": "synthesis",
            "action_type": "pagination",
            "from": "search_tab",
            "cur_tab_title": "search_tab",
            # "page_num": 0,
            "page_num": page,
            # "filter_vendor": "all",
            # "index_resource": "all",
            # "filter_period": "all",
            # "min_time": min_time,
            # "max_time": max_time,
            # "search_id": "20250204173626A258818342E34C1771D8"
        }
        try:
            # 发送请求
            response = requests.get(url, params=params, headers=headers, cookies=cookies)
            # 检查响应状态码
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            print(f"请求出错: {e}")
            return None


    def parse_search_results(self, html_content):
        if html_content:
            try:
                tree = html.fromstring(html_content)
                # 定位所有文章卡片
                article_cards = tree.xpath('//div[@class="s-result-list"]//div[@class="cs-view cs-view-block cs-card"]')
                for card in article_cards:
                    # 提取标题
                    title_a = card.xpath('.//a[@class="text-ellipsis text-underline-hover"][1]')
                    if title_a:
                        title = ''.join(title_a[0].itertext()).strip()
                    else:
                        title = ""
                    # 提取链接
                    if title_a:
                        href = title_a[0].get('href')
                        if href:
                            start_index = href.find('url=')
                            if start_index != -1:
                                url_encoded = href[start_index + 4:]
                                # 对 URL 进行解码
                                url = unquote(url_encoded)
                            else:
                                url = ""
                        else:
                            url = ""
                    else:
                        url = ""

                    # 提取内容
                    content_span = card.xpath('.//span[@class="text-underline-hover"]')
                    if content_span:
                        content_text = ''.join(content_span[0].itertext())
                        content = content_text.strip() if content_text else ""
                    else:
                        content = ""

                    # 提取作者
                    author_span = card.xpath(
                        './/div[@class="cs-view cs-view-flex align-items-center flex-row cs-source-content"]/span[1]/span[@class="text-ellipsis"]')
                    if author_span:
                        author_text = author_span[0].text
                        author = author_text.strip() if author_text else ""
                    else:
                        author = ""

                    # 提取评论数
                    comment_span = card.xpath(
                        './/div[@class="cs-view margin-top-3 cs-view-block cs-source"]//div[@class="cs-view cs-view-flex align-items-center flex-row cs-source-content"]/span[@class="text-ellipsis margin-right-4"][contains(text(), "评论")]')
                    if comment_span:
                        comment_text = comment_span[0].text
                        comment_count_str = ''.join(filter(str.isdigit, comment_text)) if comment_text else ""
                        comment_count = int(comment_count_str)
                    else:
                        comment_count = 0

                    # 提取发布时间
                    publish_time_span = card.xpath(
                        './/div[@class="cs-view margin-top-3 cs-view-block cs-source"]//div[@class="cs-view cs-view-flex align-items-center flex-row cs-source-content"]/span[@class="text-ellipsis"][last()]')
                    if publish_time_span:
                        publish_time_text = publish_time_span[0].text
                        publish_time = publish_time_text.strip() if publish_time_text else ""
                    else:
                        publish_time = ""

                    # 判断发布时间是否在范围内
                    publish_time = self.transTime(publish_time)
                    if not publish_time:
                        if self.debug:
                            print(f"发布时间解析失败, 跳过该条数据, title={title}, url={url}")
                        continue  # 跳过发布时间为空的数据
                    new = datetime.strptime(publish_time, "%Y-%m-%d %H:%M:%S")
                    if new < self.latest:
                        if self.debug:
                            print(f"发布时间 {new} 早于设定时间 {self.latest}, 跳过")
                        continue

                    item = {
                        'source': '今日头条',
                        'title': title,
                        'url': url,
                        'publish_time': publish_time,
                        'author': author,
                        'content': content,
                        'like_count': 0,
                        'share_count': 0,
                        'comment_count': comment_count,
                        'read_count': comment_count
                    }
                    self.threads.append(item)  # 将当前文章卡片的数据添加到列表中
                    if self.debug:
                        print(item)
                        print("Succeeded in crawling one thread!\n")
                    self.threads_count += 1
                return self.deal_info()
            except Exception as e:
                print(f"解析 HTML 出错: {e}")


    def transTime(self, time_str):
        try:
            if time_str is None or time_str == "":
                return None
            if time_str.find("刚刚") != -1:
                return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("秒前") != -1:
                seconds = int(re.search(r'(\d+)', time_str).group(1))
                return (datetime.now() - datetime1.timedelta(seconds=seconds)).strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("分钟前") != -1:
                seconds = int(re.search(r'(\d+)', time_str).group(1))
                return datetime.strptime((datetime.now() - datetime1.timedelta(seconds=seconds)).strftime("%Y-%m-%d %H:%M") + ':00', "%Y-%m-%d %H:%M:%S").strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("小时前") != -1:
                hours = int(re.search(r'(\d+)', time_str).group(1))
                return datetime.strptime((datetime.now() - datetime1.timedelta(hours=hours)).strftime("%Y-%m-%d %H") + ':00:00', "%Y-%m-%d %H:%M:%S").strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("昨天") != -1:
                time_str = time_str.replace("昨天", "")
                return datetime.strptime((datetime.now() - datetime1.timedelta(days=1)).strftime("%Y-%m-%d") + ' ' + time_str, "%Y-%m-%d %H:%M").strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("前天") != -1:
                time_str = time_str.replace("前天", "")
                return datetime.strptime((datetime.now() - datetime1.timedelta(days=2)).strftime("%Y-%m-%d") + ' ' + time_str, "%Y-%m-%d %H:%M").strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("天前") != -1:
                days = int(re.search(r'(\d+)', time_str).group(1))
                return datetime.strptime((datetime.now() - datetime1.timedelta(days=days)).strftime("%Y-%m-%d") + ' ' + '00:00:00', "%Y-%m-%d %H:%M:%S").strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("年") != -1 and time_str.find("月") != -1 and time_str.find("日") != -1:
                time_str = time_str.replace("年", "-").replace("月", "-").replace("日", "")
                return datetime.strptime(time_str, "%Y-%m-%d").strftime("%Y-%m-%d %H:%M:%S")
            if time_str.find("月") != -1 and time_str.find("日") != -1:
                time_str = time_str.replace("月", "-").replace("日", "")
                return datetime.strptime(str(datetime.now().year) + "-" + time_str, "%Y-%m-%d").strftime("%Y-%m-%d %H:%M:%S")
        except:
            return None


    def deal_info(self):
        """处理信息"""
        print("jinru     ")
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
    x = toutiao_infos(6, True, True)
    x.crawl_data()
