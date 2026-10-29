"""
南方周末库的适配器
有且仅有此脚本可直接调用south_crawler
"""

import json
import south_weekend.south_crawler as south_crawler

class SouthAdaptor:
    def search(self, keyword: str = '南方周末', page: int = 1) -> list:
        return south_crawler.search(keyword, page)


if __name__ == "__main__":
    south_adaptor = SouthAdaptor()
    res = south_adaptor.search()

    for item in res:
        print(item)
    print("len:", len(res))
