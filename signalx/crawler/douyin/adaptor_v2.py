# -*- coding: utf-8 -*-
import os
from datetime import datetime

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait  # 显示等待
from selenium.webdriver.common.action_chains import ActionChains  # 鼠标操作
import urllib.parse
import json
from time import sleep

from config.config import selenium_config


class DouyinAdaptor:

    def __init__(self):
        """对象初始化"""
        CONTANT = json.load(open('./common/CONTANT.json', 'r', encoding='utf-8'))
        self.infos = CONTANT['douyin']
        self.base_url = self.infos['base_url']
        self.threads = []

        self.browser = selenium_config.get_browser()

    def try_get_element(self, fathor_item, item):
        """尝试获取元素"""
        try:
            element = fathor_item.find_element(self.infos['By'][item], self.infos['find_element'][item])
            return element
        except Exception as e:
            print(f"cannot find element: {item}")
            return None


    def get_info(self):
        """获取帖子信息"""
        try:
            sleep(5)
            filter_btn = self.try_get_element(self.browser, 'filter_btn')
            actions = ActionChains(self.browser)
            actions.move_to_element(filter_btn).perform()
            # self.try_get_element(self.browser, 'filter_option_sort_time').click()
            sleep(1)
            self.try_get_element(self.browser, 'filter_option_time_day').click()
            try_cnt = 0
            while try_cnt < 3:
                self.browser.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                elements = self.browser.find_elements(self.infos['By']['page_end'], self.infos['find_element']['page_end'])
                if elements[-1].text == "暂时没有更多了":
                    print("已到达页面底部")
                    break
                try_cnt += 1
                sleep(0.5)
            if try_cnt == 3:
                print("页面加载失败")
                return False
            sleep(2)
            items = self.browser.find_elements(self.infos['By']['article_item'], self.infos['find_element']['article_item'])
            print(f"获取items成功, 共{len(items)}个元素")
        except Exception as e:
            print(f"无法加载帖子列表, exception: {e}")
            sleep(2)
            return False
        for item in items:
            try:
                t={}
                id = item.find_element(By.XPATH, '..').get_attribute("id").split("_")[2]
                if self.try_get_element(item, 'picture_tag') is not None:
                    type = "note"
                else:
                    type = "video"
                t['third_id'] = id
                t['url'] = self.base_url + f"/{type}/" + id
                t["img"] = self.try_get_element(item, 'img').get_attribute("src")
                t["like_count"] = item.find_element(self.infos['By']['like_count'], self.infos['find_element']['like_count'][type]).text
                t["content"] = self.try_get_element(item, 'content').text
                t["author"] = self.try_get_element(item, 'author').text
                t["publish_datetime"] = _id_to_time(id)
                t["abstract_publish_datetime"] = self.try_get_element(item, 'publish_datetime').text
                print("获取帖子信息成功")
                self.threads.append(t)
                print(t)
            except Exception as e:
                print(item.get_attribute("text"))
                print(f"获取帖子信息失败, exception: {e}")
                continue
        return len(items) > 0


    def search(self, keyword = "深圳", page = None, page_size = None):
        """搜索关键词"""
        encoded = urllib.parse.quote(keyword)
        self.url = self.base_url + '/search/' + encoded
        self.browser.get(self.url)
        self.browser.maximize_window()
        self.wait = WebDriverWait(self.browser,5)
        sleep(1)
        try_cnt = 0
        while try_cnt < 3:
            if self.get_info():
                break
            try_cnt += 1
            print("尝试获取抖音信息失败，重试中...")
        return self.threads

    def close(self):
        """释放资源"""
        try:
            self.browser.quit()
        except Exception as e:
            print(f"close error: {e}")
            return None

def _id_to_time(third_id):
    return datetime.fromtimestamp(int(third_id) >> 32)


if __name__ == "__main__":
    print(os.getcwd())
    # adaptor = douyinAdaptor()
    # results = adaptor.search()
    # adaptor.close()
