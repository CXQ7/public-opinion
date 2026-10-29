import os
import json
import requests
import pandas as pd
import datetime as datetime1
from datetime import datetime

class tengxun_infos:
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
        url_list = [
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aEGswbJah2Y%3D',
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aUStwLxYgWQ%3D',
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aEGsxLNdhWU%3D',
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aUWpxLNdg2c%3D',
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aUepxrtchGM%3D',
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aUStwL9QhGU%3D',
            'https://i.news.qq.com/web_feed/getCollectionArticles?tagId=aUeoyrtcgWo%3D',
            'https://i.news.qq.com/gw/event/pc_hot_ranking_list?ids_hash=&offset=0&page_size=50&appver=15.5_qqnews_7.1.60&rank_id=ent',
            'https://i.news.qq.com/gw/event/pc_hot_ranking_list?ids_hash=&offset=0&page_size=51&appver=15.5_qqnews_7.1.60&rank_id=hot'
        ]
        for url in url_list:
            response = requests.get(url)
            data = response.json()
            try:
                if url.find('getCollectionArticles') == -1:
                    for article in data['idlist'][0]["newslist"][1:]:
                        t={}
                        try:
                            t["source"] = '腾讯网'
                            t["title"] = self.trytry(article,'title')
                            t["url"] = self.trytry(article,'url')
                            t["publsh_time"] = self.trytry(article,'time')
                            t["author"] = self.trytry(article['card'],'chlname')
                            t['content'] = self.trytry(article,'abstract')
                            t['like_count'] = self.trytry(article,'likeInfo')
                            t['share_count'] = self.trytry(article,'shareCount')
                            t["comment_count"] = self.trytry(article,'commentNum')
                            t['read_count'] = self.trytry(article,'readCount')
                        except:
                            if self.debug:
                                print("get info err")
                        try:
                            new = datetime.strptime(t["publsh_time"], "%Y-%m-%d %H:%M:%S")
                        except:
                            if self.debug:
                                print("get info err")
                            continue
                        if new>latest:
                            self.threads.append(t)
                            self.threads_count += 1
                            if self.debug:
                                print(t)
                                print("Succeeded in crawling one thread!\n")
                else:
                    for article in data['data']['articleList']:
                        t={}
                        try:
                            t["source"] = '腾讯网'
                            t["title"] = self.trytry(article,'title')
                            t["url"] = self.trytry(article['link_info'],'url')
                            t["publsh_time"] = self.trytry(article,'publish_time')
                            t["author"] = self.trytry(article['media_info'],'chl_name')
                            t['content'] = self.trytry(article,'desc') + self.trytry(article['video_info'],'desc')
                            t['like_count'] = self.trytry(article,'likeInfo')
                            t['share_count'] = self.trytry(article,'shareCount')
                            t["comment_count"] = self.trytry(article,'commentNum')
                            t['read_count'] = self.trytry(article,'readCount')
                        except:
                            if self.debug:
                                print("get info err")
                        try:
                            new = datetime.strptime(t["publsh_time"], "%Y-%m-%d %H:%M:%S")
                        except:
                            if self.debug:
                                print("get info err")
                            continue
                        if new>latest:
                            self.threads.append(t)
                            self.threads_count += 1
                            if self.debug:
                                print(t)
                                print("Succeeded in crawling one thread!\n")
            except:
                if self.debug:
                    print("get url err")
        if self.debug:
            print('---'*30)
            print("The latest data crawling is completed!\n")
        return self.deal_info()


    def deal_info(self):
        """处理信息"""
        result_headers = [
            "source", "title", "url", "publish_time", "author", "content", "like_count", "share_count", "comment_count", "read_count"
        ]
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

    a = tengxun_infos(10, True, True)
    a.crawl_data()
