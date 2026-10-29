import data_cleaner
import heat_calculator
import pandas as pd
from datetime import datetime, timedelta
from cctv_crawler.cctv_crawler import cctv_infos
from sina_crawler.sina_crawler import sina_infos
from xhs_crawler.xhs_crawler import xhs_infos
from weibo_crawler.weibo_crawler import weibo_infos
from toutiao_crawler.toutiao_crawler import toutiao_infos
from douyin_crawler.douyin_crawler import douyin_infos
from tengxun_crawler.tengxun_crawler import tengxun_infos
from south_weekend_crawler.south_weekend_crawler import south_weekend_infos

def produce_data(start_time=None, end_time=None, debug=False, output_as_file=False):
    """执行所有爬虫"""
    if debug:
        print('\n'+ datetime.strftime(datetime.today(), "%Y-%m-%d %H:%M:%S") + '   start crawling')
        print('===' * 50)

    # 执行新华网爬虫
    if debug:
        print('running cctv_crawler')
    cctv = cctv_infos(12, debug, output_as_file)
    cctv_data = cctv.crawl_data()
    # cctv_data = pd.read_csv("insightmon_crawler\cctv_crawler\crawler_result.csv")

    # 执行新浪网爬虫
    if debug:
        print('running sina_crawler')
    sina = sina_infos(12, debug, output_as_file)
    sina_data = sina.crawl_data() # 很久
    # sina_data = pd.read_csv("insightmon_crawler/sina_crawler/crawler_result.csv")

    # 执行腾讯网爬虫
    if debug:
        print('running tengxun_crawler')
    tengxun = tengxun_infos(12, debug, output_as_file)
    tengxun_data = tengxun.crawl_data()
    # tengxun_data = pd.read_csv("insightmon_crawler/tengxun_crawler/crawler_result.csv")

    # 执行南方周末爬虫
    if debug:
        print('running south_weekend_crawler')
    south_weekend = south_weekend_infos(12, debug, output_as_file)
    south_weekend_data = south_weekend.crawl_data()
    # south_weekend_data = pd.read_csv("insightmon_crawler/south_weekend_crawler/crawler_result.csv")

    # 执行小红书爬虫
    if debug:
        print('running xiaohongshu_crawler')
    xhs = xhs_infos(12, debug, output_as_file)
    try:
        xhs_data = xhs.crawl_data()
        if debug: print("小红书爬取成功")
    except Exception as e:
        if debug: print(f"小红书爬取失败: {e}")
    # xhs_data = pd.read_csv("insightmon_crawler/xhs_crawler/crawler_result.csv")

    # 执行微博爬虫
    if debug:
        print('running weibo_crawler')
    weibo = weibo_infos(12, debug, output_as_file)
    try:
        weibo_data = weibo.crawl_data()
        if debug: print("微博爬取成功")
    except Exception as e:
        if debug: print(f"微博爬取失败: {e}")
    # weibo_data = pd.read_csv("insightmon_crawler/weibo_crawler/crawler_result.csv")

    # 执行头条爬虫
    if debug:
        print('running toutiao_crawler')
    toutiao = toutiao_infos(12, debug, output_as_file)
    try:
        toutiao_data = toutiao.crawl_data()
        if debug: print("头条爬取成功")
    except Exception as e:
        if debug: print(f"头条爬取失败: {e}")
    # toutiao_data = pd.read_csv("insightmon_crawler/toutiao_crawler/crawler_result.csv")

    # 执行抖音爬虫
    if debug:
        print('running douyin_crawler')
    douyin = douyin_infos(12, debug, output_as_file)
    try:
        douyin_data = douyin.crawl_data()
        if debug: print("抖音爬取成功")
    except Exception as e:
        if debug: print(f"抖音爬取失败: {e}")
    # douyin_data = pd.DataFrame()

    # cctv_data = pd.read_csv("insightmon_crawler\cctv_crawler\crawler_result.csv")
    # sina_data = pd.read_csv("insightmon_crawler/sina_crawler/crawler_result.csv")
    # tengxun_data = pd.read_csv("insightmon_crawler/tengxun_crawler/crawler_result.csv")
    # south_weekend_data = pd.read_csv("insightmon_crawler/south_weekend_crawler/crawler_result.csv")
    # xhs_data = pd.read_csv("insightmon_crawler/xhs_crawler/crawler_result.csv")
    # weibo_data = pd.read_csv("insightmon_crawler/weibo_crawler/crawler_result.csv")
    # toutiao_data = pd.read_csv("insightmon_crawler/toutiao_crawler/crawler_result.csv")
    # douyin_data = pd.read_csv("insightmon_crawler/toutiao_crawler/crawler_result.csv")

    # 清理整合数据
    return handle_data(cctv_data, sina_data, tengxun_data, south_weekend_data, xhs_data, weibo_data, toutiao_data, douyin_data, start_time, end_time, debug, output_as_file)


def handle_data(cctv_data, sina_data, tengxun_data, south_weekend_data, xhs_data, weibo_data, toutiao_data, douyin_data, start_time, end_time, debug=False, output_as_file=False):
    """处理数据"""
    if debug:
        print(f"cctv_data type: {type(cctv_data)}")
        print(f"sina_data type: {type(sina_data)}")
        print(f"tengxun_data type: {type(tengxun_data)}")
        print(f"south_weekend_data type: {type(south_weekend_data)}")
        print(f"xhs_data type: {type(xhs_data)}")
        print(f"weibo_data type: {type(weibo_data)}")
        print(f"toutiao_data type: {type(toutiao_data)}")
        print(f"douyin_data type: {type(douyin_data)}")
        # 输出每个数据源的数量
    if debug:
        print(f'cctv_data: {len(cctv_data)}')
        print(f'sina_data: {len(sina_data)}')
        print(f'tengxun_data: {len(tengxun_data)}')
        print(f'south_weekend_data: {len(south_weekend_data)}')
        print(f'xhs_data: {len(xhs_data)}')
        print(f'weibo_data: {len(weibo_data)}')
        print(f'toutiao_data: {len(toutiao_data)}')
        print(f'douyin_data: {len(douyin_data)}')
    data = pd.concat(objs=[cctv_data, sina_data, tengxun_data, south_weekend_data, xhs_data, weibo_data, toutiao_data, douyin_data], axis=0, join='outer', ignore_index=True)
    clean_data = data_cleaner.data_cleaning(data, start_time, end_time, debug, output_as_file) # 数据清洗
    return heat_calculator.cal_heat(clean_data, debug, output_as_file)


# TODO: this is just a demo
if __name__ == "__main__":
    # 获取昨天的日期
    yesterday = datetime.now() - timedelta(days=2)
    start_time = pd.Timestamp(yesterday.strftime('%Y-%m-%d 00:00:00'))
    end_time = pd.Timestamp(yesterday.strftime('%Y-%m-%d 23:59:59'))
    produce_data(start_time, end_time, True, True)
