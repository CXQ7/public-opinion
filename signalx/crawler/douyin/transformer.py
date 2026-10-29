"""
数据转化器
将爬取的数据转化为统一的格式，便于后续分析 OR 推送 OR 存入数据库
"""

import json

from common.base_transformer import BaseTransformer
from common.constant import SourceEnum


class DouyinTransformer(BaseTransformer):
    def _normalize_func(self, raw_item) -> dict:
        return {
            "third_id": raw_item['third_id'],
            "source": SourceEnum.douyin.name,
            "title": raw_item['content'][:32],
            "author": raw_item['author'],
            "content": raw_item['content'],
            "heat": raw_item['like_count'],
            "publish_time": raw_item['publish_datetime'],
            "url": raw_item['url'],
            "ext": {
                "img": raw_item['img'],
                "like_count": raw_item['like_count'],
                "abstract_publish_datetime": raw_item['abstract_publish_datetime']
            }
        }


if __name__ == "__main__":
    with open('logs/douyin.json', 'r') as file:
        data = json.load(file)
    transformer = DouyinTransformer()
    transformed_data = transformer.batch_transform(data)
    print(transformed_data)
    for item in transformed_data:
        print(item)
