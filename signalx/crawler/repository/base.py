import traceback
from datetime import datetime
from typing import List

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from dataclasses import dataclass

from publicity_idl.domain import NewsDO, News, SliceDO, Slice, TaskDO, Task
from publicity_idl import repo as idl_repo

@dataclass
class DBConfig:
    # host: str = '127.0.0.1'
    # user: str = 'root'
    # password: str = '20260202'

    # 本机配置
    host: str = 'localhost'  # 数据库主机地址
    user: str = 'root'  # 数据库用户名
    password: str = __import__("os").environ.get("PASSWORD", "")  # 数据库密码

    port: int = 3306
    database: str = 'signalx'
    charset: str = 'utf8mb4'
    collation: str = 'utf8mb4_unicode_ci'

class Repo:
    def __init__(self, do_cls, en_cls):
        conf = DBConfig()

        self.do_cls = do_cls
        self.en_cls = en_cls
        self.engine = create_engine(f'mysql+mysqlconnector://'
                                    f'{conf.user}:{conf.password}@'
                                    f'{conf.host}:{conf.port}'
                                    f'/{conf.database}')
        self._session_factory = sessionmaker(bind=self.engine)

    def create(self, instance=None):
        do = instance.to_orm()

        def operation(session):
            session.add(do)
            session.commit()
            return instance.from_orm(do)

        return self._with_rollback(operation)

    def create_batch(self, instances):
        dos = [instance.to_orm() for instance in instances]

        def operation(session):
            session.bulk_save_objects(dos, return_defaults=True)
            session.commit()
            return [self.en_cls.from_orm(do) for do in dos]

        return self._with_rollback(operation)

    def get(self, id: int):
        """根据ID获取单个记录，返回业务实体或None"""
        do = self._with_rollback(lambda s: s.get(self.do_cls, id))
        return self.en_cls.from_orm(do) if do else None

    def _get_by_condition(self, **kwargs):
        def operation(session):
            query = session.query(self.do_cls)
            for key, value in kwargs.items():
                query = query.filter(getattr(self.do_cls, key) == value)
            orm_obj = query.first()
            if orm_obj is None:
                self._log(f"警告: 根据条件{kwargs}未找到slice")
                return None
            return self.en_cls.from_orm(orm_obj)

        return self._with_rollback(operation)

    def get_all(self, limit: int = None, offset: int = 0):
        """获取所有记录，支持分页"""

        def operation(session):
            query = session.query(self.do_cls)
            if limit is not None:
                query = query.limit(limit)
            if offset > 0:
                query = query.offset(offset)
            orm_objs = query.all()
            return [self.en_cls.from_orm(obj) for obj in orm_objs]

        return self._with_rollback(operation)

    def update(self, instance):
        """更新记录并返回更新后的业务实体"""
        if instance.id is None:
            self._log(f"警告: 记录 ID 缺失，跳过更新: {instance}")
            return instance
        def operation(session):
            do = instance.to_orm()
            merged_obj = session.merge(do)
            session.commit()
            return self.en_cls.from_orm(merged_obj)

        return self._with_rollback(operation)

    def update_batch(self, instances):
        """批量更新记录，返回更新后的业务实体列表"""
        # 先过滤掉异常数据
        valid_instances = [inst for inst in instances if inst.id is not None]
        for inst in instances:
            if inst.id is None:
                self._log(f"警告: 记录 ID 缺失，跳过更新: {inst}")

        def operation(session):
            updated_orms = [
                session.merge(inst.to_orm())
                for inst in valid_instances
            ]
            session.commit()
            return [self.en_cls.from_orm(obj) for obj in updated_orms]

        return self._with_rollback(operation)

    def delete_by_id(self, id: int) -> bool:
        """根据ID删除记录，返回是否删除成功"""
        def operation(session):
            rows_deleted = session.query(self.do_cls).filter_by(id=id).delete()
            session.commit()
            return rows_deleted > 0

        result = self._with_rollback(operation)
        if not result:
            self._log(f"警告: ID为{id}的记录不存在")
        return result

    def delete(self, instance) -> None:
        """删除指定业务实体对应的记录"""
        def operation(session):
            do = instance.to_orm()
            do = session.merge(do)
            session.delete(do)
            session.commit()

        self._with_rollback(operation)

    def _log(self, msg):
        print(f"[{self.do_cls.__tablename__}_repo]: {msg}")

    def _with_rollback(self, func):
        session = self._session_factory()
        try:
            return func(session)
        except Exception as e:
            traceback.print_exc()
            session.rollback()
            raise e  # 让调用者处理异常
        finally:
            session.close()

    def delete_batch(self, ids: List[int]) -> int:
        """批量删除记录，返回实际删除的数量"""
        def operation(session):
            query = session.query(self.do_cls).filter(self.do_cls.id.in_(ids))
            deleted_count = query.delete(synchronize_session=False)
            session.commit()
            return deleted_count

        result = self._with_rollback(operation)
        if result < len(ids):
            self._log(f"部分删除: 请求预期删除{len(ids)}条记录，实际删除{result}条")
        return result


news_repo = Repo(NewsDO, News)
slice_repo = Repo(SliceDO, Slice)
task_repo = Repo(TaskDO, Task)

# 初始化 idl 的数据库连接
db_config = DBConfig()
idl_repo.must_init(idl_repo.DBConfig(
    host=db_config.host,
    port=db_config.port,
    database=db_config.database,
    user=db_config.user,
    password=db_config.password
))

# 从 idl 导入 task_repo
task_repo = idl_repo.task_repo

if __name__ == '__main__':

    # news = News(
    #     task_id=1,
    #     slice_id=1,
    #     third_id='1',
    #     source='1',
    #     author='1',
    #     title='1',
    #     content='1',
    #     heat=1.0,
    #     sentiment=1.0,
    #     publish_time=datetime.now(),
    #     url='http://127.0.0.1:',
    # )
    # news = news_repo.create(news)
    # print(news)

    news_list = [
        News(id = 101,
               task_id=2,
                 slice_id=1000,
                 third_id='1',
                 source='1',
                 author='1',
                 title='1',
                 content='1',
                 heat=1.0,
                 sentiment=1.0,
                 publish_time=datetime.now(),
                 url='http://127.0.0.1:'
                 ),

        News(id = 100,
                task_id=3,
                 slice_id=1000,
                 third_id='1',
                 source='1',
                 author='1',
                 title='1',
                 content='1',
                 heat=1.0,
                 sentiment=1.0,
                 publish_time=datetime.now(),
                 url='http://127.0.0.1:'
                 )
    ]

    news_list = news_repo.update_batch(news_list)
    for n in news_list:
        print(n)

    # news = News(
    #     id=105,
    #     task_id=100,
    #     slice_id=1,
    #     third_id='1',
    #     source='1',
    #     author='1',
    #     title='1',
    #     content='1',
    #     heat=1.0,
    #     sentiment=1.0,
    #     publish_datetime=datetime.now(),
    #     url='http://127.0.0.1:',
    # )




    # record_repo.delete(Record(
    #     id=71
    # ))
