import os
import re
import pandas as pd
from datetime import datetime, timedelta

def remove_special_sign(tar_data, debug):
    """去除特殊符号"""
    special_sign = " ！@#￥%……&*（）【】|、：；“”‘’《》，。…？~·.!.\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~\n"
    for the_sign in special_sign:
            tar_data = tar_data.str.replace(the_sign,'')
    if debug:
        print("---" * 50)
        print("removed special signs in contents")
    return tar_data


def data_cleaning(data, start_time, end_time, debug=False, output_as_file=False):
    """数据清洗"""
    # 去除特殊符号，填充空值，去重
    if debug:
        print("原始数据概况 (before cleaning):")
        try:
            data.info()
        except Exception as e:
            print(f"data.info() 失败: {e}")

    # 兼容：如果某些必需列缺失，先补列再处理，避免 KeyError
    required_cols = ['title','content','author','source','like_count','share_count','comment_count','read_count','publish_time']
    for col in required_cols:
        if col not in data.columns:
            # 对于计数字段用 0，其余用空字符串
            if col in ['like_count','share_count','comment_count','read_count']:
                data[col] = 0
            else:
                data[col] = ''

    data['content'] = data['content'].fillna(value=data['title'])
    data['content'] = remove_special_sign(data['content'], debug) # 去除特殊符号
    data['author'] = data['author'].fillna(value=data['source'])
    for col in ['like_count','share_count','comment_count','read_count']:
        data[col] = data[col].fillna(value=0)

    # ---------------- 新增：数值列规范化 ----------------
    def _parse_num(val):
        """将包含中文单位/逗号/空白的计数字段转换为 int.
        支持示例: '1,234', '2.5万', '3.1 亿', '—', '-', '', '12', 15
        未能解析时返回 0。"""
        if pd.isna(val):
            return 0
        if isinstance(val, (int, float)):
            return int(val)
        if not isinstance(val, str):
            return 0
        s = val.strip()
        if s in ['', '-', '—', '--', 'null', 'None']:
            return 0
        s = s.replace(',', '')  # 去掉千分位
        # 匹配数字 + 可选单位
        m = re.match(r'^([0-9]+\.?[0-9]*)\s*(万|億|亿)?$', s)
        if m:
            num = float(m.group(1))
            unit = m.group(2)
            if unit in ['万']:
                num *= 10000
            elif unit in ['亿','億']:
                num *= 100000000
            return int(num)
        # 尝试直接转
        try:
            return int(float(s))
        except:
            return 0

    for col in ['like_count','share_count','comment_count','read_count']:
        try:
            data[col] = data[col].apply(_parse_num).astype(int)
        except Exception as e:
            if debug:
                print(f"列 {col} 转换失败: {e}")

    if debug:
        print("数值列规范化后 sample:")
        print(data[['like_count','comment_count','share_count','read_count']].head())
    # -------------------------------------------------

    data = data.drop_duplicates(subset=['title'])  # 去重
    # 筛选发布时间在昨天的news
    if start_time is not None and end_time is not None:
        data.loc[:, 'publish_time'] = pd.to_datetime(data['publish_time'])
        data = data[(data['publish_time'] >= start_time) & (data['publish_time'] <= end_time)]

    if debug:
        print("---" * 50)
        print("filled NA/NAN value & cleaned")
        try:
            data.info()
        except Exception as e:
            print(f"data.info() 失败: {e}")
    if output_as_file:
         # 将整合后的数据存入'./data/all_info.csv'
        file_dir = './data'
        file_path = file_dir + os.sep + 'all_info.csv'
        if not os.path.isdir(file_dir):
            os.makedirs(file_dir)
        data.to_csv(file_path, encoding="utf_8_sig", index_label='event_id')
        print("---" * 50)
        print("succeeded in renewing all the info in './data/all_info.csv'")
        print("---" * 50)
    return data

# TODO: this is just a demo
if __name__ == "__main__":
    cctv_data = pd.read_csv("./cctv_crawler/crawler_result.csv", index_col='event_id')
    sina_data = pd.read_csv("./sina_crawler/crawler_result.csv", index_col='event_id')
    tengxun_data = pd.read_csv("./tengxun_crawler/crawler_result.csv", index_col='event_id')
    south_weekend_data = pd.read_csv("./south_weekend_crawler/crawler_result.csv", index_col='event_id')
    data = pd.concat(objs=[cctv_data, sina_data, tengxun_data, south_weekend_data], axis=0, join='outer', ignore_index=True)

    # 获取昨天的日期
    yesterday = datetime.now() - timedelta(days=2)
    start_time = pd.Timestamp(yesterday.strftime('%Y-%m-%d 00:00:00'))
    end_time = pd.Timestamp(yesterday.strftime('%Y-%m-%d 23:59:59'))

    data_cleaning(data, start_time, end_time, True, True)
