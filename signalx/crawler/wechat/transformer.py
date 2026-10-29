"""
数据转化器
将爬取的数据转化为统一的格式，便于后续分析 OR 推送 OR 存入数据库
"""
from datetime import datetime, timedelta
from bs4 import BeautifulSoup
from common.base_transformer import BaseTransformer

class WechatTransformer(BaseTransformer):
    def get_relative_time(self, timestamp):
        if (type(timestamp) == int):
            article_time = datetime.fromtimestamp(timestamp)
            return article_time
        else:
            if (type(timestamp) == str):
                try:
                    article_time = datetime.strptime(timestamp, "%Y-%m-%d %H:%M:%S")
                    return article_time
                except:
                    return None

    def transform_article(self, article):
        """转换单篇文章的格式"""
        return {
            "标题": article["title"],
            "作者": article["author"],
            "内容": article["content"],
            "发布时间": datetime.fromtimestamp(article["create_time"]).strftime("%Y-%m-%d %H:%M:%S"),
            "链接": article["link"]
        }

    def extract_text_content(self, html_content):
        """提取HTML中的纯文本内容"""
        soup = BeautifulSoup(html_content, 'html.parser')
        # 移除script和style标签
        for script in soup(["script", "style"]):
            script.decompose()
        # 获取文本并清理
        text = soup.get_text(separator=' ', strip=True)
        # 清理多余的空白字符
        text = ' '.join(text.split())
        return text

    def _normalize_func(self, raw_item):
        raw_item["title"] = raw_item["title"].replace("<em>", "").replace("</em>", "")
        raw_item["content"] = self.extract_text_content(raw_item["content"])
        content_preview = raw_item["content"][:100] + "..." if len(raw_item["content"]) > 100 else raw_item["content"]
        raw_item["create_time"] = self.get_relative_time(raw_item["create_time"])
        return {
            "source": "微信公众号",
            "title": raw_item['title'][:32],
            "author": raw_item['author'],
            "content": content_preview,
            "heat": 10*raw_item['comment_count'],
            "publish_time": raw_item['create_time'],
            "url": raw_item['link'][:512] if raw_item['link'] else ""
        }
