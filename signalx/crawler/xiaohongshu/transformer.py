from common.base_transformer import BaseTransformer
from common.constant import SourceEnum
from .xhs_utils import id_to_time
from utils import logger

class XHSTransformer(BaseTransformer):
    def _normalize_func(self, raw_item) -> dict:
        if raw_item['model_type'] == 'rec_query':
            logger.debug('XHS transformer: model_type=rec_query, skip')
            return None
        # 尽量从 note_card 中提取信息，失败时回退到 raw_item 的字段
        card = raw_item.get('note_card', {}) if isinstance(raw_item, dict) else {}
        title = card.get('display_title') or ''
        title = title[:32]

        if not title.strip():
            return None

        author = (card.get('user') or {}).get('nickname') or ''
        content = title

        # publish_time 优先使用 id 转时间函数，否则使用可能存在的 publish_time 字段
        publish_time = ''
        try:
            if raw_item.get('id'):
                publish_time = id_to_time(raw_item.get('id'))
            else:
                publish_time = raw_item.get('publish_time', '')
        except Exception:
            publish_time = raw_item.get('publish_time', '')

        interact = card.get('interact_info', {}) if isinstance(card.get('interact_info', {}), dict) else {}
        comment_count = int(interact.get('comment_count', 0) or 0)
        heat = 10 * comment_count

        url = ''
        if card.get('cover') and isinstance(card.get('cover'), dict):
            url = card.get('cover').get('url_default') or ''

        return {
            "source": "xiaohongshu",
            "title": title,
            "author": author,
            "content": content,
            "heat": heat,
            "publish_time": publish_time,
            "url": url
        }
