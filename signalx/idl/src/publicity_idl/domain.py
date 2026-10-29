import json
from typing import Optional
from datetime import datetime, date, time
from pydantic import BaseModel, Field, ConfigDict, Json

from sqlalchemy import Column, BigInteger, String, Float, Double, DateTime, Text, Time, Date
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.ext.hybrid import hybrid_property
from sqlalchemy.sql import func
from sqlalchemy_serializer import SerializerMixin
from sqlalchemy import UniqueConstraint, Index

from .enum import MysqlTable

do_base = declarative_base()

class Batch(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持从 ORM 模型转换

    id: Optional[int] = None
    total_cnt: Optional[int] = Field(default=0)
    success_cnt: Optional[int] = Field(default=0)
    create_time: Optional[datetime] = Field(default_factory=datetime.now)
    update_time: Optional[datetime] = Field(default_factory=datetime.now)

    @classmethod
    def from_orm(cls, orm_obj: "BatchDO") -> "Batch":
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "BatchDO":
        return BatchDO(
            id=self.id,
            total_cnt=self.total_cnt,
            success_cnt=self.success_cnt,
            create_time=self.create_time,
            update_time=self.update_time,
        )

    @classmethod
    def from_dict(cls, data: dict) -> "Batch":
        """从字典创建Keyword对象"""
        return cls.model_validate(data)

    def __str__(self) -> str:
        return self.model_dump_json()

class BatchDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.Batch.value
    __table_args__ = {'mysql_charset': 'utf8mb4'}

    id = Column(BigInteger, primary_key=True)
    total_cnt = Column(BigInteger, nullable=False)
    success_cnt = Column(BigInteger, nullable=False)
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"


class Keyword(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持从 ORM 模型转换

    id: Optional[int] = None
    keyword: Optional[str] = Field(default=None, max_length=32)
    create_time: Optional[datetime] = Field(default_factory=datetime.now)
    update_time: Optional[datetime] = Field(default_factory=datetime.now)

    @classmethod
    def from_orm(cls, orm_obj: "KeywordDO") -> "Keyword":
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "KeywordDO":
        return KeywordDO(
            id=self.id,
            keyword=self.keyword,
            create_time=self.create_time,
            update_time=self.update_time,
        )

    @classmethod
    def from_dict(cls, data: dict) -> "Keyword":
        """从字典创建Keyword对象"""
        return cls.model_validate(data)

    def __str__(self) -> str:
        return self.model_dump_json()

class KeywordDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.Keyword.value
    __table_args__ = {'mysql_charset': 'utf8mb4'}

    id = Column(BigInteger, primary_key=True)
    keyword = Column(String(32), nullable=False)
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                        onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"


class Event(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持 ORM 转换

    id: Optional[int] = None
    batch_id: Optional[int] = Field(None, comment='批id')
    keyword_id: Optional[int] = Field(None, comment='关键词id')
    name: Optional[str] = Field(None, max_length=128, comment='事件名')
    heat: Optional[float] = Field(0, comment='热度')
    sentiment: Optional[float] = Field(None, comment='情感值')
    create_time: Optional[datetime] = Field(default_factory=datetime.now, comment='创建时间')
    update_time: Optional[datetime] = Field(default_factory=datetime.now, comment='更新时间')

    record_cnt: Optional[int] = Field(default=0, exclude=True)
    @classmethod
    def from_orm(cls, orm_obj: "EventDO") -> "Event":
        """从数据库模型转换（直接使用Pydantic内置解析）"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "EventDO":
        """转换为数据库模型"""
        return EventDO(
            id=self.id,
            batch_id=self.batch_id,
            keyword_id=self.keyword_id,
            name=self.name,
            heat=self.heat,
            sentiment=self.sentiment,
            create_time=self.create_time,
            update_time=self.update_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class EventDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.Event.value
    __table_args__ = (
        UniqueConstraint('batch_id', 'keyword_id', 'name', name='uk_batch_keyword_name'),
        Index('idx_keyword', 'keyword_id'),
        {'mysql_charset': 'utf8mb4'}
    )

    id = Column(BigInteger, primary_key=True)
    batch_id = Column(BigInteger, nullable=False, comment='批id')
    keyword_id = Column(BigInteger, nullable=False, comment='关键词id')
    name = Column(String(128), nullable=False, unique=True, comment='事件名')
    heat = Column(Double, default=0, comment='热度')
    sentiment = Column(Double, comment='情感值')
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                        onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class Record(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持从 ORM 模型转换

    id: Optional[int] = None
    keyword_id: Optional[int] = None
    event_id: Optional[int] = None
    third_id: Optional[str] = Field(default=None, max_length=32)
    source: Optional[str] = Field(default=None, max_length=32)
    author: Optional[str] = Field(default=None, max_length=128)
    title: Optional[str] = Field(default=None, max_length=128)
    content: Optional[str] = Field(default=None, max_length=1024)
    heat: Optional[float] = Field(default=0)
    sentiment: Optional[float] = None
    publish_time: Optional[datetime] = None
    url: Optional[str] = Field(default=None, max_length=512)
    ext: Optional[dict] = None
    create_time: Optional[datetime] = Field(default_factory=datetime.now)
    update_time: Optional[datetime] = Field(default_factory=datetime.now)

    event_pos: Optional[int] = Field(default=None, exclude=True)

    @classmethod
    def from_orm(cls, orm_obj: "RecordDO") -> "Record":
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "RecordDO":
        return RecordDO(
            id=self.id,
            keyword_id=self.keyword_id,
            event_id=self.event_id,
            third_id=self.third_id,
            source=self.source,
            author=self.author,
            title=self.title,
            content=self.content,
            heat=self.heat,
            sentiment=self.sentiment,
            publish_time=self.publish_time,
            url=self.url,
            ext=json.dumps(self.ext),
            create_time=self.create_time,
            update_time=self.update_time,
        )

    @classmethod
    def from_dict(cls, data: dict) -> "Record":
        """从字典创建Record对象"""
        return cls.model_validate(data)

    def __str__(self) -> str:
        return self.model_dump_json()

class RecordDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.Record.value
    __table_args__ = {
        Index('idx_keyword_source', 'keyword_id', 'source'),
        Index('idx_event', 'event_id'),
        {'mysql_charset': 'utf8mb4'}
    }
    __allow_unmapped__ = True  # 允许业务定义未持久化的拓展字段

    id = Column(BigInteger, primary_key=True)
    keyword_id = Column(BigInteger, nullable=False, comment='关键词id')
    event_id = Column(BigInteger, nullable=False, comment='事件id')

    third_id = Column(String(32), nullable=False, comment='第三方id')
    source = Column(String(32), nullable=False, comment='渠道')
    author = Column(String(128), nullable=False, comment='作者')
    title = Column(String(128), nullable=False, comment='标题')
    content = Column(String(1024), comment='内容')
    heat = Column(Double, default=0, comment='热度')
    sentiment = Column(Double, comment='情感值')
    publish_time = Column(DateTime, nullable=False, comment='发布时间')
    url = Column(String(512), nullable=False, comment='链接')
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='更新时间')

    # 原始字段，存储JSON字符串
    _ext = Column('ext', Json, comment='拓展字段')

    # 业务字段，不持久化
    event_pos: Optional[int] = None

    @hybrid_property
    def ext(self):
        """获取ext字段时会自动将JSON字符串转为字典"""
        if self._ext is None:
            return {}
        try:
            return json.loads(self._ext)
        except (json.JSONDecodeError, TypeError):
            return {}

    @ext.setter
    def ext(self, value):
        """设置ext字段时会自动将字典转为JSON字符串"""
        if value is None:
            self._ext = None
        elif isinstance(value, str):
            # 如果是字符串，直接存储（假设已经是JSON字符串）
            self._ext = value
        else:
            # 如果是字典或其他可序列化对象，转为JSON
            self._ext = json.dumps(value, ensure_ascii=False)

    def to_dict(self):
        """重写SerializerMixin的to_dict方法，确保ext返回字典"""
        result = super().to_dict()
        if '_ext' in result:
            result['ext'] = self.ext  # 使用property返回字典
            del result['_ext']
        return result

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class ErrorLog(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持 ORM 转换

    id: Optional[int] = None
    batch_id: Optional[int] = Field(None, comment='批id')
    source: Optional[str] = Field(None, max_length=32, comment='渠道')
    err_code: Optional[int] = Field(None, comment='错误码')
    err_msg: Optional[str] = Field(None, max_length=32, comment='错误信息')
    ext: Optional[dict] = Field(None, comment='拓展字段')
    create_time: Optional[datetime] = Field(default_factory=datetime.now, comment='创建时间')
    update_time: Optional[datetime] = Field(default_factory=datetime.now, comment='更新时间')

    @classmethod
    def from_orm(cls, orm_obj: "ErrorLogDO") -> "ErrorLog":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "ErrorLogDO":
        """转换为数据库模型"""
        return ErrorLogDO(
            id=self.id,
            batch_id=self.batch_id,
            source=self.source,
            err_code=self.err_code,
            err_msg=self.err_msg,
            ext=json.dumps(self.ext),
            create_time=self.create_time,
            update_time=self.update_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class ErrorLogDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.ErrorLog.value
    __table_args__ = {
        Index('idx_batch_source', 'batch_id', 'source'),
        Index('idx_source', 'source'),
        Index('idx_err_code', 'err_code'),
        {'mysql_charset': 'utf8mb4', 'comment': '错误记录表'}}

    id = Column(BigInteger, primary_key=True)
    batch_id = Column(BigInteger, nullable=False, comment='批id')
    source = Column(String(32), nullable=False, comment='渠道')
    err_code = Column(BigInteger, nullable=False, comment='错误码')
    err_msg = Column(String(32), nullable=False, comment='错误信息')
    _ext = Column('ext', Text, comment='拓展字段')
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"


class SourceKeywordOffset(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持ORM转换

    id: Optional[int] = None
    keyword_id: Optional[int] = Field(None, comment='关键词id')
    source: Optional[str] = Field(None, max_length=32, comment='渠道')
    sync_time: Optional[datetime] = Field(default=datetime.min, comment='同步时间（爬取的新闻列表中最晚的一条记录）')
    create_time: Optional[datetime] = Field(default=datetime.min, comment='创建时间')
    update_time: Optional[datetime] = None

    @classmethod
    def from_orm(cls, orm_obj: "SourceKeywordOffsetDO") -> "SourceKeywordOffset":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "SourceKeywordOffsetDO":
        """转换为数据库模型"""
        return SourceKeywordOffsetDO(
            id=self.id,
            keyword_id=self.keyword_id,
            source=self.source,
            sync_time=self.sync_time,
            create_time=self.create_time,
            update_time=self.update_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class SourceKeywordOffsetDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.SourceKeywordOffset.value
    __table_args__ = {
        UniqueConstraint('keyword_id', 'source', name='uk_keyword_source'),
        {'mysql_charset': 'utf8mb4'}
    }

    id = Column(BigInteger, primary_key=True)
    keyword_id = Column(BigInteger, nullable=False, comment='关键词id')
    source = Column(String(32), nullable=False, comment='渠道')
    sync_time = Column(DateTime, nullable=False, server_default=func.now(), comment='同步时间（爬取的新闻列表中最晚的一条记录）')
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                        onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class PushConfig(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持 ORM 转换

    id: Optional[int] = None
    user_id: Optional[int] = None
    name: Optional[str] = None
    start_time: Optional[time] = None
    end_time: Optional[time] = None
    push_frequency: Optional[int] = None
    expire_date: Optional[date] = None
    create_time: Optional[datetime] = None
    update_time: Optional[datetime] = None

    @classmethod
    def from_orm(cls, orm_obj: "PushConfigDO") -> "PushConfig":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "PushConfigDO":
        """转换为数据库模型"""
        return PushConfigDO(
            id=self.id,
            user_id=self.user_id,
            name=self.name,
            start_time=self.start_time,
            end_time=self.end_time,
            push_frequency=self.push_frequency,
            expire_date=self.expire_date,
            create_time=self.create_time,
            update_time=self.update_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class PushConfigDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.PushConfig.value
    __table_args__ = {
        UniqueConstraint('user_id', 'name', name='uk_user_name'),
        {'mysql_charset': 'utf8mb4', 'comment': '报告推送配置表'}
    }

    id = Column(BigInteger, primary_key=True, comment='主键ID')
    user_id = Column(BigInteger, nullable=False, comment='用户ID')
    name = Column(String(32), nullable=False, comment='配置名称')
    start_time = Column(Time, nullable=False, comment='一天中推送开始的时间')
    end_time = Column(Time, nullable=False, comment='一天中推送结束的时间')
    push_frequency = Column(BigInteger, nullable=True, comment='推送频率（分钟）')
    expire_date = Column(Date, nullable=False, comment='过期时间')
    create_time = Column(DateTime, default=func.now(), nullable=True, comment='创建时间')
    update_time = Column(DateTime, default=func.now(), onupdate=func.now(), nullable=True,
                         comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class PushConfigKeyword(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持 ORM 转换

    id: Optional[int] = None
    push_config_id: Optional[int] = None
    keyword_id: Optional[int] = None
    create_time: Optional[datetime] = None
    update_time: Optional[datetime] = None

    @classmethod
    def from_orm(cls, orm_obj: "PushConfigKeywordDO") -> "PushConfigKeyword":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "PushConfigKeywordDO":
        """转换为数据库模型"""
        return PushConfigKeywordDO(
            id=self.id,
            config_id=self.config_id,
            keyword_id=self.keyword_id,
            create_time=self.create_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class PushConfigKeywordDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.PushConfigKeyword.value
    __table_args__ = {
        UniqueConstraint('push_config_id', 'keyword_id', name='uk_config_keyword'),
        {'mysql_charset': 'utf8mb4'}
    }

    id = Column(BigInteger, primary_key=True)
    push_config_id = Column(BigInteger, nullable=False)
    keyword_id = Column(BigInteger, nullable=False)
    create_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class User(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持 ORM 转换

    id: Optional[int] = None
    username: Optional[str] = None
    password: Optional[str] = None
    phone: Optional[str] = None
    avatar_url: Optional[str] = None
    create_time: Optional[datetime] = None
    update_time: Optional[datetime] = None
    status: Optional[int] = None

    @classmethod
    def from_orm(cls, orm_obj: "UserDO") -> "User":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "UserDO":
        """转换为数据库模型"""
        return UserDO(
            id=self.id,
            username=self.username,
            password=self.password,
            phone=self.phone,
            avatar_url=self.avatar_url,
            create_time=self.create_time,
            update_time=self.update_time,
            status=self.status
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class UserDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.User.value
    __table_args__ = {'mysql_charset': 'utf8mb4', 'comment': '用户表'}

    id = Column(BigInteger, primary_key=True, comment='主键ID')
    username = Column(String(50), nullable=False, comment='用户名')
    password = Column(String(128), nullable=False, comment='密码')
    phone = Column(String(20), nullable=False, comment='电话号码')
    avatar_url = Column(String(256), nullable=True, default=None, comment='头像链接')
    create_time = Column(DateTime, default=func.now(), nullable=True, comment='创建时间')
    update_time = Column(DateTime, default=func.now(), onupdate=func.now(), nullable=True,
                         comment='更新时间')
    status = Column(BigInteger, nullable=False, comment='状态')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class ProxyUsage(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持从 ORM 模型转换

    id: Optional[int] = None
    source: Optional[str] = Field(None, max_length=32, comment='渠道')
    ip: Optional[str] = Field(None, max_length=64, comment='代理ip')
    success_cnt: Optional[int] = Field(default=0, comment='成功获取次数')
    total_cnt: Optional[int] = Field(default=0, comment='总获取次数')
    create_time: Optional[datetime] = Field(default_factory=datetime.now, comment='代理创建时间')
    update_time: Optional[datetime] = Field(default_factory=datetime.now, comment='代理更新时间')

    # 非持久化字段
    success_rate: Optional[float] = Field(default=0.0, exclude=True, comment='成功率')

    @classmethod
    def from_orm(cls, orm_obj: "ProxyUsageDO") -> "ProxyUsage":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "ProxyUsageDO":
        """转换为数据库模型"""
        return ProxyUsageDO(
            id=self.id,
            source=self.source,
            ip=self.ip,
            success_cnt=self.success_cnt,
            total_cnt=self.total_cnt,
            create_time=self.create_time,
            update_time=self.update_time,
        )

    @classmethod
    def from_dict(cls, data: dict) -> "ProxyUsage":
        """从字典创建ProxyUsage对象"""
        return cls.model_validate(data)

    def calculate_success_rate(self) -> float:
        """计算成功率"""
        if self.total_cnt and self.total_cnt > 0:
            return round((self.success_cnt or 0) / self.total_cnt * 100, 2)
        return 0.0


    def __str__(self) -> str:
        return self.model_dump_json()


class ProxyUsageDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.ProxyUsage.value
    __table_args__ = (
        UniqueConstraint('source', 'ip', name='uk_source_ip'),
        {'mysql_charset': 'utf8mb4', 'comment': '代理使用情况表'}
    )
    __allow_unmapped__ = True  # 允许业务定义未持久化的拓展字段

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    source = Column(String(32), nullable=False, comment='渠道')
    ip = Column(String(64), nullable=False, comment='代理ip')
    success_cnt = Column(BigInteger, default=0, comment='成功获取次数')
    total_cnt = Column(BigInteger, default=0, comment='总获取次数')
    create_time = Column(DateTime, server_default=func.now(), comment='代理创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                        onupdate=func.now(), comment='代理更新时间')

    # 业务字段，不持久化
    success_rate: Optional[float] = None

    def calculate_success_rate(self) -> float:
        """计算成功率"""
        if self.total_cnt and self.total_cnt > 0:
            return round(self.success_cnt / self.total_cnt * 100, 2)
        return 0.0

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class KeywordReport(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持从 ORM 模型转换

    id: Optional[int] = None
    keyword_id: Optional[int] = Field(None, comment='关键词ID')
    start_time: Optional[datetime] = Field(None, comment='覆盖时段开始')
    end_time: Optional[datetime] = Field(None, comment='覆盖时段结束')
    event_id_list: Optional[str] = Field(None, max_length=200, comment='事件ID集合')
    trend_data: Optional[dict] = Field(None, comment='舆情走势图数据')
    source_distribution: Optional[dict] = Field(None, comment='信息来源分布数据')
    word_cloud_data: Optional[dict] = Field(None, comment='词云图数据')
    news_summary: Optional[str] = Field(None, max_length=100, comment='主要新闻内容概括(100字内)')
    media_analysis: Optional[str] = Field(None, max_length=200, comment='媒体新闻分析(200字内)')
    create_time: Optional[datetime] = Field(default_factory=datetime.now, comment='创建时间')
    update_time: Optional[datetime] = Field(default_factory=datetime.now, comment='更新时间')

    @classmethod
    def from_orm(cls, orm_obj: "KeywordReportDO") -> "KeywordReport":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "KeywordReportDO":
        """转换为数据库模型"""
        return KeywordReportDO(
            id=self.id,
            keyword_id=self.keyword_id,
            start_time=self.start_time,
            end_time=self.end_time,
            event_id_list=self.event_id_list,
            trend_data=self.trend_data,
            source_distribution=self.source_distribution,
            word_cloud_data=self.word_cloud_data,
            news_summary=self.news_summary,
            media_analysis=self.media_analysis,
            create_time=self.create_time,
            update_time=self.update_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class KeywordReportDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.KeywordReport.value
    __table_args__ = {
        Index('idx_keyword_id', 'keyword_id'),
        {'mysql_charset': 'utf8mb4', 'comment': '关键词报表表'}
    }

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    keyword_id = Column(BigInteger, nullable=False, comment='关键词ID')
    start_time = Column(DateTime, nullable=False, comment='覆盖时段开始')
    end_time = Column(DateTime, nullable=False, comment='覆盖时段结束')
    event_id_list = Column(String(200), comment='事件ID集合')
    trend_data = Column(Json, comment='舆情走势图数据')
    source_distribution = Column(Json, comment='信息来源分布数据')
    word_cloud_data = Column(Json, comment='词云图数据')
    news_summary = Column(String(100), comment='主要新闻内容概括(100字内)')
    media_analysis = Column(String(200), comment='媒体新闻分析(200字内)')
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"

class Report(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # 支持 ORM 转换

    id: Optional[int] = None
    user_id: Optional[int] = Field(None, comment='用户ID')
    config_id: Optional[int] = Field(None, comment='配置ID')
    report_time: Optional[datetime] = Field(None, comment='报表时间')
    image_oss_path: Optional[str] = Field(None, max_length=512, comment='图片集合OSS路径')
    report_text: Optional[dict] = Field(None, comment='报告文字内容(JSON格式)')
    report_oss_path: Optional[str] = Field(None, max_length=512, comment='报告OSS路径')
    push_status: Optional[int] = Field(0, comment='推送状态(0-未推送,1-已推送)')
    create_time: Optional[datetime] = Field(default_factory=datetime.now, comment='创建时间')
    update_time: Optional[datetime] = Field(default_factory=datetime.now, comment='更新时间')

    @classmethod
    def from_orm(cls, orm_obj: "ReportDO") -> "Report":
        """从数据库模型转换"""
        return cls.model_validate(orm_obj.to_dict())

    def to_orm(self) -> "ReportDO":
        """转换为数据库模型"""
        return ReportDO(
            id=self.id,
            user_id=self.user_id,
            config_id=self.config_id,
            report_time=self.report_time,
            image_oss_path=self.image_oss_path,
            report_text=self.report_text,
            report_oss_path=self.report_oss_path,
            push_status=self.push_status,
            create_time=self.create_time,
            update_time=self.update_time
        )

    def __str__(self) -> str:
        return self.model_dump_json()

class ReportDO(do_base, SerializerMixin):
    __tablename__ = MysqlTable.Report.value
    __table_args__ = {
        Index('idx_user_id', 'user_id'),
        Index('idx_config_id', 'config_id'),
        {'mysql_charset': 'utf8mb4', 'comment': '用户报表表'}
    }

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False, comment='用户ID')
    config_id = Column(BigInteger, nullable=False, comment='配置ID')
    report_time = Column(DateTime, nullable=False, comment='报表时间')
    image_oss_path = Column(String(512), comment='图片集合OSS路径')
    report_text = Column(Json, comment='报告文字内容(JSON格式)')
    report_oss_path = Column(String(512), comment='报告OSS路径')
    push_status = Column(BigInteger, default=0, comment='推送状态(0-未推送,1-已推送)')
    create_time = Column(DateTime, server_default=func.now(), comment='创建时间')
    update_time = Column(DateTime, server_default=func.now(),
                         onupdate=func.now(), comment='更新时间')

    def __str__(self):
        return f"<{self.__tablename__} {self.to_dict()}>"
