import os
import re
import time
import random
import requests
from bs4 import BeautifulSoup
import pandas as pd
import datetime as datetime1
from datetime import datetime, timedelta
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# 不知道什么时候会过期, 过期后需要更换新的登录态COOKIE
WEIBO_COOKIE = __import__("os").environ.get("WEIBO_COOKIE", "")

HOT_COOKIE = __import__("os").environ.get("HOT_COOKIE", "")

class weibo_infos:
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
        except:
            if self.debug:
                print("获取关键词失败，请关闭无界面运行，手动验证！")
        df = pd.DataFrame()
        for keyword in keywords:
            if self.debug:
                print(f"开始爬取关键词: {keyword}")
            df = self.get_info(keyword, 2)
            if self.debug:
                print(f"关键词 {keyword} 爬取完成\n\n")
            time.sleep(random.uniform(1, 3))
        return df

    def get_keyword(self):
        url = "https://tophub.today/n/KqndgxeLl9"
        keywords = []
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
        try_cnt = 0
        while try_cnt < 3:
            try:
                driver.get(url)
                WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.TAG_NAME, "table"))
                )
                rows = driver.find_elements(By.XPATH, '//*[@id="page"]/div[3]/div/div[2]/div[2]/div[2]/div/div[1]/table/tbody/tr')
                for row in rows:
                    title = row.find_element(By.XPATH, './td[2]/a').text
                    keywords.append(title)
                break
            except Exception as e:
                if self.debug: print(f"Selenium 抓取失败: {e}")
                try_cnt += 1
                time.sleep(random.uniform(1, 3))
        return keywords

    def parse_weibo_count(self, count_str):
        """优化数值解析逻辑"""
        if not count_str: return 0
        count_str = count_str.strip()

        # 处理特殊格式
        if "万" in count_str:
            count_str = count_str.replace("万", "0000")
        elif "亿" in count_str:
            count_str = count_str.replace("亿", "00000000")

        match = re.search(r'(\d+\.?\d*)', count_str)
        return int(float(match.group(1))) if match else 0

    def get_info(self, keyword: str = '特朗普', page: int = 1):
        for page_i in range(1, page + 1):
            headers = {
                "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "accept-encoding": "gzip, deflate, br, zstd",
                "accept-language": "zh-CN,zh;q=0.9",
                "cache-control": "no-cache",
                "cookie": WEIBO_COOKIE,
                "pragma": "no-cache",
                "priority": "u=0, i",
                "sec-ch-ua": '"Chromium";v="140", "Not=A?Brand";v="24", "Microsoft Edge";v="140"',
                "sec-ch-ua-mobile": "?0",
                "sec-ch-ua-platform": '"Windows"',
                "sec-fetch-dest": "document",
                "sec-fetch-mode": "navigate",
                "sec-fetch-site": "none",
                "sec-fetch-user": "?1",
                "upgrade-insecure-requests": "1",
                "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1"
            }
            url = f'https://s.weibo.com/weibo?q={keyword}&typeall=1&suball=1&page={page_i}'
            if self.debug: print(f"正在爬取第 {page_i} 页, URL: {url}")
            response = requests.get(url, headers=headers)
            soup = BeautifulSoup(response.text, 'lxml')
            if soup.find('div', class_='card card-no-result'):
                return []
            for card in soup.find_all('div', class_='card'):
                item = {
                    "source": "微博",
                    "title": "",
                    "url": "",
                    "publish_time": "",
                    "author": "",
                    "content": "",
                    "like_count": 0,
                    "share_count": 0,
                    "comment_count": 0,
                    "read_count": 0,
                }
                # 基础信息
                author_tag = card.find('a', class_='name')
                item['author'] = author_tag.text.strip() if author_tag else "未知用户"

                # 内容处理
                content_tag = card.find('p', class_='txt')
                if content_tag:
                    content = content_tag.text.strip().replace('\u200b', '')
                    item['content'] = content.replace(' 展开c', '...')
                    if item['content'].count('#') >= 2:
                        item['title'] = item['content'].split('#')[1]
                        item['title'] = item['title'].split('#')[0]
                    if item['title'] == "":
                        item['title'] = content[:30] + "..." if len(content) > 30 else content

                # 时间与链接
                time_tag = card.find('a', href=re.compile(r'//weibo\.com/\d+/\w+'))
                if time_tag:
                    time_str = time_tag.text.strip()
                    try:
                        if "秒前" in time_str:
                            seconds = int(re.search(r'(\d+)', time_str).group(1))
                            item["publish_time"] = (datetime.now() - timedelta(seconds=seconds)).strftime("%Y-%m-%d %H:%M:%S")
                        elif "分钟前" in time_str:
                            minutes = int(re.search(r'(\d+)', time_str).group(1))
                            item["publish_time"] = (datetime.now() - timedelta(minutes=minutes)).strftime("%Y-%m-%d %H:%M:%S")
                        elif "小时前" in time_str:
                            hours = int(re.search(r'(\d+)', time_str).group(1))
                            item["publish_time"] = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
                        elif "今天" in time_str:
                            item["publish_time"] = datetime.now().strftime("%Y-%m-%d ") + time_str.replace("今天", "") + ":00"
                        elif "月" in time_str and "日" in time_str:
                            item["publish_time"] = datetime.strptime(time_str, "%m月%d日 %H:%M").strftime("%Y-%m-%d %H:%M:%S")
                        else:
                            item["publish_time"] = datetime.strptime(time_str, "%Y-%m-%d").strftime("%Y-%m-%d %H:%M:%S")
                    except Exception as e:
                        if self.debug: print('time:', time_str)
                        if self.debug: print('error:', e)
                    item['url'] = time_tag['href'].replace('//', 'https://')

                # 判断发布时间是否在范围内
                pt = item.get("publish_time")
                if not pt:
                    if self.debug:
                        print("发布时间缺失，跳过：", item)
                    continue
                try:
                    new = datetime.strptime(pt, "%Y-%m-%d %H:%M:%S")
                except Exception as e:
                    if self.debug:
                        print(f"发布时间解析失败 ({pt})，跳过。错误: {e}")
                        print(item)
                    continue
                # 如果 new < self.latest，说明这条的发布时间早于阈值（更旧），应跳过
                if new < self.latest:
                    if self.debug:
                        print(f"发布时间早于阈值，跳过: {new} < {self.latest}")
                        print(item)
                    continue

                # 互动数据计算（不显示）
                reposts = comments = likes = 0
                interaction_buttons = card.find_all('a',
                                                    class_="woo-box-flex woo-box-alignCenter woo-box-justifyCenter")
                if len(interaction_buttons) >= 3:
                    reposts = self.parse_weibo_count(interaction_buttons[-3].text.strip())
                    comments = self.parse_weibo_count(interaction_buttons[-2].text.strip())

                likes_tag = card.find('span', class_="woo-like-count")
                if likes_tag:
                    likes = self.parse_weibo_count(likes_tag.text.strip())
                item['like_count'] = likes
                item['share_count'] = reposts
                item['comment_count'] = comments
                item['read_count'] = max(likes, reposts, comments)
                self.threads.append(item)
                self.threads_count += 1
        return self.deal_info()


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
    x = weibo_infos(6, True, True)
    x.crawl_data()
