# import requests
# import json
# import time
# from datetime import datetime
# import re
# import logging
# import os
# from docx import Document
# from docx.shared import Inches

# # 配置日志
# logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
# logger = logging.getLogger(__name__)

# def get_articles():
#     # 请求头设置
#     headers = {
#         "Cookie": "__CONFIGURE_LOCALLY__",
#         "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
#     }

#     session = requests.Session()

#     try:
#         # 获取token
#         response = session.get("https://mp.weixin.qq.com/", headers=headers)
#         token_match = re.search(r'token=(\d+)', response.text)
#         if not token_match:
#             logger.error("无法获取token")
#             return []

#         token = token_match.group(1)
#         logger.info("获取token成功")

#         # 搜索公众号
#         query = {
#             "action": "search_biz",
#             "token": token,
#             "lang": "zh_CN",
#             "f": "json",
#             "ajax": "1",
#             "random": str(int(time.time() * 1000)),
#             "query": "厦信青年",
#             "begin": "0",
#             "count": "5"
#         }

#         response = session.get(
#             "https://mp.weixin.qq.com/cgi-bin/searchbiz",
#             headers=headers,
#             params=query
#         )
#         data = response.json()

#         if not data.get("list"):
#             logger.error("未找到公众号")
#             return []

#         fakeid = data["list"][0]["fakeid"]
#         logger.info("获取公众号ID成功")

#         # 获取文章列表
#         articles = []
#         begin = 0
#         batch_size = 10

#         while True:
#             query = {
#                 "action": "list_ex",
#                 "begin": str(begin),
#                 "count": str(batch_size),
#                 "fakeid": fakeid,
#                 "type": "9",
#                 "query": "共青团",
#                 "token": token,
#                 "lang": "zh_CN",
#                 "f": "json",
#                 "ajax": "1"
#             }

#             response = session.get(
#                 "https://mp.weixin.qq.com/cgi-bin/appmsg",
#                 headers=headers,
#                 params=query
#             )
#             data = response.json()

#             if not data.get("app_msg_list"):
#                 break

#             for item in data["app_msg_list"]:
#                 try:
#                     # 获取文章详情
#                     article_url = item["link"]
#                     article_response = session.get(article_url, headers=headers)
#                     article_content = article_response.text

#                     # 检查文章内容中是否包含关键词
#                     keywords = ["厦门", "共青团"]  # 可以添加多个关键词
#                     if any(keyword in article_content for keyword in keywords):
#                         article = {
#                             "标题": item["title"],
#                             "发布时间": datetime.fromtimestamp(item["create_time"]).strftime("%Y-%m-%d %H:%M:%S"),
#                             "链接": item["link"],
#                             "作者": item.get("author", "未知")
#                         }
#                         articles.append(article)
#                         logger.info(f"找到包含关键词的文章: {item['title']}")

#                     # 添加延时避免请求过于频繁
#                     time.sleep(0.05)

#                 except Exception as e:
#                     logger.error(f"获取文章详情失败: {str(e)}")

#                 time.sleep(0.05)

#             begin += batch_size
#             if begin >= data.get("app_msg_cnt", 0):
#                 break

#         return articles

#     except Exception as e:
#         logger.error(f"发生错误: {str(e)}")
#         return []

# def save_to_word(articles):
#     """保存文章到Word文档"""
#     if not articles:
#         logger.warning("没有文章可以保存")
#         return

#     try:
#         # 指定保存路径
#         current_path = os.path.realpath(__file__)
#         directory_path = os.path.dirname(current_path)
#         filename = directory_path + "/wechat.docx"

#         # 确保目录存在
#         os.makedirs(os.path.dirname(filename), exist_ok=True)

#         # 创建新的Word文档
#         doc = Document()

#         # 添加标题
#         doc.add_heading('微信公众号文章列表', 0)

#         # 添加文章信息
#         for article in articles:
#             # 添加文章标题
#             doc.add_heading(article['标题'], level=1)

#             # 添加文章信息
#             doc.add_paragraph(f"发布时间: {article['发布时间']}")
#             doc.add_paragraph(f"作者: {article['作者']}")
#             doc.add_paragraph(f"链接: {article['链接']}")

#             # 添加分隔线
#             doc.add_paragraph('=' * 50)

#         # 保存文档
#         doc.save(filename)
#         logger.info(f"数据已保存到: {os.path.abspath(filename)}")
#         logger.info(f"保存了 {len(articles)} 篇文章")

#     except Exception as e:
#         logger.error(f"保存Word文档失败: {str(e)}")
#         logger.exception("详细错误信息：")

# def main():
#     try:
#         logger.info("开始爬取文章...")
#         articles = get_articles()

#         if articles:
#             logger.info(f"共获取到 {len(articles)} 篇文章")
#             logger.info(f"示例文章: {articles[0]['标题']}")
#             save_to_word(articles)
#         else:
#             logger.warning("未获取到文章")

#     except Exception as e:
#         logger.error(f"程序运行出错: {str(e)}")
#         logger.exception("详细错误信息：")

# if __name__ == "__main__":
#     main()
