import sys
sys.path.append('insightmon_model')
sys.path.append('insightmon_crawler')

import pandas as pd
from datetime import datetime, timedelta
import insightmon_crawler.data_producer as crawler_app
import insightmon_model.app as model_app


if __name__ == "__main__":
    # 获取12小时前的时间
    start_time = pd.Timestamp((datetime.now() - timedelta(hours=12)).strftime('%Y-%m-%d %H:%M:%S'))
    end_time = pd.Timestamp(datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    # data_df = crawler_app.produce_data(start_time, end_time, True, True)

    current_date = datetime.now().strftime('%Y%m%d-%H%M%S')
    # data_df.to_csv(f'./debug/train_data_{current_date}.csv', index=False)

    model_app.train_and_clean_data(
        # data_df,
        # pd.read_csv("./insightmon_model/train_data/raw_data.csv"),
        pd.read_csv("./debug/train_data(1).csv"),
        "./insightmon_model")
