from typing import Optional

from publicity_idl.domain import SliceDO, Slice
from repository.base import Repo

class SliceRepo(Repo):
    def __init__(self):
        super().__init__(SliceDO, Slice)

    def get_by_title(self, title) -> Optional[Slice]:
        return self._get_by_condition(title=title)


slice_repo = SliceRepo()

if __name__ == '__main__':
    s = slice_repo.get_by_title('巴以冲突停火')
    print(s)
