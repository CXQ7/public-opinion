"""
对外暴露的爬虫API
"""
import sys

sys.path.append("douyin")
sys.path.append("douyin/douyin_lib")
sys.path.append("douyin/douyin_lib/lib")

from south_weekend.adaptor import SouthAdaptor
from south_weekend.transformer import SouthWeekendTransformer
from common.constant import SourceEnum

class SouthApi:
    def __init__(self):
        # 初始化实例属性
        self.adaptor = SouthAdaptor()
        self.transformer = SouthWeekendTransformer()
        self.source = SourceEnum.south_weekend
        self.name = self.source.name

    def search(self, keyword: str = "拜登", page: int = 0):
        data_arr = self.adaptor.search(keyword, page)
        data_arr = self.transformer.batch_transform(data_arr)
        return data_arr


if __name__ == "__main__":
    southApi = SouthApi()
    res = southApi.search(page=1)

    for item in res:
        print(item)
