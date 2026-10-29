import sys
sys.path.append('public_eye_model')
sys.path.append('public_eye_crawler')

import pandas as pd
from datetime import datetime, timedelta
import public_eye_crawler.data_producer as crawler_app
import public_eye_model.app as model_app


if __name__ == "__main__":
    # 获取昨天的日期
    yesterday = datetime.now() - timedelta(days=1)
    start_time = pd.Timestamp(yesterday.strftime('%Y-%m-%d 00:00:00'))
    end_time = pd.Timestamp(yesterday.strftime('%Y-%m-%d 23:59:59'))
    data_df = crawler_app.produce_data(start_time, end_time, True, False)

    current_date = datetime.now().strftime('%Y%m%d')
    data_df.to_csv(f'./debug/train_data_{current_date}.csv', index=False)

    model_app.train_and_clean_data(
        data_df,
        # pd.read_csv("./public_eye_model/train_data/raw_data.csv"),
        # pd.read_csv("./debug/train_data.csv"),
        "./public_eye_model")
