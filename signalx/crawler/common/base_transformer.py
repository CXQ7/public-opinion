from abc import ABC, abstractmethod
from typing import Optional

from publicity_idl.domain import News

class BaseTransformer(ABC):
    """记录转换器基类，子类必须实现 normalize_func 方法"""
    @abstractmethod
    def _normalize_func(self, raw_item) -> dict:
        """子类必须实现的标准化方法"""
        raise NotImplementedError

    def transform(self, raw_item) -> Optional[News]:
        """转换单个原始数据为News对象"""
        try:
            normalized_item = self._normalize_func(raw_item)
            if normalized_item is None:
                return None
            return News.from_dict(normalized_item)
        except Exception as e:
            print(f"数据转换失败: {self.__class__.__name__} {str(e)}")
            return None

    def batch_transform(self, raw_items) -> list[News]:
        """批量转换原始数据为News对象列表"""
        if not raw_items:
            return []
        try:
            return [news for raw_item in raw_items
                    if (news := self.transform(raw_item)) is not None]
        except TypeError:
            # raw_items 可能为 None 或非可迭代对象
            return []
