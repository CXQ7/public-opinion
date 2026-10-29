import sys

sys.path.append('repository')
sys.path.append('model')
sys.path.append('common')

import pandas as pd
import json
import logging
import colorlog

MODE_DEBUG = True  # DEBUG, FATAL
RAW_DATA_PATH = "train_data/raw_data.csv"
HANLP_DIR = "hanlp\\mtl\\close_tok_pos_ner_srl_dep_sdp_con_electra_base_20210111_124519"
CONFIG_PATH = "config.json"
debug_dir = "debug"

# 创建一个彩色日志处理器
handler = colorlog.StreamHandler()
handler.setLevel(logging.DEBUG)

# 定义日志格式，包括颜色设置
formatter = colorlog.ColoredFormatter(
    '%(log_color)s[%(asctime)s] - %(levelname)s - %(filename)s:%(lineno)d - %(message)s',
    log_colors={
        'DEBUG': 'yellow',
        'INFO': 'green',
        'WARNING': 'yellow,bg_white',
        'ERROR': 'red',
        'CRITICAL': 'red,bg_white'
    }
)
handler.setFormatter(formatter)

file_formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(filename)s:%(lineno)d - %(message)s')
file_handler = logging.FileHandler('app.log', 'a', 'utf-8')
file_handler.setFormatter(file_formatter)

# 获取日志记录器并添加处理器
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
logger.addHandler(handler)
logger.addHandler(file_handler)

def try_parse_json(json_str):
    start_index = json_str.find('{')
    end_index = json_str.rfind('}')
    if start_index != -1 and end_index != -1 and end_index > start_index:
        json_str = json_str[start_index:end_index + 1]
        try:
            return json.loads(json_str)
        except json.JSONDecodeError as e:
            logging.error(f"JSON 解析失败: {e}, 待解析的字符串: {json_str}")
    logging.error(f"JSON 解析失败, 找不到匹配的大括号, 待解析的字符串: {json_str}")
    return None

def get_data_df(url="./xm_fish/major_info/blog_records_with_urls.csv"):
    data_df = pd.read_csv(url)
    data_df = convert_to_train_data(data_df)
    return data_df

def convert_to_train_data(data_df):
    data_df = data_df[['话题', '评论数', '点赞数', '转发数', '完整日期', 'weibo_url']].dropna()
    data_df.reset_index(drop=True, inplace=True)  # 重置索引
    return data_df


def save_json_dict(filepath, data_dict):
    with open(filepath, 'w', encoding='utf-8') as file:
        json.dump(data_dict, file)

def load_json_dict(filepath=f'{debug_dir}/train_result_with_urls.json'):
    with open(filepath, 'r', encoding='utf-8') as file:
        json_dict = json.load(file)
        return json_dict


def sum_datetime(datetime1, datetime2):
    # 将日期转换为时间戳（以纳秒为单位）
    ts1 = datetime1.value
    ts2 = datetime2.value
    sum_ts = ts1 + ts2
    return pd.Timestamp(sum_ts)


def cal_average_datetime(datetime1, datetime2):
    # 将日期转换为时间戳（以纳秒为单位）
    ts1 = datetime1.value
    ts2 = datetime2.value
    average_ts = (ts1 + ts2) / 2
    # 将平均时间戳转换回日期
    return pd.Timestamp(average_ts)


def multi_datetime_with_number(datetime, multiplier):
    # 将日期转换为时间戳（以纳秒为单位）
    ts = datetime.value / multiplier
    # 将平均时间戳转换回日期
    return pd.Timestamp(ts)


def process_unmatched_csv_file(filepath):
    data_df = pd.read_csv(filepath)

    data_df['话题'] = data_df['转发数']
    data_df['转发数'] = data_df['评论数']
    data_df['评论数'] = data_df['点赞数']
    data_df['点赞数'] = data_df['工具']
    data_df['工具'] = data_df['日期']
    data_df['日期'] = data_df['位置']
    data_df['视频url'] = data_df['原始图片url']
    data_df['原始图片url'] = data_df['头条文章url']
    data_df = data_df.drop('@用户', axis=1)
    data_df = data_df.drop('头条文章url', axis=1)
    data_df = data_df.drop('位置', axis=1)

    data_df.to_csv("./xm_fish/major_info/blog_records_with_urls3.csv", index=False)
