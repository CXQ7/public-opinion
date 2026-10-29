from douyin.api import DouyinApi
from toutiao.api import ToutiaoApi
from weibo.api import WeiboApi
from wechat.api import WechatApi
from south_weekend.api import SouthApi
from xiaohongshu.api import XHSApi
import common.constant as const
from common.constant import SourceEnum

class ApiFactory:
    def __init__(self):
        print("Initializing ApiFactory")
        self.api_map = self.init_api_map()

    def get_api(self, source: SourceEnum):
        return self.api_map.get(source)

    # 仅初始化source_set中的api
    def init_api_map(self):
        api_map = {}
        for source in const.source_set:
            if source == SourceEnum.douyin:
                api_map[SourceEnum.douyin] = DouyinApi()
            elif source == SourceEnum.toutiao:
                api_map[SourceEnum.toutiao] = ToutiaoApi()
            elif source == SourceEnum.weibo:
                api_map[SourceEnum.weibo] = WeiboApi()
            elif source == SourceEnum.wechat:
                api_map[SourceEnum.wechat] = WechatApi()
            elif source == SourceEnum.south_weekend:
                api_map[SourceEnum.south_weekend] = SouthApi()
            elif source == SourceEnum.xiaohongshu:
                api_map[SourceEnum.xiaohongshu] = XHSApi()
        return api_map
