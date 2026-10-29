import time

import common.constant as const
import concurrent.futures
from api_factory import ApiFactory
from api_executor import ApiExecutor

class ApiScheduler:
    def __init__(self):
        print("Initializing ApiScheduler......")
        self.factory = ApiFactory()
        self.pool = concurrent.futures.ThreadPoolExecutor()

    def start_once(self):
        # 每次触发都刷新来源配置与任务关键词，避免进程启动后“锁死”
        const.source_set = const.load_source_set()
        const.task_keyword_list = const.load_task_keyword_list()
        executors = [ApiExecutor(self.factory.get_api(source)) for source in const.source_set]
        futures = [self.pool.submit(executor.start) for executor in executors]
        concurrent.futures.wait(futures)  # 等待所有任务完成
        print("finished all tasks")

    def start(self):
        executors = [ApiExecutor(self.factory.get_api(source)) for source in const.source_set]
        for executor in executors:
            future = self.pool.submit(executor.start)
            future.add_done_callback(self.interval_callback)

    # 休眠一段时间的回调
    def interval_callback(self, future: concurrent.futures.Future):
        print(f"Task {future} is done, sleeping {const.SCHEDULER_INTERVAL} seconds.")
        time.sleep(const.SCHEDULER_INTERVAL)
        executor = future.result()
        new_future = self.pool.submit(executor.start)
        new_future.add_done_callback(self.interval_callback)


if __name__ == "__main__":
    scheduler = ApiScheduler()
    scheduler.start()
    while True:
        pass
