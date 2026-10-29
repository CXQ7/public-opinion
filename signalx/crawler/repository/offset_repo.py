from typing import Optional
from datetime import datetime

from publicity_idl.domain import SourceKeywordOffsetDO, SourceKeywordOffset
from repository.base import Repo

class TaskKeywordOffsetRepo(Repo):
    def __init__(self):
        super().__init__(SourceKeywordOffsetDO, SourceKeywordOffset)

    def get_by_task_keyword_and_source(self, task_id: int, keyword: str, source: str) -> Optional[SourceKeywordOffset]:
        """根据 task_id, keyword, source 查找或创建 offset"""
        def operation(session):
            query = session.query(self.do_cls).filter(
                self.do_cls.keyword_id == task_id,  # 复用 keyword_id 字段存储 task_id
                self.do_cls.source == f"{source}_{keyword}"  # 将 source 和 keyword 组合存储
            )
            orm_obj = query.first()
            if orm_obj is None:
                # 创建新的 offset 记录
                new_offset = SourceKeywordOffset(
                    keyword_id=task_id,
                    source=f"{source}_{keyword}",
                    create_time=datetime(1000, 1, 1),
                    update_time=datetime(1000, 1, 1)
                )
                return self.create(new_offset)
            return self.en_cls.from_orm(orm_obj)

        return self._with_rollback(operation)


offset_repo = TaskKeywordOffsetRepo()
