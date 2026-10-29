from typing import Optional

from publicity_idl.domain import EventDO, Event
from repository.base import Repo

class EventRepo(Repo):
    def __init__(self):
        super().__init__(EventDO, Event)

    def get_by_name(self, name) -> Optional[Event]:
        return self._get_by_condition(name=name)


event_repo = EventRepo()

if __name__ == '__main__':
    e = event_repo.get_by_name('巴以冲突停火')
    print(e)
