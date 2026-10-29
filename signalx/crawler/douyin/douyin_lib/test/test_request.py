from lib.request import Request
import pytest
# cookie = '__CONFIGURE_LOCALLY__'
# ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36'
# r = Request(cookie, UA=ua)
r = Request()


def get_aweme_detail(id='7379453318386437428') -> tuple[dict, bool]:
    params = {"aweme_id": id}
    uri = '/aweme/v1/web/aweme/detail/'
    resp = r.getJSON(uri, params)
    aweme_detail = resp.get('aweme_detail')
    return aweme_detail


def get_hash_detail(id='1749186610476046'):
    params = {"ch_id": id, "query_type": 0}
    uri = '/aweme/v1/web/challenge/detail/'
    resp = r.getJSON(uri, params)
    ch_info = resp.get('ch_info')
    return ch_info


def request_search(keyword: str = '抖音', max_cursor: int = 0, limit: int = 10) -> tuple[dict, bool]:
    """
    请求抖音获取搜索信息
    """
    params = {
        "search_id": '',
        "search_channel": "aweme_video_web",
        "search_source": "tab_search",
        "query_correct_type": 1,
        "from_group_id": "",
        "is_filter_search": 1,
        "list_type": "single",
        "need_filter_settings": 1,
        "offset": max_cursor,
        "sort_type": 1,  # 排序 综合 1最多点赞 2最新
        "enable_history": 1,
        "search_range": 0,  # 搜索范围  不限
        "publish_time": 0,  # 发布时间  不限
        "filter_duration": '',  # 时长 不限 0-1  1-5  5-10000
        'count': 18,
        "keyword": keyword
    }
    resp = r.getJSON('/aweme/v1/web/search/item/', params)
    ret = resp.get('data')
    return ret


def request_search_user(keyword: str = '新华社', max_cursor: int = 0, limit: int = 10) -> tuple[dict, bool]:
    """
    请求抖音获取搜索信息
    """
    params = {
        'count': 10,
        "from_group_id": "",
        "is_filter_search": 0,
        "keyword": keyword,
        "list_type": "single",
        "need_filter_settings": 0,
        "offset": max_cursor,
        # "search_id": '',
        "query_correct_type": 1,
        "search_channel": "aweme_user_web",
        "search_source": "tab_search"
    }
    resp = r.getJSON('/aweme/v1/web/discover/search/', params)
    ret = resp.get('user_list')
    return ret


@pytest.mark.parametrize('function', [get_aweme_detail, get_hash_detail, request_search, request_search_user])
def test_request(function):
    assert function()


def test_getHTML():
    assert r.getHTML('https://v.douyin.com/UhYnoMS/')
