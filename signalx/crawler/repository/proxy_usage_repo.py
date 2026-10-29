from typing import Optional, List
from datetime import datetime
from sqlalchemy import and_
from sqlalchemy import update

from publicity_idl.domain import ProxyUsageDO, ProxyUsage
from repository.base import Repo

import time

class ProxyUsageRepo(Repo):
    def __init__(self):
        super().__init__(ProxyUsageDO, ProxyUsage)

    def generate_id(self):
        return int(time.time()) << 32

    def upsert_usage_stats(self, source: str, ip: str, success: bool) -> dict:
        def operation(session):
            # 更新记录
            stmt = (
                update(self.do_cls)
                .where(and_(self.do_cls.source == source, self.do_cls.ip == ip))
                .values(
                    total_cnt=self.do_cls.total_cnt + 1,
                    success_cnt=self.do_cls.success_cnt + (1 if success else 0),
                    update_time=datetime.now()
                )
            )
            session.execute(stmt)
            session.commit()

            # 查询更新后的记录
            result = session.query(self.do_cls).filter(
                and_(self.do_cls.source == source, self.do_cls.ip == ip)
            ).one_or_none()

            if not result:
                # 如果记录不存在，创建新记录
                new_proxy_usage = ProxyUsageDO(
                    id=self.generate_id(),
                    source=source,
                    ip=ip,
                    success_cnt=1 if success else 0,
                    total_cnt=1,
                    update_time=datetime.now()
                )
                session.add(new_proxy_usage)
                session.commit()
                return {
                    "total_cnt": new_proxy_usage.total_cnt,
                    "success_rate": new_proxy_usage.calculate_success_rate()  # 动态计算 success_rate
                }

            return {
                "total_cnt": result.total_cnt,
                "success_rate": result.calculate_success_rate()  # 动态计算 success_rate
            }

        return self._with_rollback(operation)

    def exists(self, source: str, ip: str) -> bool:
        """判断某个 source 和 ip 是否存在于数据库"""
        def operation(session):
            return session.query(self.do_cls).filter(
                and_(self.do_cls.source == source, self.do_cls.ip == ip)
            ).count() > 0
        return self._with_rollback(operation)

    def increase_success(self, source: str, ip: str) -> dict:
        """增加成功次数"""
        return self.upsert_usage_stats(source, ip, success=True)

    def increase_fail(self, source: str, ip: str) -> Optional[dict]:
        """增加失败次数并检查淘汰条件"""
        if self.exists(source, ip):
            proxy_usage = self.upsert_usage_stats(source, ip, success=False)

            # 检查淘汰条件：total_cnt > 4 且 success_rate < 20
            if proxy_usage["total_cnt"] > 4 and proxy_usage["success_rate"] < 20:
                self.delete_by_source_ip(source, ip)
                self._log(f"淘汰代理: source={source}, ip={ip}, total_cnt={proxy_usage['total_cnt']}, success_rate={proxy_usage['success_rate']}")
                return None  # 返回 None 表示代理已被淘汰

            return proxy_usage


    def delete_by_source_ip(self, source: str, ip: str) -> bool:
        """根据渠道和IP删除记录"""
        def operation(session):
            rows_deleted = session.query(self.do_cls).filter(
                and_(self.do_cls.source == source, self.do_cls.ip == ip)
            ).delete()
            session.commit()
            return rows_deleted > 0

        result = self._with_rollback(operation)
        if not result:
            self._log(f"警告: source={source}, ip={ip}的记录不存在")
        return result


# 创建全局实例
proxy_usage_repo = ProxyUsageRepo()
