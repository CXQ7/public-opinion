import time
import multiprocessing
from proxypool.processors.server import app
from proxypool.processors.getter import Getter
from proxypool.processors.tester import Tester
from proxypool.setting import APP_PROD_METHOD_GEVENT, APP_PROD_METHOD_MEINHELD, APP_PROD_METHOD_TORNADO, CYCLE_GETTER, CYCLE_TESTER, API_HOST, \
    API_THREADED, API_PORT, ENABLE_SERVER, IS_PROD, APP_PROD_METHOD, \
    ENABLE_GETTER, ENABLE_TESTER, IS_WINDOWS, SYNC_DB_TO_REDIS_CYCLE
from loguru import logger

from proxypool.repository.proxy_usage_repo import proxy_usage_repo
from proxypool.storages.redis import RedisClient
from proxypool.schemas.proxy import Proxy


if IS_WINDOWS:
    multiprocessing.freeze_support()

tester_process, getter_process, server_process = None, None, None


class Scheduler():
    """
    scheduler
    """

    def run_sync_db_to_redis(self, cycle=SYNC_DB_TO_REDIS_CYCLE):
        redis_client = RedisClient()
        loop = 0
        while True:
            logger.info(f'sync_db_to_redis loop {loop} start...')

            # 从数据库获取所有代理
            db_proxies = proxy_usage_repo.get_all()
            db_proxy_set = {f"{proxy.ip}" for proxy in db_proxies}  # 数据库中的代理集合

            # 从 Redis 获取所有子池的代理
            redis_proxy_set = set()
            for source in redis_client.get_all_sources():
                # 跳过 candidate 和 universal 池
                if source in ["proxies:candidate", "proxies:universal"]:
                    continue
                redis_proxy_set.update(redis_client.get_all_proxies(source))

            # 找出 Redis 中多余的代理
            extra_proxies = redis_proxy_set - db_proxy_set
            for proxy in extra_proxies:
                try:
                    for source in redis_client.get_all_sources():
                        # 仅从非 candidate 和 universal 池中移除
                        if source not in ["proxies:candidate", "proxies:universal"]:
                            redis_client.remove_proxy(proxy, source)
                except Exception as e:
                    logger.warning(f"移除代理 {proxy} 时出错: {e}")

            # 同步数据库中的代理到 Redis
            count = 0
            for proxy in db_proxies:
                if proxy.source and proxy.source != 'default':
                    try:
                        ip, port = proxy.ip.split(':')
                        success_cnt = proxy.success_cnt
                        total_cnt = proxy.total_cnt
                        redis_client.set_to_pool(
                            Proxy(host=ip, port=int(port)),
                            source=proxy.source,
                            score=success_cnt / total_cnt * 100 if total_cnt > 0 else 0
                        )
                        count += 1
                    except Exception as e:
                        logger.warning(f"同步代理 {proxy.ip} 到子池 {proxy.source} 失败: {e}")

            logger.info(f"已同步 {count} 条代理到 Redis 子池")
            loop += 1
            time.sleep(cycle)


    def run_tester(self, cycle=CYCLE_TESTER):
        """
        run tester
        """
        if not ENABLE_TESTER:
            logger.info('tester not enabled, exit')
            return
        tester = Tester()
        loop = 0
        while True:
            logger.debug(f'tester loop {loop} start...')
            tester.run()
            loop += 1
            time.sleep(cycle)

    def run_getter(self, cycle=CYCLE_GETTER):
        """
        run getter
        """
        if not ENABLE_GETTER:
            logger.info('getter not enabled, exit')
            return
        getter = Getter()
        loop = 0
        while True:
            logger.debug(f'getter loop {loop} start...')
            getter.run()
            loop += 1
            time.sleep(cycle)

    def run_server(self):
        """
        run server for api
        """
        if not ENABLE_SERVER:
            logger.info('server not enabled, exit')
            return
        if IS_PROD:
            if APP_PROD_METHOD == APP_PROD_METHOD_GEVENT:
                try:
                    from gevent.pywsgi import WSGIServer
                except ImportError as e:
                    logger.exception(e)
                else:
                    http_server = WSGIServer((API_HOST, API_PORT), app)
                    http_server.serve_forever()

            elif APP_PROD_METHOD == APP_PROD_METHOD_TORNADO:
                try:
                    from tornado.wsgi import WSGIContainer
                    from tornado.httpserver import HTTPServer
                    from tornado.ioloop import IOLoop
                except ImportError as e:
                    logger.exception(e)
                else:
                    http_server = HTTPServer(WSGIContainer(app))
                    http_server.listen(API_PORT)
                    IOLoop.instance().start()

            elif APP_PROD_METHOD == APP_PROD_METHOD_MEINHELD:
                try:
                    import meinheld
                except ImportError as e:
                    logger.exception(e)
                else:
                    meinheld.listen((API_HOST, API_PORT))
                    meinheld.run(app)

            else:
                logger.error("unsupported APP_PROD_METHOD")
                return
        else:
            app.run(host=API_HOST, port=API_PORT, threaded=API_THREADED, use_reloader=False)

    def run(self):
        global tester_process, getter_process, server_process, sync_process
        try:
            logger.info('starting proxypool...')
            if ENABLE_TESTER:
                tester_process = multiprocessing.Process(
                    target=self.run_tester)
                logger.info(f'starting tester, pid {tester_process.pid}...')
                tester_process.start()

            if ENABLE_GETTER:
                getter_process = multiprocessing.Process(
                    target=self.run_getter)
                logger.info(f'starting getter, pid {getter_process.pid}...')
                getter_process.start()

            if ENABLE_SERVER:
                server_process = multiprocessing.Process(
                    target=self.run_server)
                logger.info(f'starting server, pid {server_process.pid}...')
                server_process.start()

            sync_process = multiprocessing.Process(
            target=self.run_sync_db_to_redis)
            logger.info(f'starting sync_db_to_redis, pid {sync_process.pid}...')
            sync_process.start()

            tester_process and tester_process.join()
            getter_process and getter_process.join()
            server_process and server_process.join()
            sync_process and sync_process.join()
        except KeyboardInterrupt:
            logger.info('received keyboard interrupt signal')
            tester_process and tester_process.terminate()
            getter_process and getter_process.terminate()
            server_process and server_process.terminate()
            sync_process and sync_process.terminate()
        finally:
            # must call join method before calling is_alive
            tester_process and tester_process.join()
            getter_process and getter_process.join()
            server_process and server_process.join()
            sync_process and sync_process.join()
            logger.info(
                f'tester is {"alive" if tester_process.is_alive() else "dead"}')
            logger.info(
                f'getter is {"alive" if getter_process.is_alive() else "dead"}')
            logger.info(
                f'server is {"alive" if server_process.is_alive() else "dead"}')
            logger.info(
            f'sync_db_to_redis is {"alive" if sync_process.is_alive() else "dead"}')
            logger.info('proxy terminated')


if __name__ == '__main__':
    scheduler = Scheduler()
    scheduler.run()
