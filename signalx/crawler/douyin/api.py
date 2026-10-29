"""
对外暴露的爬虫API
"""
from publicity_idl.domain import News

from .adaptor_v1 import DouyinAdaptor
from .transformer import DouyinTransformer
from common.constant import SourceEnum

class DouyinApi:
    def __init__(self):
        # 初始化实例属性
        self.adaptor = DouyinAdaptor()
        self.transformer = DouyinTransformer()
        self.source = SourceEnum.douyin
        self.name = self.source.name

    def search(self, keyword: str = "拜登", page: int = 0, page_size: int = 10) -> list[News]:
        raw_arr = self.adaptor.search_v3(keyword, page, page_size)
        data_arr = self.transformer.batch_transform(raw_arr)
        return data_arr


if __name__ == "__main__":
    douyin_api = DouyinApi()
    print("Starting Douyin API search...")
    try:
        res = douyin_api.search(page=1)
        print(f"Search result: {res}")
        for item in res:
            print(item)
        for i in range(len(res)):
            res[i].task_id = res[i].slice_id = 1
        print("Finished processing results.")
    except Exception as e:
        print(f"An error occurred: {e}")

    # new_res = record_repo.create_batch(res)
    # for item in new_res:
    #     print(item)
