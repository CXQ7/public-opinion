"""
对外暴露的爬虫API
"""

from xiaohongshu.adaptor import XHSAdaptor
from xiaohongshu.transformer import XHSTransformer
from common.constant import SourceEnum

class XHSApi:
    def __init__(self):
        # 初始化实例属性
        self.adaptor = XHSAdaptor()
        self.transformer = XHSTransformer()
        self.source = SourceEnum.xiaohongshu
        self.name = self.source.name

    def search(self, keyword: str = "奶龙", page: int = 0):
        page = 1 + page  # 小红书的页码从1开始
        data_arr = self.adaptor.search(keyword, page)
        data_arr = self.transformer.batch_transform(data_arr)
        return data_arr
