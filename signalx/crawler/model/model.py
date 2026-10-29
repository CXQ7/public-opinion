from snownlp import SnowNLP
from openai import OpenAI
import json
import traceback
import os
import time
from typing import Optional

import requests

from publicity_idl.domain import Slice
from publicity_idl.domain import News
from repository.base import news_repo
from repository.slice_repo import slice_repo
from common.constant import TaskKeyword
from utils import *

DEEPSEEK_API_KEY = '__CONFIGURE_LOCALLY__'
DEEPSEEK_BASE_URL = 'https://api.deepseek.com'


AI_ROUTER_BASE_URL = os.getenv("AI_ROUTER_BASE_URL", "http://127.0.0.1:5000").rstrip("/")
AI_ROUTER_SENTIMENT_URL = f"{AI_ROUTER_BASE_URL}/api/sentiment"

# 如果设置了 AI_ROUTER_AI_INDEX，则爬虫会显式指定模型；
# 否则不传 ai_index，让 AI Router 跟随它的“默认模型”（可被网页端切换）。
_ai_router_ai_index_raw = os.getenv("AI_ROUTER_AI_INDEX")
AI_ROUTER_AI_INDEX: Optional[int]
try:
    AI_ROUTER_AI_INDEX = int(_ai_router_ai_index_raw) if _ai_router_ai_index_raw is not None else None
except Exception:
    AI_ROUTER_AI_INDEX = None

AI_ROUTER_TIMEOUT_SECONDS = float(os.getenv("AI_ROUTER_TIMEOUT_SECONDS", "30"))

class Model:
    def __init__(self, news_arr: list[News], task_keyword: TaskKeyword):
        self.news_arr: list[News] = news_arr
        self.task_keyword: TaskKeyword = task_keyword
        self.task_id: int = task_keyword.task_id
        self.keyword: str = task_keyword.keyword
        self.slice_arr: list[Slice] = []  # 明确指定为Slice对象列表

    def train(self):
        logger.debug(self.news_arr[:10])
        logger.info(f"Analyzing data... (玩命训练中), 共{len(self.news_arr)}条数据")

        if len(self.news_arr) == 0:
            logger.info("数据为空，跳过model训练")
            return

        # 1. 生成情感（优先走本地 AI Router，可切换模型；失败则兜底 DeepSeek 直连）
        if not self._gen_news_sentiment_by_ai_router():
            self._gen_news_sentiment_by_deepseek()

        # 2. 生成切片 (LLM) 并保存
        self._gen_slice()

        # 3. 计算并更新各切片的热度与综合情感
        self._calculate_slice_metrics()

        # 4. 保存新闻记录
        self._save_news()

        logger.info("数据训练 & 入库完毕:")

    def _calculate_slice_metrics(self):
        sentiment_counter = {}
        # 定义新闻热度的初始兜底值
        INITIAL_NEWS_HEAT = 10.0

        for news in self.news_arr:
            s_pos = getattr(news, 'slice_pos', None)
            if s_pos is None: continue

            # 使用新闻本身的 heat 进行累加，如果没有则使用初始值
            news_heat = getattr(news, 'heat', 0)
            if not news_heat or news_heat < 1:
                news_heat = INITIAL_NEWS_HEAT

            self.slice_arr[s_pos].heat += news_heat
            self.slice_arr[s_pos].news_cnt += 1

            sentiment_counter.setdefault(s_pos, {"POSITIVE": 0, "NEGATIVE": 0, "NEUTRAL": 0})
            sentiment_label = news.sentiment or "NEUTRAL"
            if sentiment_label not in sentiment_counter[s_pos]:
                sentiment_label = "NEUTRAL"
            sentiment_counter[s_pos][sentiment_label] += 1

        for idx, s in enumerate(self.slice_arr):
            counts = sentiment_counter.get(idx, {"POSITIVE": 0, "NEGATIVE": 0, "NEUTRAL": 0})
            if counts["POSITIVE"] > counts["NEGATIVE"] and counts["POSITIVE"] > counts["NEUTRAL"]:
                s.sentiment = "POSITIVE"
            elif counts["NEGATIVE"] > counts["POSITIVE"] and counts["NEGATIVE"] > counts["NEUTRAL"]:
                s.sentiment = "NEGATIVE"
            else:
                s.sentiment = "NEUTRAL"

        slice_repo.update_batch(self.slice_arr)

    def _gen_news_sentiment(self):
        try:
            logger.info("snownlp 开始生成情感")
            for item in self.news_arr:
                if item.title:
                    s = SnowNLP(item.title)
                    item.sentiment = self._sentiment_label(s.sentiments)
                else:
                    item.sentiment = "NEUTRAL"
            logger.info("snownlp 情感训练完毕")
        except Exception as e:
            logger.error(f"生成情感失败, {e}")

    def _gen_news_sentiment_by_deepseek(self):
        """
        使用 deepseek-chat 批量分析新闻标题情感，严格返回 POSITIVE/NEGATIVE/NEUTRAL，失败兜底为 NEUTRAL。
        """
        logger.info("进入deepseek情感分析方法，待分析新闻数：%d", len(self.news_arr))
        client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_BASE_URL)
        title_list = [item.title if item.title else "" for item in self.news_arr]
        logger.info("待分析标题列表：%s", title_list)
        title_list_str = json.dumps(title_list, ensure_ascii=False)
        prompt = (
            "你是一个情感分析专家。请对下列新闻标题进行情感分类，只能选择'POSITIVE'（积极）、'NEGATIVE'（消极）、'NEUTRAL'（中性）三种标签之一。"
            "请严格返回如下格式的JSON：{\"results\": [\"POSITIVE\", \"NEGATIVE\", ...]}，results长度与输入标题列表一致。"
            f"输入标题列表：{title_list_str}"
        )
        logger.info("deepseek prompt: %s", prompt)
        success = False
        for attempt in range(3):
            logger.info(f"deepseek情感分析第{attempt+1}次尝试...")
            try:
                response = client.chat.completions.create(
                    model="deepseek-chat",
                    messages=[{"role": "user", "content": prompt}],
                    temperature=0,
                )
                logger.info(f"deepseek API返回: {response.choices[0].message.content}")
                res_obj = try_parse_json(response.choices[0].message.content)
                results = res_obj.get("results", [])
                logger.info(f"deepseek解析结果: {results}")
                if isinstance(results, list) and len(results) == len(self.news_arr):
                    for item, sentiment in zip(self.news_arr, results):
                        if sentiment in ("POSITIVE", "NEGATIVE", "NEUTRAL"):
                            item.sentiment = sentiment
                        else:
                            item.sentiment = "NEUTRAL"
                    logger.info(f"deepseek 情感分析完毕，成功返回 {len(results)} 条")
                    success = True
                    break
                else:
                    logger.warning(f"deepseek 返回格式不佳，重试中 ({attempt+1}/3)，原始结果: {response.choices[0].message.content}")
            except Exception as e:
                logger.error(f"第 {attempt+1} 次尝试 deepseek 情感分析失败: {e}")
        if not success:
            logger.error("deepseek 情感分析失败，全部兜底为 NEUTRAL")
            for item in self.news_arr:
                item.sentiment = "NEUTRAL"

    def _gen_news_sentiment_by_ai_router(self) -> bool:
        """通过 AI Router 服务批量生成情感。

        返回：True 表示成功并已写入 item.sentiment；False 表示失败（调用方可继续兜底）。
        """
        try:
            titles = [item.title if item.title else "" for item in self.news_arr]
            if not titles:
                return True

            payload = {"titles": titles}
            if AI_ROUTER_AI_INDEX is not None:
                payload["ai_index"] = AI_ROUTER_AI_INDEX

            last_error = None
            for attempt in range(3):
                try:
                    logger.info(
                        "AI Router 情感分析第 %d 次尝试: url=%s ai_index=%s",
                        attempt + 1,
                        AI_ROUTER_SENTIMENT_URL,
                        payload.get("ai_index", "(server-default)"),
                    )
                    resp = requests.post(
                        AI_ROUTER_SENTIMENT_URL,
                        json=payload,
                        timeout=AI_ROUTER_TIMEOUT_SECONDS,
                    )
                    if resp.status_code != 200:
                        last_error = f"HTTP {resp.status_code}: {resp.text}"
                        raise RuntimeError(last_error)

                    data = resp.json() if resp.content else {}
                    if not isinstance(data, dict) or not data.get("ok"):
                        last_error = f"响应失败: {data}"
                        raise RuntimeError(last_error)

                    results = data.get("results")
                    if not isinstance(results, list) or len(results) != len(self.news_arr):
                        last_error = f"results 长度不匹配: len(results)={len(results) if isinstance(results, list) else 'N/A'}"
                        raise RuntimeError(last_error)

                    for item, sentiment in zip(self.news_arr, results):
                        if sentiment in ("POSITIVE", "NEGATIVE", "NEUTRAL"):
                            item.sentiment = sentiment
                        else:
                            item.sentiment = "NEUTRAL"

                    logger.info(
                        "AI Router 情感分析成功: provider=%s model=%s count=%d",
                        data.get("provider"),
                        data.get("model"),
                        len(results),
                    )
                    return True
                except Exception as e:
                    last_error = str(e)
                    logger.warning("AI Router 情感分析失败: %s", last_error)
                    time.sleep(0.5)

            logger.error("AI Router 情感分析连续失败，准备兜底: %s", last_error)
            return False
        except Exception as e:
            logger.error("AI Router 情感分析异常，准备兜底: %s", e)
            return False

    def _save_news(self):
        logger.info("保存新闻记录...")
        for news in self.news_arr:
            slice_pos = getattr(news, 'slice_pos', None)
            if slice_pos is not None:
                news.slice_id = self.slice_arr[slice_pos].id
        news_repo.create_batch(self.news_arr)

    def _gen_slice(self):
        logger.info("开始生成切片")
        client = OpenAI(api_key=DEEPSEEK_API_KEY, base_url=DEEPSEEK_BASE_URL)
        title_list_str = json.dumps([item.title for item in self.news_arr], ensure_ascii=False)

        def is_valid(res_obj):
            # 校验返回结构和索引合法性
            if not (isinstance(res_obj, dict) and 'slices' in res_obj and 'newsSliceIds' in res_obj):
                return False
            slices = res_obj['slices']
            newsSliceIds = res_obj['newsSliceIds']
            if not (isinstance(slices, list) and isinstance(newsSliceIds, list)):
                return False
            if len(newsSliceIds) != len(self.news_arr):
                return False
            if not all(isinstance(i, int) and 0 <= i < len(slices) for i in newsSliceIds):
                return False
            return True

        max_attempts = 2
        for attempt in range(max_attempts):
            try:
                response = client.chat.completions.create(
                    model="deepseek-chat",
                    messages=[
                        {"role": "user", "content":
                            "你是一个新闻事件聚合专家。请根据给定的新闻标题列表，将内容相关、描述同一事件或故事线上某个节点的新闻归为同一个“切片”（Slice），每个切片代表一个有故事性的事件。"
                            "切片名称（slices）应简明扼要地描述该事件的核心内容或主题，避免使用“新闻1”“事件A”这类无意义的名称。"
                            "只返回且仅返回一个合法的 JSON 对象，内容包含 'slices'（事件名称列表）和 'newsSliceIds'（每个新闻对应的切片索引）。"
                            "newsSliceIds 的长度必须严格等于输入新闻列表的长度，且每个索引都必须在 slices 数组范围内。"
                            "返回的 JSON schema 如下："
                            '{"slices": [string, ...], "newsSliceIds": [integer, ...]}\n'
                            f"输入标题列表：{title_list_str}"
                        }
                    ],
                    temperature=0,
                )
                res_obj = try_parse_json(response.choices[0].message.content)
                if is_valid(res_obj):
                    # 1. 构造 Slice 对象并持久化以获得 ID
                    self.slice_arr = [
                        Slice(title=name, task_id=self.task_id, heat=0, news_cnt=0)
                        for name in res_obj['slices']
                    ]
                    self.slice_arr = slice_repo.create_batch(self.slice_arr)
                    # 2. 将记录的 slice_pos 指向索引
                    for idx, s_idx in enumerate(res_obj['newsSliceIds']):
                        setattr(self.news_arr[idx], 'slice_pos', s_idx)
                    logger.info(f"生成了 {len(self.slice_arr)} 个切片")
                    return
                else:
                    logger.warning(f"LLM 返回格式或索引不合法，重试中 ({attempt+1}/{max_attempts})")
            except Exception as e:
                logger.error(f"第 {attempt+1} 次尝试生成切片失败: {e}")

        # 兜底方案
        logger.error("切片生成失败，使用兜底逻辑")
        default_slice = Slice(title="未分类切片", task_id=self.task_id, heat=0, news_cnt=0)
        self.slice_arr = [slice_repo.create(default_slice)]
        for news in self.news_arr:
            setattr(news, 'slice_pos', 0)

if __name__ == "__main__":
    news_arr = [
        News(task_id=1, title="特朗普批美国前任政府：#拜登送钱就像送水一样 ，最终什么也没捞到"),
        News(task_id=1, title="特朗普对拜登秋后算账，意外帮了中国大忙，马斯克查办快刀斩乱麻"),
        News(task_id=1, title="特朗普的报复已经开始，他与拜登的大戏在后头")
    ]
    Model(news_arr, TaskKeyword(task_id=1, keyword="示例关键词")).train()
