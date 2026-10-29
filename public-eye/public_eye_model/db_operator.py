import json

import pymysql
from datetime import datetime
import pandas as pd
from utils import logger

# 数据库连接配置
db_config = {
    # 云服务器配置
    'host': '127.0.0.1',  # 数据库主机地址
    'user': 'dev',  # 数据库用户名
    'password': '__CONFIGURE_LOCALLY__',  # 数据库密码

    # 本机配置
    # 'host': 'localhost',  # 数据库主机地址
    # 'user': 'root',  # 数据库用户名
    # 'password': '__CONFIGURE_LOCALLY__',  # 数据库密码

    'database': 'public_eye',  # 数据库名称
    'charset': 'utf8mb3',
}

insert_keyword_sql = """
     INSERT INTO keyword (keyword, heat, create_time, update_time)
     VALUES (%s, %s, %s, %s)
 """

insert_event_sql = """
    INSERT INTO event (keyword_id, description, heat, news_ids, urls,
    sentiment, event_datetime, create_time, update_time)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
"""

insert_news_sql = """
    INSERT INTO news_record (id, source, author,
    title, content, heat, publish_datetime, url)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
"""


# 连接数据库
connection = pymysql.connect(**db_config, cursorclass=pymysql.cursors.DictCursor)
cursor = connection.cursor()

def clean_keyword_with_event():
    try:
        logger.info("准备删除keyword, event表中数据")
        # 删除 event 表中的所有数据
        sql_event = "DELETE FROM event;"
        cursor.execute(sql_event)
        # 重置 event 表自增列起始值
        sql_reset_event_id = "ALTER TABLE event AUTO_INCREMENT = 1;"
        cursor.execute(sql_reset_event_id)
        # 删除 keyword 表中的所有数据
        sql_keyword = "DELETE FROM keyword;"
        cursor.execute(sql_keyword)
        # 重置 keyword 表自增列起始值
        sql_reset_keyword_id = "ALTER TABLE keyword AUTO_INCREMENT = 1;"
        cursor.execute(sql_reset_keyword_id)
        connection.commit()
        logger.info("keyword, evnet表中数据删除成功")
    except Exception as e:
        connection.rollback()
        logger.error(f"数据删除失败：{e}")

# 解析JSON文件并插入数据
def insert_keyword_with_event(data):
    try:
        # 遍历关键词数据
        for keyword, details in data.items():
            # 插入keyword
            heat = details.get("heat", 0)
            create_time = details.get("create_time", datetime.now())
            update_time = details.get("update_time", datetime.now())
            cursor.execute(insert_keyword_sql, (keyword, heat, create_time, update_time))
            keyword_id = cursor.lastrowid  # 获取插入后的关键词ID
            logger.debug(f"{keyword}, heat: {heat}, keyword_id: {keyword_id}")
            # 插入event
            for event_desc, event_data in details.get('records', {}).items():
                logger.debug(f"{event_desc}: {event_data}")
                heat = event_data.get('heat', 0.0)
                sentiment = float(event_data.get('sentiment', 0))
                event_datetime = event_data.get('event_datetime', '1970-01-01 00:00:00')
                create_time = event_data.get("create_time", datetime.now())
                update_time = event_data.get("update_time", datetime.now())

                # 获取URL字段
                # 为避免ids或urls过长，限制36条
                news_ids = event_data.get('ids', None)[:36]
                news_ids = ' '.join(map(str, news_ids))

                urls = event_data.get('urls', None)[:36]
                urls = ' '.join(urls)

                cursor.execute(insert_event_sql, (
                    keyword_id, event_desc, heat, news_ids, urls,
                    sentiment, event_datetime, create_time, update_time
                ))

        # 提交事务
        connection.commit()
        logger.info("数据插入成功!")

    except Exception as e:
        connection.rollback()  # 如果出错，回滚事务
        logger.error(e)
        logger.error("keyword or event 插入失败")


def insert_news_records(data_df, news_id_bias, batch_size=1000):
    logger.info("开始插入news_records")
    id = news_id_bias
    data_array = []
    try:
        for index, row in data_df.iterrows():
            logger.debug(row)
            title = row['title']
            source = row['source']
            author = row['author']
            content = row['content']
            heat = row['heat']
            publish_datetime = row['publish_time']
            url = row['url']
            # event_date = datetime.strptime(row['event_datetime'], '%Y-%m-%d %H:%M:%S').date()

            # 将数据加入数组
            data_array.append((
                id, source, author, title, content, heat, publish_datetime, url
            ))
            id += 1

            # 当数据达到 batch_size 时执行一次插入
            if len(data_array) == batch_size:
                cursor.executemany(insert_news_sql, data_array)
                connection.commit()  # 提交事务
                data_array = []  # 清空数据数组

        # 插入剩余的记录
        if data_array:
            cursor.executemany(insert_news_sql, data_array)
            connection.commit()  # 提交剩余的事务
            logger.info("插入news_records完毕")
    except Exception as e:
        connection.rollback()  # 如果出错，回滚事务
        logger.error(e)
        logger.error("news_records插入失败")

    return id


def query_data_dict_from_db():
    logger.info("开始聚合mysql中的keyword与evnet")
    sql = "SELECT * FROM keyword"
    cursor.execute(sql)
    results = cursor.fetchall()
    id_keyword_map = {}
    data_dict = {}
    for item in results:
        id_keyword_map[item['id']] = item['keyword']
        data_dict[item['keyword']] = {
            'heat': float(item['heat']),
            'create_time': pd.to_datetime(item['create_time']),
            'update_time': pd.to_datetime(item['update_time']),
            'records': {}
        }

    sql = "SELECT * FROM event"
    cursor.execute(sql)
    results = cursor.fetchall()
    for item in results:
        ids_str_list = item['news_ids'].split(' ')
        ids_num_list = [int(id_str) for id_str in ids_str_list]
        data_dict[id_keyword_map[item['keyword_id']]]['records'][item['description']] = {
            'heat': float(item['heat']),
            'sentiment': float(item['sentiment']),
            'ids': ids_num_list,
            'urls': item['urls'].split(' '),
            'event_datetime': item['event_datetime'],
            'create_time': pd.to_datetime(item['create_time']),
            'update_time': pd.to_datetime(item['update_time']),
        }
    logger.info("mysql中的keyword与event聚合完毕")
    logger.debug(data_dict)
    return data_dict


# 释放连接
def release_connection():
    cursor.close()
    connection.close()


if __name__ == "__main__":
    data = query_data_dict_from_db()
    # insert_news_records(pd.read_csv("./train_data/raw_data.csv"), 1000)

    # with open('./debug/clean_train_result_with_urls.json', 'r') as file:
    #     # 读取文件内容并解析为Python字典
    #     data = json.load(file)
    # insert_keyword_with_event(data)

    # 调用函数，解析并插入数据
    release_connection()
