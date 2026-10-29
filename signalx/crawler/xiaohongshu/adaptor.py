
import xiaohongshu.xhs_crawler as XHS_crawler

class XHSAdaptor:
    def search(self, keyword: str = '小红书', page: int = 1) -> list:
        return XHS_crawler.search(keyword, page)
