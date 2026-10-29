"""
微信公众号爬虫API
"""
# from adaptor import WechatAdaptor

from wechat.adaptor import WechatAdaptor
from common.constant import SourceEnum

class WechatApi:
    def __init__(self, public_account_list: list = ["求是网", "人民日报评论", "澎湃新闻", "半月谈", "财经早餐", "第一财经", "券商中国", "正和岛", "凤凰网财经", "凤凰网科技"], debug: bool = False):
        self.debug = debug
        self.adaptor = WechatAdaptor(public_account_list=public_account_list ,debug=debug)
        self.source = SourceEnum.wechat
        self.name = self.source.name

    def search(self, keyword: str = "特朗普", page: int = 1):
        """
        搜索微信公众号文章
        Args:
            keyword: 搜索关键词
            public_account_list: 要搜索的公众号列表
            debug: 是否开启调试模式
        Returns:
            list: 包含文章信息的列表，每篇文章格式如下：
            {
                "source": "微信公众号",
                "title": "文章标题",
                "author": "作者",
                "content": "文章内容预览...",
                "comment_count": 0,
                "publish_time": "发布时间",
                "url": "文章链接"
            }
        """
        try:
            results = self.adaptor.search(
                keyword=keyword,
                page=page,
                debug=self.debug
            )
            return results if results else []
        except Exception as e:
            if self.debug:
                print(f"WechatApi search error: {str(e)}")
            return []

if __name__ == "__main__":
    # 测试代码
    api = WechatApi(debug=True)
    results = api.search(page=1)
    for item in results:
        print(item)
