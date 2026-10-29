"""
对外暴露的爬虫API
"""
# from adaptor import WeiboAdaptor
# from transformer import Transformer

from weibo.adaptor import WeiboAdaptor
from weibo.transformer import WeiboTransformer
from common.constant import SourceEnum

class WeiboApi:
    def __init__(self):
        self.adaptor = WeiboAdaptor()
        self.transformer = WeiboTransformer()
        self.source = SourceEnum.weibo
        self.name = self.source.name

    def search(self, keyword: str = "特朗普", page: int = 2):
        results = self.adaptor.search(keyword, page + 1)
        return self.transformer.batch_transform(results)

if __name__ == "__main__":
    api = WeiboApi()
    for i in range(0, 3):
        results = api.search(keyword="特朗普", page=i)
        for item in results:
            print(item)
