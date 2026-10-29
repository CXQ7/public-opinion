"""
抖音库的适配器
有且仅有此脚本可直接调用douyin_lib库
"""

import json

from .douyin_lib.lib.request import Request

# cookie = '__CONFIGURE_LOCALLY__'
COOKIE = '__CONFIGURE_LOCALLY__'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36'


class DouyinAdaptor:
    def __init__(self):
        # 初始化实例属性
        self.request_instance = Request(UA=UA, cookie=COOKIE)

    def search(self, keyword: str = '抖音', page: int = 0, page_size: int = 18) -> tuple[dict, bool]:
        offset = page * page_size
        params = {
            "search_id": '',
            "search_channel": "aweme_video_web",
            "search_source": "tab_search",
            "query_correct_type": 1,
            "from_group_id": "",
            "is_filter_search": 1,
            "filter_selected": {"sort_type": "2", "publish_time": "0"},  # 按时间
            "list_type": "single",
            "need_filter_settings": 0,
            "offset": offset,
            "sort_type": 2,  # 排序 综合 1最多点赞 2最新
            "enable_history": 1,
            # "search_range": 0,  # 搜索范围  不限
            # "publish_time": 0,  # 发布时间  不限
            # "filter_duration": '',  # 时长 不限 0-1  1-5  5-10000
            'count': page_size,
            "keyword": keyword
        }
        url = '/aweme/v1/web/search/item/'
        resp = self.request_instance.getJSON(url, params)
        ret = resp.get('data')
        return ret

    def search_v2(self, keyword: str = '抖音', page: int = 1, page_size: int = 10) -> tuple[dict, bool]:
        offset = page * page_size
        params = {
            "device_platform": "webapp",
            "aid": 6383,
            "channel": "channel_pc_web",
            "search_channel": "aweme_general",
            "enable_history": 1,
            "keyword": keyword,
            "search_source": "normal_search",
            "query_correct_type": 1,
            "is_filter_search": 1,
            "filter_selected": {"sort_type": "2", "publish_time": "0"},  # 按发布时间降序
            "from_group_id": "",
            "offset": offset,  # 页面偏移量
            "count": page_size,  # 页面大小
            "need_filter_settings": 0,
            "list_type": "multi",
            "search_id": "20250203000116C445D264C3747BF90D82",
            "update_version_code": 170400,
            "pc_client_type": 1,
            "pc_libra_divert": "Windows",
            "support_h265": 1,
            "support_dash": 1,
            "version_code": 190600,
            "version_name": "19.6.0",
            "cookie_enabled": True,
            "screen_width": 1536,
            "screen_height": 864,
            "browser_language": "zh-CN",
            "browser_platform": "Win32",
            "browser_name": "Edge",
            "browser_version": "127.0.0.1",
            "browser_online": True,
            "engine_name": "Blink",
            "engine_version": "127.0.0.1",
            "os_name": "Windows",
            "os_version": 10,
            "cpu_core_num": 20,
            "device_memory": 8,
            "platform": "PC",
            "downlink": 10,
            "effective_type": "4g",
            "round_trip_time": 50,
            "webid": "7461470133350057484",
            "uifid": "26198ff38959f773c63a6fc9b3542e2fdcfd2f10d2782124ed1adc24709862df565867b0d52abf89aad1acac9c4edac6cc6312d417846060b2698748364a3f47264879a3be67142c2f29a3e477ff888920fcc6ef76da5661e16699a0905f9b35a2d2a38c4e429d4391615dfd618f7571b385ac427bf663b7abdd66f889064dfcf085ff17c24c6039045cf154b98c3c71759dfdc9143f3ad025cd0d0a64622143",
            # "msToken": "wLBCH48dKVCEibXUwrEKaRao2iuy4n1taqbjVMlC4fHyDDqL6wTyowtuutzg590K5TGcEgBC880ACPgC_pp87HhKqR53p3SRZKS23TjWby2QyArUj70Z6LQuA1S3jQyQXFsgH9p2p6GLWahve2Ec09MAona1hCJN5qZxTraE6D0Z4v8JzvK9OpOU",
        }
        url = "/aweme/v1/web/general/search/single/"
        resp = self.request_instance.getJSON(url, params)
        ret = resp.get('data')
        # print(params)
        return ret

    def search_v3(self, keyword: str = '抖音', page: int = 1, page_size: int = 10) -> tuple[dict, bool]:
        offset = page * page_size
        params = {
            "device_platform": "webapp",
            "aid": 6383,
            "channel": "channel_pc_web",
            "search_channel": "aweme_general",
            "enable_history": 1,
            "keyword": keyword,
            "search_source": "normal_search",
            "query_correct_type": 1,
            "is_filter_search": 1,
            "filter_selected": {"sort_type": "2", "publish_time": "0"},  # 按发布时间降序
            "from_group_id": "",
            "offset": offset,  # 页面偏移量
            "count": page_size,  # 页面大小
            "need_filter_settings": 0,
            "list_type": "multi",
            # "search_id": "20250203000116C445D264C3747BF90D82",
            "update_version_code": 170400,
            "pc_client_type": 1,
            "pc_libra_divert": "Windows",
            "support_h265": 1,
            "support_dash": 1,
            "version_code": 190600,
            "version_name": "19.6.0",
            "cookie_enabled": True,
            "screen_width": 1536,
            "screen_height": 864,
            "browser_language": "zh-CN",
            "browser_platform": "Win32",
            "browser_name": "Edge",
            "browser_version": "127.0.0.1",
            "browser_online": True,
            "engine_name": "Blink",
            "engine_version": "127.0.0.1",
            "os_name": "Windows",
            "os_version": 10,
            "cpu_core_num": 20,
            "device_memory": 8,
            "platform": "PC",
            "downlink": 10,
            "effective_type": "4g",
            "round_trip_time": 50,
            "webid": "7461470133350057484",
            "uifid": "26198ff38959f773c63a6fc9b3542e2fdcfd2f10d2782124ed1adc24709862df565867b0d52abf89aad1acac9c4edac6cc6312d417846060b2698748364a3f47264879a3be67142c2f29a3e477ff888920fcc6ef76da5661e16699a0905f9b35a2d2a38c4e429d4391615dfd618f7571b385ac427bf663b7abdd66f889064dfcf085ff17c24c6039045cf154b98c3c71759dfdc9143f3ad025cd0d0a64622143",
            # "msToken": "wLBCH48dKVCEibXUwrEKaRao2iuy4n1taqbjVMlC4fHyDDqL6wTyowtuutzg590K5TGcEgBC880ACPgC_pp87HhKqR53p3SRZKS23TjWby2QyArUj70Z6LQuA1S3jQyQXFsgH9p2p6GLWahve2Ec09MAona1hCJN5qZxTraE6D0Z4v8JzvK9OpOU",
        }
        url = "/aweme/v1/web/general/search/stream/"
        print(params)
        resp = self.request_instance.getJSON(url, params)
        ret = resp.get('data')
        return ret


if __name__ == "__main__":
    douyin_api = DouyinAdaptor()
    res = douyin_api.search_v1('美国', 1, 10)

    for item in res:
        print(item)
    print("len:", len(res))

    with open("logs/douyin.json", "w") as file:
        json.dump(res, file)
    # print(res)
