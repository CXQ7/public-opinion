import requests
import datetime as datetime1
from datetime import datetime

class SouthWeekendConf:
    def __init__(self, dayBefore, debug=False, output_as_file=False):
        """对象初始化"""
        self.threads = []
        self.threads_count = 0
        self.write_count = 0
        self.debug = debug
        self.output_as_file = output_as_file
        self.latest = datetime.today() - datetime1.timedelta(days=dayBefore)
        if self.debug:
            print("最后一次的爬取时间:", self.latest, '\n')

    def crawl_data(self):
        """爬取页面信息"""
        latest = self.latest
        page_index = 0
        to_the_end = False
        while to_the_end == False:
            page_index += 1
            if self.debug:
                print(f"crawling page {page_index}")
            try:
                url = f'https://www.infzm.com/contents?term_id=1&page={page_index}&format=json'
                response = requests.get(url)
                data = response.json()
                for article in data['data']['contents']:
                    t = {}
                    t["source"] = '南方周末'
                    t["title"] = article['subject']
                    t["url"] = 'https://www.infzm.com/contents/' + str(article['id'])
                    t["publsh_time"] = article['publish_time']
                    if article['author'] == "":
                        t["author"] = None
                    else:
                        t["author"] = article['author']
                    if article['introtext'] == "":
                        t["content"] = None
                    else:
                        t['content'] = article['introtext']
                    t['like_count'] = article['ding_count']
                    t['share_count'] = article['share_count']
                    t["comment_count"] = article['comment_count']
                    t['read_count'] = None
                    new = datetime.strptime(article['publish_time'], "%Y-%m-%d %H:%M:%S")
                    if new > latest:
                        self.threads.append(t)
                        self.threads_count += 1
                        if self.debug:
                            print(t)
                            print("Succeeded in crawling one thread!\n")
                    else:
                        to_the_end = True
                        break
            except:
                if self.debug:
                    print("get info err")

        if self.debug:
            print('---' * 30)
            print("The latest data crawling is completed!\n")


if __name__ == "__main__":
    a = SouthWeekendConf(10, True, True)
    a.crawl_data()
    print("爬取完毕，结果如下:")
    print(a.threads)
