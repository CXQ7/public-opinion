"""
数据转化器
将爬取的数据转化为统一的格式，便于后续分析 OR 推送 OR 存入数据库
"""
import re
from datetime import datetime, timedelta
from common.base_transformer import BaseTransformer
from common.constant import SourceEnum

class ToutiaoTransformer(BaseTransformer):
    def transTime(self, time_str):
        try:
            if time_str is None or time_str == "":
                return None
            if time_str.find("刚刚") != -1:
                return datetime.now()
            if time_str.find("秒前") != -1:
                seconds = int(re.search(r'(\d+)', time_str).group(1))
                return (datetime.now() - timedelta(seconds=seconds))
            if time_str.find("分钟前") != -1:
                seconds = int(re.search(r'(\d+)', time_str).group(1))
                return datetime.strptime((datetime.now() - timedelta(seconds=seconds)).strftime("%Y-%m-%d %H:%M") + ':00', "%Y-%m-%d %H:%M:%S")
            if time_str.find("小时前") != -1:
                hours = int(re.search(r'(\d+)', time_str).group(1))
                return datetime.strptime((datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H") + ':00:00', "%Y-%m-%d %H:%M:%S")
            if time_str.find("昨天") != -1:
                time_str = time_str.replace("昨天", "")
                return datetime.strptime((datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d") + ' ' + time_str, "%Y-%m-%d %H:%M")
            if time_str.find("前天") != -1:
                time_str = time_str.replace("前天", "")
                return datetime.strptime((datetime.now() - timedelta(days=2)).strftime("%Y-%m-%d") + ' ' + time_str, "%Y-%m-%d %H:%M")
            if time_str.find("天前") != -1:
                days = int(re.search(r'(\d+)', time_str).group(1))
                return datetime.strptime((datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d") + ' ' + '00:00:00', "%Y-%m-%d %H:%M:%S")
            if time_str.find("年") != -1 and time_str.find("月") != -1 and time_str.find("日") != -1:
                time_str = time_str.replace("年", "-").replace("月", "-").replace("日", "")
                return datetime.strptime(time_str, "%Y-%m-%d")
            if time_str.find("月") != -1 and time_str.find("日") != -1:
                time_str = time_str.replace("月", "-").replace("日", "")
                return datetime.strptime(str(datetime.now().year) + "-" + time_str, "%Y-%m-%d")
        except:
            return None

    def _normalize_func(self, raw_item) -> dict:
        return {
            "source": "今日头条",
            "title": raw_item['title'][:32],
            "author": raw_item['author'],
            "content": raw_item['content'],
            "heat": 10 * raw_item['comment_count'],
            "publish_time": self.transTime(raw_item['publish_time']),
            "url": raw_item['url'][:512] if raw_item['url'] else ""
        }
