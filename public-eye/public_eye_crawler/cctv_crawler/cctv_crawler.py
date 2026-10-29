import os
import json
import requests
import pandas as pd
import datetime as datetime1
from datetime import datetime

class cctv_infos:
    def __init__(self, dayBefore, debug = False, output_as_file=False):
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
        while to_the_end == False and page_index <= 10:
            page_index += 1
            if self.debug:
                print(f"crawling page {page_index}")
            try:
                url = f'https://news.cctv.com/2019/07/gaiban/cmsdatainterface/page/china_{page_index}.jsonp?cb=china'
                response = requests.get(url)
                text = str(response.content, 'utf-8')
                # 找到第一个'('和最后一个')'的位置
                start_index = text.find('(')
                end_index = text.rfind(')')
                if start_index != -1 and end_index != -1:
                    # 截取字符串
                    string = text[start_index + 1: end_index]
                    data = json.loads(string)
                for article in data['data']['list']:
                    t={}
                    t["source"] = '央视网'
                    t["title"] = article['title']
                    t["url"] = article['url']
                    t["publsh_time"] = article['focus_date']
                    t["author"] = None
                    t['content'] = article['brief']
                    t['like_count'] = None
                    t['share_count'] = None
                    t["comment_count"] = None
                    t['read_count'] = None
                    new = datetime.strptime(article['focus_date'], "%Y-%m-%d %H:%M:%S")
                    if new>latest:
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
            print('---'*30)
            print("The latest data crawling is completed!\n")
        return self.deal_info()


    def deal_info(self):
        """处理信息"""
        result_headers = [
            "source", "title", "url", "publish_time", "author", "content", "like_count", "share_count", "comment_count", "read_count"
        ] # 要写入结果文件的表头
        result_data = [t.values() for t in self.threads[self.write_count:]]
        df = pd.DataFrame(data=result_data, columns=result_headers)
        if self.output_as_file:
            file_path =  os.path.split(os.path.realpath(__file__))[0] + os.sep + 'crawler_result.csv'
            df.to_csv(file_path, encoding="utf_8_sig", index_label='event_id')
        if self.debug:
            df.info()
        return df


    def trytry(self, artcle, tar_str):
        """尝试获取目标字段值"""
        try:
            return artcle[tar_str]
        except:
            return None


if __name__ == "__main__":

    a = cctv_infos(1, True, True)
    a.crawl_data()
