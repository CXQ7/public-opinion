import os
import enum
import json
from pathlib import Path
from typing import List, Set

from dataclasses import dataclass

from publicity_idl.domain import Task
from repository.base import task_repo
from publicity_idl.enum import SourceEnum

BASE_DIR = Path(__file__).resolve().parents[1]
LAST_INFO_PATH = str(BASE_DIR / 'config' / 'last_info.json')
SOURCE_SET_PATH = str(BASE_DIR / 'config' / 'source_set.json')

# 退避重试相关
SCHEDULER_INTERVAL = 600  # 爬虫爬完暂停10min
KEYWORD_INTERVAL = 2  # 每个关键词爬完暂停2s
PAGE_INTERVAL = 0  # 每个页面爬完暂停0s

# 代理相关
PROXY_POOL_HOST = "localhost"  # 代理服务器默认主机
PROXY_POOL_PORT = 5555  # 代理服务器端口
PROXY_REQUEST_RETRY = 20  # 代理请求失败重试次数
# PROXY_DEFAULT_TIMEOUT = 5  # 代理请求超时时间
PROXY_DEFAULT_TIMEOUT = 10  # 代理请求超时时间

from utils import *

class SourceEnum(enum.Enum):
    douyin = "douyin"
    toutiao = "toutiao"
    weibo = "weibo"
    wechat = "wechat"
    south_weekend = "south_weekend"
    xiaohongshu = "xiaohongshu"

@dataclass
class TaskKeyword:
    task_id: int
    keyword: str


def load_task_keyword_list() -> List[TaskKeyword]:
    tasks: List[Task] = task_repo.get_all()
    task_keywords: List[TaskKeyword] = []
    for task in tasks:
        if task.status is not None and int(task.status) == 0:
            continue
        if not task.keywords:
            continue
        keywords = task.keywords
        if isinstance(keywords, str):
            try:
                keywords = json.loads(keywords)
            except json.JSONDecodeError:
                keywords = [keywords]
        for keyword in keywords:
            if keyword:
                task_keywords.append(TaskKeyword(task_id=task.id, keyword=keyword))
    return task_keywords

def load_source_set():
    default_sources: Set[SourceEnum] = {
        SourceEnum.toutiao,
        SourceEnum.weibo,
        SourceEnum.south_weekend,
        SourceEnum.xiaohongshu,
    }

    source_set_raw = None
    if os.path.exists(SOURCE_SET_PATH):
        with open(SOURCE_SET_PATH, 'r', encoding='utf-8') as f:
            source_set_raw = json.load(f)
    else:
        # 兜底方案：写一个“占位模板”，避免文件缺失
        os.makedirs(str(BASE_DIR / 'config'), exist_ok=True)
        source_set_raw = {
            SourceEnum.douyin.name: None,
            SourceEnum.toutiao.name: None,
            SourceEnum.weibo.name: None,
            SourceEnum.wechat.name: None,
            SourceEnum.south_weekend.name: None,
            SourceEnum.xiaohongshu.name: None,
        }
        with open(SOURCE_SET_PATH, 'w', encoding='utf-8') as f:
            json.dump(source_set_raw, f, indent=1, ensure_ascii=False)

    # 兼容多种配置格式：
    # 1) ["toutiao", "weibo"] 列表：显式启用
    # 2) {"toutiao": true, "douyin": false}：显式启用/禁用
    # 3) {"xxx": null, ...} 占位模板：视为未显式配置，回退到 default_sources
    if isinstance(source_set_raw, list):
        enabled = set()
        for key in source_set_raw:
            if isinstance(key, str) and key in SourceEnum.__members__:
                enabled.add(SourceEnum[key])
        return enabled or default_sources

    if isinstance(source_set_raw, dict):
        values = list(source_set_raw.values())
        has_bool = any(isinstance(v, bool) for v in values)

        if has_bool:
            enabled = {SourceEnum[k] for k, v in source_set_raw.items() if v is True and k in SourceEnum.__members__}
            return enabled or default_sources

        # 全是 null/占位：认为用户没配置，保持默认安全集合
        if all(v is None for v in values):
            return default_sources

        # 其它情况：按 key 存在即启用（向后兼容）
        enabled = {SourceEnum[k] for k in source_set_raw.keys() if k in SourceEnum.__members__}
        return enabled or default_sources

    return default_sources


# API接口，必须初始化对应属性，实现相关方法
class BaseApi:
    def __init__(self):
        self.adaptor = "implement me"
        self.transformer = "implement me"
        self.source = "implement me"
        self.name = "base api"

    def search(self, keyword: str = "拜登", page: int = 0, page_size: int = 10):
        error = "implement me"
        return error


task_keyword_list: List[TaskKeyword] = load_task_keyword_list()
source_set = load_source_set()
