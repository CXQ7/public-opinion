'''
使用hanlp分词训练
输入：blog_records_with_urls.csv
输出：train_result.json
'''

import json

import hanlp
from snownlp import SnowNLP
from utils import *

hanlp_loaded = False
hanlp_instance = None

class InfoItem:
    def __init__(self, arg0, pred, arg1, heat, event_datetime, sentiment, index):
        self.arg0 = arg0
        self.pred = pred
        self.arg1 = arg1
        self.heat = heat
        self.event_datetime = event_datetime
        self.sentiment = sentiment
        # self.time = 0
        self.cnt = 1
        self.ids = {index}

    def to_json(self):
        return {
            # 'pred': self.pred,
            # 'arg1': self.arg1,
            'heat': str(self.heat),
            'event_datetime': str(self.event_datetime),
            'sentiment': str(self.sentiment),
            'cnt': str(self.cnt),
            'ids': self.ids
        }

    def combine(self, other):
        for o_id in other.ids:
            if o_id in self.ids:
                return
        self.heat += other.heat
        self.cnt += 1
        self.event_datetime = cal_average_datetime(self.event_datetime, other.event_datetime)
        self.sentiment += other.sentiment
        self.ids.update(other.ids)

    def process(self):  # 收集完数据后再统一处理，比如做时间戳的取平均
        # self.event_datetime = multi_datetime_with_number(self.event_datetime, 1 / self.cnt)
        self.ids = list(self.ids)
        self.sentiment = self.sentiment / self.cnt

    def __eq__(self, other):
        if not isinstance(other, InfoItem):
            return False
        return self.arg0 == other.arg0 and self.pred == other.pred and self.arg1 == other.arg1

    def __hash__(self):
        return hash((self.arg0, self.pred, self.arg1))

    def __str__(self):
        return (f"({self.pred}, {self.arg1}) | heat: {self.heat}"
                f" | ev_time: {self.event_datetime} | sentiment: {self.sentiment}"
                f" | cnt: {self.cnt} | ids: {self.ids}")

def load_hanlp(model_dir=HANLP_DIR):
    global hanlp_loaded, hanlp_instance
    if not hanlp_loaded:
        logger.info("Loading hanlp...")
        # 加载模型
        hanlp_loaded = True
        try:
            hanlp_instance = hanlp.load(model_dir)
            logger.info("Loading hanlp successfully")
        except Exception as e:
            logger.error("加载模型失败")
            logger.error(e)



def get_freq_df(doc):
    freq_dic = {}
    for sentence_index, srl in enumerate(doc['srl']):
        for predicate_argument_structure in srl:
            for arg in predicate_argument_structure:
                if freq_dic.get(arg[1]) is None:
                    freq_dic[arg[1]] = []
                freq_dic[arg[1]].append(arg[0])
    freq_df = pd.DataFrame([freq_dic])
    return freq_df


def get_relation_dict(doc):
    relation_dict = {}
    for sentence_index, srl in enumerate(doc['srl']):
        cur_arg0, cur_pred = "我", ""
        for predicate_argument_structure in srl:
            for arg in predicate_argument_structure:
                if arg[1] == "ARG0":  # 主语
                    cur_arg0 = arg[0]
                elif arg[1] == "PRED":  # 动词
                    cur_pred = arg[0]
                elif arg[1] == "ARG1":  # 宾语
                    if relation_dict.get(cur_arg0) is None:
                        relation_dict[cur_arg0] = []
                    relation_dict[cur_arg0].append((cur_pred, arg[0]))
                    cur_arg0, cur_pred = "我", ""
    # freq_df = pd.DataFrame([freq_dic])
    return relation_dict


def sort_relation_dict(relation_dict_with_counter, heat_dict):
    # 对字典按照热度排序
    sorted_keys = sorted(relation_dict_with_counter.keys(), key=lambda key: heat_dict[key], reverse=True)
    relation_dict_with_counter = dict(zip(sorted_keys, [relation_dict_with_counter[key] for key in sorted_keys]))
    for arg0, value_dict in relation_dict_with_counter.items():
        # 内层按事件时间排序
        sorted_items = sorted(value_dict.items(), key=lambda item: item[1].event_datetime)
        for key, item in sorted_items:
            item.process()
        relation_dict_with_counter[arg0] = dict(sorted_items)
    return relation_dict_with_counter


def to_json_relation_dict(relation_dict_with_counter, heat_dict):
    for arg0, value_dict in relation_dict_with_counter.items():
        inner_json_dict = {}
        for item in value_dict.items():
            item[1].event_datetime = item[1].event_datetime.strftime("%Y-%m-%d %H:%M:%S")
            inner_json_dict[f'{item[0][0]},{item[0][1]}'] = item[1].to_json()
        relation_dict_with_counter[arg0] = {
            "heat": int(heat_dict[arg0]),
            "records": inner_json_dict
        }
    return relation_dict_with_counter


def get_relation_dict_with_counter(doc, heat_list, datetime_list, sentiment_list, output_as_file=False):
    relation_dict_with_counter, heat_dict = {}, {}
    for i, srl in enumerate(doc['srl']):
        cur_arg0, cur_pred = " ", ""
        for predicate_argument_structure in srl:
            for arg in predicate_argument_structure:
                if arg[1] == "ARG0":  # 主语
                    cur_arg0 = arg[0]
                elif arg[1] == "PRED":  # 动词
                    cur_pred = arg[0]
                elif arg[1] == "ARG1":  # 宾语
                    if relation_dict_with_counter.get(cur_arg0) is None:
                        relation_dict_with_counter[cur_arg0] = {}
                        heat_dict[cur_arg0] = 0

                    key_tuple = (cur_pred, arg[0])
                    # print(f"heat_list[{i}]", heat_list[i])
                    info_item = InfoItem(cur_arg0, cur_pred, arg[0], heat_list[i], datetime_list[i],
                                         sentiment_list[i], i)
                    heat_dict[cur_arg0] += heat_list[i]

                    if relation_dict_with_counter[cur_arg0].get(key_tuple) is None:
                        relation_dict_with_counter[cur_arg0][key_tuple] = info_item
                    else:
                        relation_dict_with_counter[cur_arg0][key_tuple].combine(info_item)
                    cur_arg0, cur_pred = " ", ""

    relation_dict_with_counter = sort_relation_dict(relation_dict_with_counter, heat_dict)
    relation_dict_with_counter = to_json_relation_dict(relation_dict_with_counter, heat_dict)
    if output_as_file:
        save_json_dict(f'{debug_dir}/train_result.json', relation_dict_with_counter)

    for arg0, value_dict in relation_dict_with_counter.items():
        logger.debug("heat: %s {", heat_dict[arg0])
        for key_tuple, info_item in value_dict.items():
            # info_item.process()
            logger.debug(f"  {info_item}")
        logger.debug("}")

    return relation_dict_with_counter


def train(data_df, model_dir=HANLP_DIR, output_as_file=False):
    logger.debug(data_df)
    logger.info("Analyzing data... (玩命训练中)")
    load_hanlp(model_dir)

    # CLOSE_TOK_POS_NER_SRL_DEP_SDP_CON_ELECTRA_BASE_ZH
    datetime_list, text = (
        pd.to_datetime(data_df['publish_time']),
        data_df['title'].dropna().tolist())

    # 计算情感值，获取sentiment_list
    sentiment_list = []
    for i in range(len(text)):
        s = SnowNLP(text[i])
        sentiment_list.append(s.sentiments)

    print("text:", text)
    doc = hanlp_instance(text)
    logger.info("hanlp doc训练完毕")
        # doc.pretty_print()

    relation_dict_with_counter = get_relation_dict_with_counter(doc, data_df['heat'],
                                                                datetime_list, sentiment_list,
                                                                output_as_file)
    logger.info("模型训练完毕:")
    logger.debug(relation_dict_with_counter)
    return relation_dict_with_counter
