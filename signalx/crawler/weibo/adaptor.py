import datetime
import json
import requests
import copyheaders
import re
from bs4 import BeautifulSoup
from datetime import datetime, timedelta
from pathlib import Path
import os
from common.proxy import get_proxy

headers = copyheaders.headers_raw_to_dict(b"""
cookie:__CONFIGURE_LOCALLY__
User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1
""")


def _build_headers():
    # 避免请求时对全局 headers 做原地修改
    return dict(headers)

def judge_fn(response):
    # 状态码不是200直接判为无效
    if response.status_code != 200:
        print("响应状态码异常:", response.status_code)
        return False
    text = response.text
    # 内容太短，直接判为无效
    if len(text) < 2000:
        print("响应内容过短，可能被反爬或代理异常")
        return False
    # 判断是否为微博正常页面
    soup = BeautifulSoup(text, 'lxml')
    # 1. 检查是否有微博卡片
    if soup.find('div', class_='card'):
        return True
    # 2. 检查是否为无结果页面
    if soup.find('div', class_='card card-no-result'):
        print("页面无搜索结果")
        return False
    # 3. 检查是否为登录页、验证码页、异常页
    if "请输入验证码" in text or "登录" in text or "人机验证" in text:
        print("遇到验证码或登录页，疑似被反爬")
        return False
    # 4. 检查是否为其它网站页面（如代理被劫持）
    if "GitLab" in text or "Cisco" in text or "<title>" in text and "微博" not in text:
        print("代理返回非微博页面，丢弃该代理")
        return False
    # 默认判为无效
    print("页面结构异常，丢弃该代理")
    return False

class WeiboAdaptor:

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

    def search(self, keyword: str = '特朗普', page: int = 1):
        data_arr = []
        url = f'https://s.weibo.com/weibo?q={keyword}&typeall=1&suball=1&page={page}'
        try:
            # response = get_proxy(source="weibo", url=url, headers=headers, verify=False, judge_fn=judge_fn)
            response = requests.get(url, headers=_build_headers(), timeout=10, verify=False)
        except requests.exceptions.RequestException as e:
            print("Request failed:", e)
            return []

        soup = BeautifulSoup(response.text, 'lxml')
        if soup.find('div', class_='card card-no-result'):
            return []
        for card in soup.find_all('div', class_='card'):
            item = {
                "source": "微博",
                "title": "",
                "author": "",
                "content": "",
                "heat": 0,
                "publish_time": "",
                "url": ""
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
                    elif "年" in time_str and "月" in time_str and "日" in time_str:
                        # 支持如 2025年06月13日 17:37
                        item["publish_time"] = datetime.strptime(time_str, "%Y年%m月%d日 %H:%M").strftime("%Y-%m-%d %H:%M:%S")
                    elif "月" in time_str and "日" in time_str:
                        item["publish_time"] = datetime.strptime(time_str, "%m月%d日 %H:%M").strftime("%Y-%m-%d %H:%M:%S")
                    else:
                        item["publish_time"] = datetime.strptime(time_str, "%Y-%m-%d").strftime("%Y-%m-%d %H:%M:%S")
                    item["publish_time"] = datetime.strptime(item["publish_time"], "%Y-%m-%d %H:%M:%S")
                except Exception as e:
                    print('time:', time_str)
                    print('error:', e)
                item['url'] = time_tag['href'].replace('//', 'https://')

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

            # 计算综合热度
            item['heat'] = int(reposts * 10 + comments * 20 + likes * 25)

            data_arr.append(item)
        return data_arr


if __name__ == "__main__":
    adaptor = WeiboAdaptor()
    results = adaptor.search("特朗普", page=1)
    for item in results:
        print(item)
