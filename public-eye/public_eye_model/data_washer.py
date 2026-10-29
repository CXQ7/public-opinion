'''
使用pandas数据清洗
输入：train_result.json
输出：clean_train_result_with_urls.json
'''

from utils import *

raw_file_url = f'{debug_dir}/train_result.json'
clean_file_url = f'{debug_dir}/clean_train_result.json'


def add_urls(clean_dict=None, raw_data_df=None, output_as_file=False):
    logger.info("开始为keyword添加urls")
    # if clean_dict is None:
    #     with open(clean_file_url, 'r', encoding='utf-8') as file:
    #         clean_dict = json.load(file)

    # data_df = pd.read_csv("./train_data/raw_data.csv")

    for arg0, group in clean_dict.items():
        records = group['records']
        for pred_arg1_tuple, info_item in records.items():
            urls = []
            for index in info_item['ids']:
                urls.append(str(raw_data_df.iloc[index]['url']))
            info_item['urls'] = urls
            records[pred_arg1_tuple] = info_item
        group['records'] = records
        clean_dict[arg0] = group
    logger.debug("clean_data_with_urls:{}".format(clean_dict))
    if output_as_file:
        save_json_dict(f'{debug_dir}/clean_train_result_with_urls.json', clean_dict)
    logger.info("添加url完毕")
    return clean_dict


def wash_data(train_dict=None, raw_data_df=None, output_as_file=False):
    logger.info("开始清洗数据")
    # if train_dict is None:
    #     with open(raw_file_url, 'r', encoding='utf-8') as file:
    #         train_dict = json.load(file)

    forbidden_arg0_set = {"我", "你", "他", "我们", "他们", "自己", "您", "这",
                          "那", " ", "习近平"}
    for forbidden_arg0 in forbidden_arg0_set:
        if forbidden_arg0 in train_dict:
            train_dict.pop(forbidden_arg0)
    if output_as_file:
        with open(clean_file_url, 'w', encoding='utf-8') as file:
            json.dump(train_dict, file)
    logger.debug("clean_data:{}".format(train_dict))
    logger.info("数据清洗完毕")
    return add_urls(train_dict, raw_data_df, output_as_file)
