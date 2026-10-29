import data_washer as dw
import model
import db_operator as dbo
from datetime import datetime
from utils import *


def train_and_clean_data(raw_data_df, path_prefix=''):
    logger.info("\n\n----- start training")

    model_dir = f"{path_prefix}/{HANLP_DIR}"
    config_path = f"{path_prefix}/{CONFIG_PATH}"
    with open(config_path, 'r', encoding='utf-8') as file:
        config = json.load(file)
    news_id_bias = config['news_id_bias']

    # TODO split for test
    # raw_data_df = raw_data_df[news_id_bias: news_id_bias + 100].reset_index(drop=False)
    raw_data_df['heat'] = raw_data_df['heat'].astype('float')

    relation_dict_with_counter = model.train(raw_data_df, model_dir, False)
    new_data_dict = dw.wash_data(relation_dict_with_counter, raw_data_df, False)

    old_data_dict = dbo.query_data_dict_from_db()

    for keyword, detail in new_data_dict.items():
        for desc, info in detail['records'].items():
            for i in range(len(info['ids'])):
                info['ids'][i] += news_id_bias

        # merge old with new
        if keyword in old_data_dict:  # merge keyword
            logger.debug("before combine: old, new->")
            logger.debug(old_data_dict[keyword])
            logger.debug(new_data_dict[keyword])

            old_data_dict[keyword]['heat'] += float(detail['heat'])
            for desc, info in detail['records'].items():
                if desc in old_data_dict[keyword]['records']:  # merge desc
                    old_data_dict[keyword]['records'][desc]['heat'] += float(info['heat'])
                    old_data_dict[keyword]['records'][desc]['update_time'] = cal_average_datetime(
                        pd.to_datetime(datetime.now()), old_data_dict[keyword]['records'][desc]['update_time'])
                    old_data_dict[keyword]['records'][desc]['sentiment'] = (
                        old_data_dict[keyword]['records'][desc]['sentiment'] + float(info['sentiment'])) / 2.0
                    for id in info['ids']:
                        old_data_dict[keyword]['records'][desc]['ids'].append(id)
                else:  # add desc
                    old_data_dict[keyword]['records'][desc] = info

            logger.debug("after combine: old->")
            logger.debug(old_data_dict[keyword])
        else:  # add keyword to dict
            old_data_dict[keyword] = detail

    config['news_id_bias'] = dbo.insert_news_records(raw_data_df, news_id_bias)
    dbo.clean_keyword_with_event()
    dbo.insert_keyword_with_event(old_data_dict)

    with open(config_path, 'w', encoding='utf-8') as file:
        json.dump(config, file)

    # dbo.release_connection()
def release_connection():
    dbo.release_connection()


if __name__ == "__main__":
    train_and_clean_data(pd.read_csv(RAW_DATA_PATH))
