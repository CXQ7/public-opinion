from common.base_transformer import BaseTransformer
from common.constant import SourceEnum

"""
数据转化器
将爬取的数据转化为统一的格式，便于后续分析 OR 推送 OR 存入数据库
"""

class WeiboTransformer(BaseTransformer):
    def _normalize_func(self, raw_item) -> dict:
        return {
            "source": "微博",
            "title": raw_item['title'][:32],
            "author": raw_item['author'],
            "content": raw_item['content'],
            "heat": 10*raw_item['heat'],
            "publish_time": raw_item['publish_time'],
            "url": raw_item['url']
        }
