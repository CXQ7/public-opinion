import requests
import execjs
import os
from pathlib import Path
from .xhs_utils import *
from common.proxy import post_proxy
from xhshow import Xhshow
from utils import logger

filepath = os.path.dirname(__file__)
client = Xhshow()

# 不知道什么时候会过期, 过期后需要更换新的登录态COOKIE
# 优先级：环境变量 XHS_COOKIE > crawler/config/xhs_cookie.txt > 代码内默认值
_DEFAULT_COOKIE = __import__("os").environ.get("_DEFAULT_COOKIE", "")

_LOGIN_EXPIRED_WARNED = False


def _load_xhs_cookie() -> str:
    env_cookie = os.getenv("XHS_COOKIE")
    if env_cookie and env_cookie.strip():
        return env_cookie.strip()

    cookie_file = Path(__file__).resolve().parents[1] / "config" / "xhs_cookie.txt"
    if cookie_file.exists():
        try:
            text = cookie_file.read_text(encoding="utf-8").strip()
            if text:
                return text
        except Exception:
            pass

    return _DEFAULT_COOKIE
# header = client.sign_headers_get(
#     uri="https://edith.xiaohongshu.com/api/sns/web/v1/search/notes",  # 完整 URL（推荐）
#     # uri="/api/sns/web/v1/user_posted",  # 或者只传 URI 路径
#     cookies=COOKIE,  # 传入完整 cookies
#     payload={"num": "30", "cursor": "", "user_id": "123"}
# )

def judge_fn(response):
    try:
        json_data = response.json()
    except Exception as e:
        logger.warning(f"XHS 响应不是合法 JSON: {e}")
        logger.debug(f"XHS 原始响应(head)={response.text[:500]}")
        return False
    if json_data.get('code', -1) != 0:
        logger.warning(f"XHS judge_fn: code!=0 json_data={json_data}")
        return False
    # 判断 'items' 字段是否存在且为列表
    if not isinstance(json_data.get('data', {}).get('items', None), list):
        logger.warning(f"XHS judge_fn: missing data.items json_data={json_data}")
        return False
    return True

def search(keyword: str = "奶龙", page: int = 1, page_size: int = 20):
    url = "https://edith.xiaohongshu.com/api/sns/web/v1/search/notes"
    cookie = _load_xhs_cookie()
    # headers = {
    #     "accept": "application/json, text/plain, */*",
    #     "accept-encoding": "gzip, deflate, br, zstd",
    #     "accept-language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
    #     "content-length": "176",
    #     "content-type": "application/json;charset=UTF-8",
    #     "dnt": "1",
    #     "origin": "https://www.xiaohongshu.com",
    #     "priority": "u=1, i",
    #     "referer": "https://www.xiaohongshu.com/",
    #     "sec-ch-ua": "\"Not(A:Brand\";v=\"99\", \"Microsoft Edge\";v=\"133\", \"Chromium\";v=\"133\"",
    #     "sec-ch-ua-mobile": "?0",
    #     "sec-ch-ua-platform": "\"Windows\"",
    #     "sec-fetch-dest": "empty",
    #     "sec-fetch-mode": "cors",
    #     "sec-fetch-site": "same-site",
    #     "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1",
    #     "x-b3-traceid": "a3182781eee4b68c",
    #     "x-mns": "unload",
    #     "x-s": header["x-s"],
    #     # "x-s-common": "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",
    #     "x-t": str(int(time.time() * 1000)),
    #     "x-xray-traceid": "ca9eb2bef107ac78753803d3b9740ee0",
    #
    #     "cookie": COOKIE
    # }

    body = {
        "ext_flags": [],
        "image_formats": ["jpg", "webp", "avif"],
        "keyword": keyword,
        "note_type": 0,
        "page": page,
        "page_size": page_size,
        "search_id": "2egrms4id30fhlgrojnaj",
        "sort": "time_descending"
    }
    header = client.sign_headers_post(
        uri="https://edith.xiaohongshu.com/api/sns/web/v1/search/notes",
        cookies=cookie,
        payload=body
    )
    headers = {
    "authority": "edith.xiaohongshu.com",
    "method": "POST",
    "path": "/api/sns/web/v1/search/notes",
    "scheme": "https",
    "accept": "application/json, text/plain, */*",
    "accept-encoding": "gzip, deflate, br, zstd",
    "accept-language": "zh-CN,zh;q=0.9",
    "content-type": "application/json;charset=UTF-8",
    "origin": "https://www.xiaohongshu.com",
    "referer": "https://www.xiaohongshu.com/",
    "sec-ch-ua": '"Not:A-Brand";v="99", "Google Chrome";v="145", "Chromium";v="145"',
    "sec-ch-ua-mobile": "?1",
    "sec-ch-ua-platform": '"Android"',
    "sec-fetch-dest": "empty",
    "sec-fetch-mode": "cors",
    "sec-fetch-site": "same-site",
    "user-agent": "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Mobile Safari/537.36",
    "x-b3-traceid": "2cd5f493f66ffd65",
    "x-s": header["x-s"],
    "x-s-common": "2UQAPsHCPjIjqArjwjHjNsQhPsHCH0rjNsQhPaHCH0c1PUhlHjIj2eHjwjQmJfzUJ9SDHjIj2ePjwjQhyoPTqBPT49pjHjIj2ecjwjHMN0rAN0rjNsQh+aHCH0rEG9HhweYY8erh8o864BQd+/SCG7S3P/Sd4941wBEl2g8I+0Dhq9Rl+/ZIPeZAw/D9PAGjNsQh+jHCHjHVHdW7H0ijHjIj2eWjwjQQPAYUaBzdq9k6qB4Q4fpA8b878FSet9RQzLlTcSiM8/+n4MYP8F8LagY/P9Ql4FpUzfpS2BcI8nT1GFbC/L88JdbFyrSiafp/cDMra7pFLDDAa7+8J7QgabmFz7Qjp0mcwp4fanD68p40+fp8qgzELLbILrDA+9p3JpH9LLI3+LSk+d+DJfpSL98lnLYl49IUqgcMc0mrcDShtUTozBD6qM8FyFSh8o+h4g4U+obFyLSi4nbQz/+SPFlnPrDApSzQcA4SPopFJeQmzBMA/o8Szb+NqM+c4ApQzg8Ayp8FaDRl4AYs4g4fLomD8pzBp74Q2ezLanSM+Skc49Qc4gcMag8VGLlj87PAqgzhagYSqAbn4FYQy7pTanTQ2npx87+8NM4L89L78p+l4BL6ze4AzB+IygmS8Bp8qDzFaLP98Lzn4AQQzLEAL7bFJBEVL7pwyS8Fag868nTl4e+0n04ApfuF8FSbL7SQyrLULnzHpLShJpmO2fM6anS0nBpc4F8Q4fSePDH7qFzC+7+hpdzDagG98nc7+9p8ySQoanSD8/8gGDcUpd4xanYtqA+68gP9zo8SpbmF/f+p+fpr4gqMag8889P64fpDpd4aqrQdqM+n49pQP9l1agYTJo+l4o+YLo4Eq7+HGSkm4fLAqsRSzbm72rSe8g+3zemSL9pHyLSk+7+xGfRAP94UzDSk8BL94gqAanSUy7zM4BMF4gzBagYS8pzc4r8QyrkSyp8FJrS389LILoz/t7b7Lokc4MpQ4fY3agY0q0zdarr3aLESypmFyDSiqdzQyBRAydbFLrlILnb7qDTA8B808rSi2juU4g4yqdp7LFSe8o+3Loz/tFMN8/b0cg+nqBMsaLptqM8f/9LAqgzI/op7qLS94fpfpd4kanV98nT6/BpQ4DTSnLQm8p4n4b+QPFRA2opF8DkDngbQznD3anV68nSCPo+LN9zA80ZI8p+c49MQyrp+anYkyomn49pQPMmpanV98/bgJpQNwn4SySm7LrDAnpZ6qgcMaDS68pSCJ7+hpd462S87NFSeqrSQypQU/7b7z7zn4BEQcApS8BH7q9Sc4UV6Loz9agGFzDQc4omQPFzSLBlHaLS3Po+/+FTApBh78nzl4Fu3qgcUanTU2rShP7+hqgzganY98nTI/LSQ4fQwao+LPLSeLDbQ40pSzb87LBRn4FYsqgzDaL+c8rSb/d+LcaRSygp7cFTIn0bQ4DGFPdbFt7Qc4rRQPF4Ua/P3zFSknnTQyo8Syp+NqM8PLApc4gq6aL+rPLkn49+6pd4raLLM8nTfanYQygbfz7Qd8/bM4eQOqgzGLnkiPozIqn+QPM+AaMm7GLSkN7P9pMb1anSV/fMl4AmQyMH3Lpm7apkyO/FjNsQhwaHCP0rAw/rMweZF+sIj2erIH0iINsQhP/rjwjQ1J7QTGnIjNsQhP/HjwjHl+AqU+ArM+AcEPArMwAr7+AHM+0rE+0ZAP/PjKc==",
    "x-t":  header["x-t"],
        "cookie": cookie
    }

    # response = post_proxy("XHS",url, headers=headers, body=body, verify=False, judge_fn=judge_fn)
    response = requests.post(url, headers=headers, json=body, timeout=15)

    try:
        json_data = response.json()
    except Exception as e:
        logger.error(f"XHS 响应不是合法 JSON: {e}")
        logger.error(f"XHS status_code={response.status_code}")
        logger.debug(f"XHS body(head)={response.text[:800]}")
        return []

    if json_data.get("code", -1) != 0:
        global _LOGIN_EXPIRED_WARNED
        msg = json_data.get("msg")
        if msg == "登录已过期":
            if not _LOGIN_EXPIRED_WARNED:
                _LOGIN_EXPIRED_WARNED = True
                logger.warning("XHS 登录已过期：请更新环境变量 XHS_COOKIE 或 crawler/config/xhs_cookie.txt")
        else:
            logger.warning(f"XHS 返回 code != 0，json_data={json_data}")
        return []

    data_items = json_data.get("data", {}).get("items")
    if not isinstance(data_items, list):
        logger.warning(f"XHS 返回缺少 data.items（可能 cookie/风控/结构变更），json_data={json_data}")
        return []

    for item in data_items:
        if item['model_type'] == 'rec_query':
            logger.debug('XHS item model_type=rec_query, skip')
            continue
        if item['model_type'] == 'note' or item['model_type'] == 'normal':
            try:
                card = item['note_card']
                if 'display_title' in card:
                    logger.debug(f"XHS 标题: {card['display_title']}")
                logger.debug(f"XHS id: {item.get('id')}")
                logger.debug(f"XHS author: {(card.get('user') or {}).get('nickname', '')}")
                interact = card.get('interact_info') or {}
                logger.debug(
                    "XHS 点赞:%s 收藏:%s 评论:%s 分享:%s",
                    interact.get('liked_count'),
                    interact.get('collected_count'),
                    interact.get('comment_count'),
                    interact.get('shared_count'),
                )
                cover = card.get('cover') or {}
                if isinstance(cover, dict) and cover.get('url_default'):
                    logger.debug(f"XHS cover: {cover.get('url_default')}")
                if item.get('id'):
                    logger.debug(f"XHS time: {id_to_time(item.get('id'))}")
                corner = card.get('corner_tag_info')
                if isinstance(corner, list) and corner and isinstance(corner[0], dict) and corner[0].get('text'):
                    logger.debug(f"XHS tag: {corner[0].get('text')}")
            except Exception as e:
                logger.debug(f"XHS 解析错误: {e}, item={item}")
        else:
            logger.debug(f"XHS item 非note类型: model_type={item.get('model_type')}, item={item}")

    return data_items


# TODO 查看笔记详情, X-s与X-t为必传参数, 目前未逆向, 会被xhs拦截, 待后期改造
def search_by_id(id):
    url = "https://edith.xiaohongshu.com/api/sns/web/v1/feed"
    cookie = _load_xhs_cookie()
    headers = {
        "accept": "application/json, text/plain, */*",
        "accept-encoding": "gzip, deflate, br, zstd",
        "accept-language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
        "content-length": "176",
        "content-type": "application/json;charset=UTF-8",
        "dnt": "1",
        "origin": "https://www.xiaohongshu.com",
        "priority": "u=1, i",
        "referer": "https://www.xiaohongshu.com/",
        "sec-ch-ua": "\"Not(A:Brand\";v=\"99\", \"Microsoft Edge\";v=\"133\", \"Chromium\";v=\"133\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-site",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1",
        "x-b3-traceid": "a3182781eee4b68c",
        "x-mns": "unload",
        "x-s": "XYS_2UQhPsHCH0c1PUhlHjIj2erjwjQhyoPTqBPT49pjHjIj2eHjwjQgynEDJ74AHjIj2ePjwjQTJdPIP/ZlgMqELnLF/rkiabch+DlNyLkH2pbS/epHyB+kqB4SqepHP9Sd4M+M4SHAarTC+9PMGdzbPM4Bno8Iwo4QwocE4UR1874/JgS74rMP+D4FzFpEGA+gJLELaFSiJB4jLDTVzezHpApFGL8EyFzepo8d8nHMGpzDc7L6+g8PPFztpbStwpc6a/zNPSYGz04r8dSYc9lYwn+t2dWAJDTHJFQY+Bl/tA4PNMSL+Bl1JnL3LASDadGFJBpBzep7aDYharGUPoSrynYVHjIj2ecjwjQ6GfkSG7cjKc=",
        # "x-s-common": "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",
        "x-t": str(int(time.time() * 1000)),
        "x-xray-traceid": "ce5eb9acc2f6c315decb21bceb2ae171",

        "cookie": cookie
    }

    body = {
        "source_note_id": id,
        "image_formats": ["jpg", "webp", "avif"],
        "xsec_source": "pc_search",
        "xsec_token": "",
        "extra": {
            "need_body_topic": 1,
        },
    }

    response = requests.post(url, headers=headers, json=body)
    print(response)
    print(response.status_code)
    print(response.text)


if __name__ == "__main__":
    search("端午", 1, 20)
