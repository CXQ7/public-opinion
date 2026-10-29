import time
import traceback

from repository.offset_repo import offset_repo
from utils import *
from common.constant import BaseApi, SourceEnum
import common.constant as const
from api_factory import ApiFactory
from model import Model

# 执行器类，为其分配api实例执行任务
class ApiExecutor(object):
    def __init__(self, api: BaseApi):
        print(f"Initializing ApiExecutor for {api.name}")
        self.api = api

    def start(self):
        try:
            print(f"Starting {self.api.name} ApiExecutor......")
            for task_keyword in const.task_keyword_list:
                time.sleep(const.KEYWORD_INTERVAL)

                offset_info = offset_repo.get_by_task_keyword_and_source(
                    task_keyword.task_id,
                    task_keyword.keyword,
                    self.api.source.name,
                )
                new_data_arr, vis_title_set = [], {}
                old_latest_time = new_latest_time = offset_info.update_time

                logger.info(
                    f'开始爬取{self.api.name}关键词[{task_keyword.keyword}]的数据，上次爬取时间为{old_latest_time}'
                )

                new_cnt, total_cnt = 0, 0
                for page in range(2):
                    time.sleep(const.PAGE_INTERVAL)
                    data = self.api.search(task_keyword.keyword, page)
                    total_cnt += len(data)
                    for item in data:
                        try:
                            if (
                                    item.publish_time is None
                                    or item.title is None
                                    or item.title in vis_title_set  # 目前仍会出现单次翻页爬到同一新闻的情况, 需要手动去重
                            ):
                                continue

                            vis_title_set[item.title] = True
                            publish_time = item.publish_time
                            if publish_time > old_latest_time:
                                item.task_id = task_keyword.task_id
                                new_latest_time = max(publish_time, new_latest_time)
                                new_data_arr.append(item)
                                new_cnt += 1
                            else:
                                if self.api.source == SourceEnum.toutiao or self.api.source == SourceEnum.wechat:
                                    continue
                                else:
                                    break
                        except Exception as e:

                            logger.error(f"execute failed, {e}, {self.api.source},{item}")
                            continue
                update_ratio = 0.0
                if total_cnt > 0:
                    update_ratio = round(new_cnt / total_cnt, 4)

                offset_info.update_time = new_latest_time

                logger.info(
                    f'{self.api.name} API获取到{total_cnt}条关键词[{task_keyword.keyword}]的数据，更新{new_cnt}条，更新率：{100 * update_ratio}%'
                )
                logger.debug(f"new_data_arr: {new_data_arr}")

                Model(new_data_arr, task_keyword).train()

                offset_repo.update(offset_info)

            logger.info(f"{self.api.name} Executor Finished")
        except Exception as e:
            logger.error(f"executor start failed, {e}")
            traceback.print_exc()
        return self

if __name__ == "__main__":
    factory = ApiFactory()
    toutiao_executor = ApiExecutor(factory.get_api(SourceEnum.toutiao))
    toutiao_executor.start()
