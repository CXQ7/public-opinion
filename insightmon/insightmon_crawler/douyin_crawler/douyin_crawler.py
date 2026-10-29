import os
import pandas as pd
import datetime as datetime1
from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
import json

class douyin_infos:
    def __init__(self, hoursBefore, debug = False, output_as_file=False):
        """对象初始化"""
        self.threads = []
        self.threads_count = 0
        self.write_count = 0
        self.debug = debug
        self.output_as_file = output_as_file
        self.latest = datetime.today() - datetime1.timedelta(hours=6)
        if self.debug:
            print("最后一次的爬取时间:", self.latest, '\n')


    def add_cookies(self, driver):
        """添加cookies"""
        cookies_file = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'cookies.json')
        if not os.path.exists(cookies_file):
            print("Cookies 文件不存在！")
            return

        with open(cookies_file, 'r', encoding='utf-8') as f:
            cookies = json.load(f)

        for cookie in cookies:
            # 确保 cookie 的 domain 属性与当前页面匹配
            if 'domain' in cookie and cookie['domain'].startswith('.'):
                cookie['domain'] = cookie['domain'][1:]
            try:
                driver.add_cookie(cookie)
            except Exception as e:
                print(f"添加 cookie 失败: {e}")
        return driver


    def crawl_data(self):
        url = "https://tophub.today/n/DpQvNABoNE"
        links = []
        options=Options()
        # options.add_argument('-headless')  #无界面运行
        options.add_argument('headers')  #添加请求头
        options.add_argument('-ignore-certificate-errors')
        options.add_argument('-ignore -ssl-errors')
        options.add_experimental_option('excludeSwitches', ['enable-automation'])  #设置开发者模式启动
        # service = Service("C:\\Users\\ASUS\\.cache\\selenium\\chromedriver\\win64\\140.0.7339.207\\chromedriver.exe")
        # driver = webdriver.Chrome(options = options, service=service)
        driver = webdriver.Chrome(options = options)
        try:
            driver.get(url)
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.TAG_NAME, "table"))
            )
            rows = driver.find_elements(By.XPATH, '//*[@id="page"]/div[3]/div/div[2]/div[2]/div[2]/div/div[1]/table/tbody/tr')
            for row in rows:
                links.append(row.find_element(By.XPATH, './td[3]/div[1]/a').get_attribute('href'))
        except Exception as e:
            print(f"Selenium 抓取失败: {e}")

        for link in links:
            try_cnt = 0
            while try_cnt < 3:
                try:
                    if self.debug:
                        print(f"crawling link {link}")
                    driver.get(link)
                    driver = self.add_cookies(driver)  # 调用 add_cookies 方法添加 cookies
                    driver.refresh()  # 刷新页面以应用 cookies
                    driver.implicitly_wait(10)  # 隐式等待，最长等10秒
                    t = {}
                    t["source"] = '抖音'
                    t["title"] = driver.find_element(By.XPATH, '//*[@id="douyin-right-container"]/div[2]/div/div/div[1]/div[3]/div/div[1]/div/h1/span/span[2]/span/span[1]/span/span/span').text
                    if self.debug: print(f"title: {t['title']}")
                    t["url"] = link
                    t["publish_time"] = driver.find_element(By.XPATH, '//*[@id="douyin-right-container"]/div[2]/div/div/div[1]/div[3]/div/div[2]/div[2]/span').text
                    t["publish_time"] = t["publish_time"].split('：')[1]
                    t["publish_time"] = datetime.strptime(t["publish_time"], "%Y-%m-%d %H:%M")
                    t["publish_time"] = t["publish_time"].strftime("%Y-%m-%d %H:%M:%S")

                    # 判断发布时间是否在范围内
                    new = datetime.strptime(t["publish_time"], "%Y-%m-%d %H:%M:%S")
                    if new < self.latest: break

                    if self.debug: print(f"publish_time: {t['publish_time']}")
                    t["author"] = driver.find_element(By.XPATH, '//*[@id="douyin-right-container"]/div[2]/div/div/div[2]/div/div[1]/div[2]/a/div').text
                    if self.debug: print(f"author: {t['author']}")
                    t["content"] = t["title"]
                    t['like_count'] = driver.find_element(By.XPATH, '//*[@id="douyin-right-container"]/div[2]/div/div/div[1]/div[3]/div/div[2]/div[1]/div[1]/span').text
                    if '万' in t['like_count']:
                        t['like_count'] = int(float(t['like_count'].replace('万', '')) * 10000)
                    else:
                        t['like_count'] = int(t['like_count'])
                    if self.debug: print(f"like_count: {t['like_count']}")
                    t['share_count'] = driver.find_element(By.XPATH, '//*[@id="douyin-right-container"]/div[2]/div/div/div[1]/div[3]/div/div[2]/div[1]/div[4]/span').text
                    if '万' in t['share_count']:
                        t['share_count'] = int(float(t['share_count'].replace('万', '')) * 10000)
                    else:
                        t['share_count'] = int(t['share_count'])
                    if self.debug: print(f"share_count: {t['share_count']}")
                    t["comment_count"] = driver.find_element(By.XPATH, '//*[@id="douyin-right-container"]/div[2]/div/div/div[1]/div[3]/div/div[2]/div[1]/div[2]/span').text
                    if '万' in t['comment_count']:
                        t['comment_count'] = int(float(t['comment_count'].replace('万', '')) * 10000)
                    else:
                        t['comment_count'] = int(t['comment_count'])
                    if self.debug: print(f"comment_count: {t['comment_count']}")
                    t['read_count'] = max(t['like_count'], t['share_count'], t['comment_count'])
                    self.threads.append(t)
                    if self.debug:
                        print(t)
                        print("Succeeded in crawling one thread!\n")
                    self.threads_count += 1
                    break
                except:
                    try_cnt += 1

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
    x = douyin_infos(6, debug=True, output_as_file=True)
    x.crawl_data()
