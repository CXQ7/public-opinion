"""
数据转化器
将爬取的数据转化为统一的格式，便于后续分析 OR 推送 OR 存入数据库
"""

import json
import time
from common.base_transformer import BaseTransformer
from common.constant import SourceEnum

class SouthWeekendTransformer(BaseTransformer):
    def _normalize_func(self, raw_item) -> dict:
        title = raw_item.get('short_subject', '')[:32]
        author = raw_item.get('author', '')
        content = raw_item.get('introtext', '')
        publish_time = raw_item.get('publish_time', '')
        url = raw_item.get('url', '') if raw_item.get('url') else ""

        comment_count = int(raw_item.get('comment_count', 0))
        heat = 10 * comment_count

        return {
            "source": "南方周末",
            "title": title,
            "author": author,
            "content": content,
            "heat": heat,
            "publish_time": publish_time,
            "url": f'https://www.infzm.com/contents/{url}'
        }
