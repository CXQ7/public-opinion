import data_cleaner
import heat_calculator
import pandas as pd
from datetime import datetime
from cctv_crawler.cctv_crawler import cctv_infos
from sina_crawler.sina_crawler import sina_infos
from tengxun_crawler.tengxun_crawler import tengxun_infos
from south_weekend_crawler.south_weekend_crawler import south_weekend_infos

def produce_data(debug=False, output_as_file=False):
    """执行所有爬虫"""
    if debug:
        print('\n'+ datetime.strftime(datetime.today(), "%Y-%m-%d %H:%M:%S") + '   start crawling')
        print('===' * 50)

    # 执行新华网爬虫
    if debug:
        print('running cctv_crawler')
    cctv = cctv_infos(10, debug, output_as_file)
    cctv_data = cctv.crawl_data()

    # 执行新浪网爬虫
    if debug:
        print('running sina_crawler')
    sina = sina_infos(1, debug, output_as_file)
    sina_data = sina.crawl_data()

    # 执行腾讯网爬虫
    if debug:
        print('running tengxun_crawler')
    tengxun = tengxun_infos(10, debug, output_as_file)
    tengxun_data = tengxun.crawl_data()
    # 执行南方周末爬虫

    if debug:
        print('running south_weekend_crawler')
    south_weekend = south_weekend_infos(10, debug, output_as_file)
    south_weekend_data = south_weekend.crawl_data()

    # 清理整合数据
    return handle_data(cctv_data, sina_data, tengxun_data, south_weekend_data, debug, output_as_file)


def handle_data(cctv_data, sina_data, tengxun_data, south_weekend_data, debug=False, output_as_file=False):
    """处理数据"""
    data = pd.concat(objs=[cctv_data, sina_data, tengxun_data, south_weekend_data], axis=0, join='outer', ignore_index=True)
    clean_data = data_cleaner.data_cleaning(data, debug, False) # 数据清洗
    return heat_calculator.cal_heat(clean_data, debug, output_as_file)


# TODO: this is just a demo
if __name__ == "__main__":
    produce_data(True, True)
