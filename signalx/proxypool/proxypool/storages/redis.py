import redis
from proxypool.exceptions import PoolEmptyException
from proxypool.schemas.proxy import Proxy
from proxypool.setting import REDIS_CONNECTION_STRING, REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_DB, REDIS_KEY_DEFAULT_PROXY, PROXY_SCORE_MAX, PROXY_SCORE_MIN,REDIS_KEY_CANDIDATE, \
    PROXY_SCORE_INIT
from random import choice
from typing import List
from loguru import logger
from proxypool.utils.proxy import is_valid_proxy, convert_proxy_or_proxies


REDIS_CLIENT_VERSION = redis.__version__
IS_REDIS_VERSION_2 = REDIS_CLIENT_VERSION.startswith('2.')


class RedisClient(object):
    """
    redis connection client of proxypool
    """

    def get_redis_key(self, source: str) -> str:
        if source:
            return f"proxies:{source}"
        return REDIS_KEY_DEFAULT_PROXY

    def set_to_pool(self, proxy: Proxy, score=PROXY_SCORE_INIT, redis_key=REDIS_KEY_DEFAULT_PROXY,source= None) -> None:

        if source != None:
            redis_key = self.get_redis_key(source)

        if not is_valid_proxy(f'{proxy.host}:{proxy.port}'):
            logger.info(f'Invalid proxy {proxy}, skipping sync to Redis')
            return

        # Add the proxy to the Redis pool if it doesn't already exist
        if not self.exists(proxy, redis_key):
            if IS_REDIS_VERSION_2:
                self.db.zadd(redis_key, score, proxy.string())
            else:
                self.db.zadd(redis_key, {proxy.string(): score})
            logger.info(f'Successfully synced proxy {proxy.string()} to Redis pool {redis_key}')
        else:
            if IS_REDIS_VERSION_2:
                self.db.zadd(redis_key, score, proxy.string())
            else:
                self.db.zadd(redis_key, {proxy.string(): score})
            logger.info(f'Updated proxy {proxy.string()} score in Redis pool {redis_key} to {score}')


    def __init__(self, host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD, db=REDIS_DB,
                 connection_string=REDIS_CONNECTION_STRING, **kwargs):
        """
        init redis client
        :param host: redis host
        :param port: redis port
        :param password: redis password
        :param connection_string: redis connection_string
        """
        # if set connection_string, just use it
        if connection_string:
            self.db = redis.StrictRedis.from_url(connection_string, decode_responses=True, **kwargs)
        else:
            self.db = redis.StrictRedis(
                host=host, port=port, password=password, db=db, decode_responses=True, **kwargs)

    def add(self, proxy: Proxy, score=PROXY_SCORE_INIT, redis_key=REDIS_KEY_DEFAULT_PROXY) -> int:
        """
        add proxy and set it to init score
        :param proxy: proxy, ip:port, like 127.0.0.1:88
        :param score: int score
        :return: result
        """
        if not is_valid_proxy(f'{proxy.host}:{proxy.port}'):
            logger.info(f'invalid proxy {proxy}, throw it')
            return
        if not self.exists(proxy, redis_key):
            if IS_REDIS_VERSION_2:
                return self.db.zadd(redis_key, score, proxy.string())
            return self.db.zadd(redis_key, {proxy.string(): score})

    def random(self, redis_key=REDIS_KEY_CANDIDATE, proxy_score_min=PROXY_SCORE_MIN, proxy_score_max=PROXY_SCORE_MAX) -> Proxy:
        """
        get random proxy
        firstly try to get proxy with max score
        if not exists, try to get proxy by rank
        if not exists, raise error
        :return: proxy, like 127.0.0.1:8
        """
        # # try to get proxy with max score
        # proxies = self.db.zrangebyscore(
        #     redis_key, proxy_score_max, proxy_score_max)
        # if len(proxies):
        #     return convert_proxy_or_proxies(choice(proxies))
        # else get proxy by rank
        proxies = self.db.zrevrangebyscore(redis_key, proxy_score_max, proxy_score_min)
        if len(proxies):
            return convert_proxy_or_proxies(choice(proxies))
        # else raise error
        raise PoolEmptyException

    def decrease(self, proxy: Proxy, redis_key=REDIS_KEY_DEFAULT_PROXY, proxy_score_min=PROXY_SCORE_MIN) -> int:
        """
        decrease score of proxy, if small than PROXY_SCORE_MIN, delete it
        :param proxy: proxy
        :return: new score
        """
        if IS_REDIS_VERSION_2:
            self.db.zincrby(redis_key, proxy.string(), -1)
        else:
            self.db.zincrby(redis_key, -1, proxy.string())
        score = self.db.zscore(redis_key, proxy.string())
        logger.info(f'{proxy.string()} score decrease 1, current {score}')
        if score <= proxy_score_min:
            logger.info(f'{proxy.string()} current score {score}, remove')
            self.db.zrem(redis_key, proxy.string())

    def exists(self, proxy: Proxy, redis_key=REDIS_KEY_DEFAULT_PROXY) -> bool:
        """
        if proxy exists
        :param proxy: proxy
        :return: if exists, bool
        """
        return not self.db.zscore(redis_key, proxy.string()) is None

    def max(self, proxy: Proxy, redis_key=REDIS_KEY_DEFAULT_PROXY, proxy_score_max=PROXY_SCORE_MAX) -> int:
        """
        set proxy to max score
        :param proxy: proxy
        :return: new score
        """
        logger.info(f'{proxy.string()} is valid, set to {proxy_score_max}')
        if IS_REDIS_VERSION_2:
            return self.db.zadd(redis_key, proxy_score_max, proxy.string())
        return self.db.zadd(redis_key, {proxy.string(): proxy_score_max})

    def count(self, redis_key=REDIS_KEY_DEFAULT_PROXY) -> int:
        """
        get count of proxies
        :return: count, int
        """
        return self.db.zcard(redis_key)

    def all(self, redis_key=REDIS_KEY_DEFAULT_PROXY, proxy_score_min=PROXY_SCORE_MIN, proxy_score_max=PROXY_SCORE_MAX) -> List[Proxy]:
        """
        get all proxies
        :return: list of proxies
        """
        return convert_proxy_or_proxies(self.db.zrangebyscore(redis_key, proxy_score_min, proxy_score_max))

    def batch(self, cursor, count, redis_key=REDIS_KEY_DEFAULT_PROXY) -> List[Proxy]:
        """
        get batch of proxies
        :param cursor: scan cursor
        :param count: scan count
        :return: list of proxies
        """
        cursor, proxies = self.db.zscan(redis_key, cursor, count=count)
        return cursor, convert_proxy_or_proxies([i[0] for i in proxies])

    def get_all_sources(self) -> List[str]:
        """
        获取 Redis 中所有子池的名称
        :return: 子池名称列表
        """
        return [key for key in self.db.scan_iter("proxies:*")]

    def get_all_proxies(self, redis_key=REDIS_KEY_DEFAULT_PROXY) -> List[Proxy]:
       """
       获取 Redis 中所有代理
       :return: 代理列表
       """
       return convert_proxy_or_proxies(self.db.zrange(redis_key, 0, -1))

    def get_all_proxies(self, redis_key: str) -> List[str]:
        """
        获取 Redis 子池中的所有代理
        :param redis_key: 子池名称
        :return: 代理列表
        """
        return self.db.zrange(redis_key, 0, -1)

    def remove_proxy(self, proxy: str, redis_key: str) -> None:
        """
        从 Redis 子池中移除代理
        :param proxy: 代理字符串
        :param redis_key: 子池名称
        """
        self.db.zrem(redis_key, proxy)
        logger.info(f"从 Redis 子池 {redis_key} 中移除代理: {proxy}")


if __name__ == '__main__':
    conn = RedisClient()
    result = conn.random()
    print(result)
