"""
对外暴露的爬虫API
"""
# from adaptor import ToutiaoAdaptor
# from filter import Filter
# from transformer import Transformer

from toutiao.adaptor import ToutiaoAdaptor
from toutiao.filter import Filter
from toutiao.transformer import ToutiaoTransformer
from common.constant import SourceEnum

class ToutiaoApi:
    def __init__(self):
        # 初始化实例属性
        self.adaptor = ToutiaoAdaptor()
        self.transformer = ToutiaoTransformer()
        self.source = SourceEnum.toutiao
        self.name = self.source.name

    def search(self, keyword: str = "特朗普", page: int = 0):
        html_content = self.adaptor.get_search_results(keyword, page)
        data_before = self.adaptor.parse_search_results(html_content)
        if not data_before:
            return []
        data_arr = Filter(data_before)
        data_arr = self.transformer.batch_transform(data_arr)
        return data_arr


if __name__ == "__main__":
    toutiao_api = ToutiaoApi()
    res = toutiao_api.search()
    for item in res:
        print(item)
