from typing import Optional, List
from datetime import datetime
from sqlalchemy import and_
from sqlalchemy import update

from publicity_idl.domain import ProxyUsageDO, ProxyUsage
from proxypool.repository.base import Repo


class ProxyUsageRepo(Repo):
    def __init__(self):
        super().__init__(ProxyUsageDO, ProxyUsage)

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

    def get_by_source(self, source: str, limit: int = None, offset: int = 0) -> List[ProxyUsage]:
        """根据渠道获取代理使用记录"""
        def operation(session):
            query = session.query(self.do_cls).filter(self.do_cls.source == source)
            if limit is not None:
                query = query.limit(limit)
            if offset > 0:
                query = query.offset(offset)
            orm_objs = query.all()
            return [self.en_cls.from_orm(obj) for obj in orm_objs]

        return self._with_rollback(operation)

    def get_all(self) -> List[ProxyUsage]:
        """获取所有代理使用记录"""
        def operation(session):
            orm_objs = session.query(self.do_cls).all()
            return [self.en_cls.from_orm(obj) for obj in orm_objs]
        return self._with_rollback(operation)

# 创建全局实例
proxy_usage_repo = ProxyUsageRepo()
