import requests
from urllib.parse import quote, unquote
from lxml import html
import time
import json
from common.proxy import get_proxy

def judge_fn(response):
    print("Response status code:", response.status_code)
    print("Response text:", response.text[:5000])
    return len(response.text) > 5000



class ToutiaoAdaptor:
    def get_search_results(self, keyword: str = "特朗普", page_num: int = 1):
        url = "https://so.toutiao.com/search"

        cookies = {
        "tt_webid": "7463840914033772073",
        "_ga": "GA1.1.805733647.1737810923",
        "_tea_utm_cache_4916": "undefined",
        "_S_DPR": "1.25",
        "_S_IPAD": "0",
        "s_v_web_id": "verify_m6px7b1o_AnD0SIp7_jKed_489U_BvmO_UrU9buVKvl8N",
        "notRedShot": "1",
        "__ac_nonce": "067a1f69100222cf948c1",
        "__ac_signature": "_02B4Z6wo00f0113FVuAAAIDCgp6lrA0d8ytd5VJAALDbde",
        # "__ac_referer": "https://so.toutiao.com/search?dvpf=pc&source=input&keyword=%E7%89%B9%E6%9C%97%E6%99%AE&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num=0&min_time={min_time}&max_time={max_time}",
        "__ac_referer": f"https://so.toutiao.com/search?dvpf=pc&source=input&keyword={quote(keyword)}&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num={page_num}",
        "ttwid": "1%7CcwW8kzZ307M3Eljar0nndoeqBhQT-SE3A-appJN-MqI%7C1738661363%7Cc1526e75a23a6120e4e4724e4a921843efbb6c89f1325825ad72a468856357dc",
        "_ga_QEHZPBE5HH": "GS1.1.1738660939.3.1.1738661392.0.0.0",
        "_S_WIN_WH": "298_738"
        }

        headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            # "Accept-Encoding": "gzip, deflate, br, zstd",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
            "Cache-Control": "max-age=0",
            "Connection": "keep-alive",
            "Dnt": "1",
            "Host": "so.toutiao.com",
            # "Referer": "https://so.toutiao.com/search?dvpf=pc&source=input&keyword=11111111&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num=0&min_time={min_time}&max_time={max_time}",
            "Referer": f"https://so.toutiao.com/search?dvpf=pc&source=input&keyword={quote(keyword)}&pd=synthesis&action_type=pagination&from=search_tab&cur_tab_title=search_tab&page_num={page_num}",
            "Sec-Ch-Ua": "\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"",
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": "\"Windows\"",
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "same-origin",
            "Sec-Fetch-User": "?1",
            "Upgrade-Insecure-Requests": "1",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.1 Safari/537.36 Edg/127.0.0.1",
        }

        params = {
            "dvpf": "pc",
            "source": "input",
            # "keyword": "特朗普",
            "keyword": keyword,
            "pd": "synthesis",
            "action_type": "pagination",
            "from": "search_tab",
            "cur_tab_title": "search_tab",
            # "page_num": 0,
            "page_num": page_num,
            # "filter_vendor": "all",
            # "index_resource": "all",
            # "filter_period": "all",
            # "min_time": min_time,
            # "max_time": max_time,
            # "search_id": "20250204173626A258818342E34C1771D8"
        }
        try:
            # 发送请求
            # response = get_proxy(source="toutiao", url=url, params=params,headers=headers, cookies=cookies, verify=False, judge_fn=judge_fn)
            response = requests.get(url, params=params, headers=headers, cookies=cookies, verify=False)
            # 检查响应状态码
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            print(f"请求出错: {e}")
            return None

    def parse_search_results(self, html_content):
        data_arr = []  # 用于存储所有文章卡片的数据
        if html_content:
            try:
                tree = html.fromstring(html_content)
                # 定位所有文章卡片
                article_cards = tree.xpath('//div[@class="s-result-list"]//div[@class="cs-view cs-view-block cs-card"]')
                for card in article_cards:
                    # 提取标题
                    title_a = card.xpath('.//a[@class="text-ellipsis text-underline-hover"][1]')
                    if title_a:
                        title = ''.join(title_a[0].itertext()).strip()
                    else:
                        title = ""
                    # 提取链接
                    if title_a:
                        href = title_a[0].get('href')
                        if href:
                            start_index = href.find('url=')
                            if start_index != -1:
                                url_encoded = href[start_index + 4:]
                                # 对 URL 进行解码
                                url = unquote(url_encoded)
                            else:
                                url = ""
                        else:
                            url = ""
                    else:
                        url = ""

                    # 提取内容
                    content_span = card.xpath('.//span[@class="text-underline-hover"]')
                    if content_span:
                        content_text = ''.join(content_span[0].itertext())
                        content = content_text.strip() if content_text else ""
                    else:
                        content = ""

                    # 提取作者
                    author_span = card.xpath(
                        './/div[@class="cs-view cs-view-flex align-items-center flex-row cs-source-content"]/span[1]/span[@class="text-ellipsis"]')
                    if author_span:
                        author_text = author_span[0].text
                        author = author_text.strip() if author_text else ""
                    else:
                        author = ""

                    # 提取评论数
                    comment_span = card.xpath(
                        './/div[@class="cs-view margin-top-3 cs-view-block cs-source"]//div[@class="cs-view cs-view-flex align-items-center flex-row cs-source-content"]/span[@class="text-ellipsis margin-right-4"][contains(text(), "评论")]')
                    if comment_span:
                        comment_text = comment_span[0].text
                        comment_count_str = ''.join(filter(str.isdigit, comment_text)) if comment_text else ""
                        comment_count = int(comment_count_str)
                    else:
                        comment_count = 0

                    # 提取发布时间
                    publish_time_span = card.xpath(
                        './/div[@class="cs-view margin-top-3 cs-view-block cs-source"]//div[@class="cs-view cs-view-flex align-items-center flex-row cs-source-content"]/span[@class="text-ellipsis"][last()]')
                    if publish_time_span:
                        publish_time_text = publish_time_span[0].text
                        publish_time = publish_time_text.strip() if publish_time_text else ""
                    else:
                        publish_time = ""

                    item = {
                        'source': '今日头条',
                        'title': title,
                        'author': author,
                        'content': content,
                        'comment_count': comment_count,
                        'publish_time': publish_time,
                        'url': url
                    }
                    data_arr.append(item)  # 将当前文章卡片的数据添加到列表中
                return data_arr  # 循环结束后返回包含所有文章卡片数据的列表
            except Exception as e:
                print(f"解析 HTML 出错: {e}")


if __name__ == "__main__":
    try:
        toutiao = ToutiaoAdaptor()
        html_content = toutiao.get_search_results()
        if html_content:
            data = toutiao.parse_search_results(html_content)
            for item in data:
                print(item)
    except Exception as e:
        print(f"程序出现异常: {e}")
