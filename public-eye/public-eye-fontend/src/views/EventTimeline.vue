<script setup>
import axios from 'axios'
import {ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {constant} from '@/store/constant'
import { nextTick, onMounted } from 'vue'
import { marked } from 'marked';
import Utils from "@/utils/utils";
import {store} from "@/store/store";
import router from "@/router";

const displayData = ref([])
const drawerVisible = ref(false)
const userInput = ref('')
const titleFilter = ref('')
const contentFilter = ref('')
const dateFilter = ref([])
const loading = ref(false)
const conversation = ref([])
const conversationRef = ref(null) // 对话容器的引用
const analyzedData = ref(null);

const searchByTitle = () => {
  //筛选标题逻辑
}

const searchByContent = () => {
  //筛选内容逻辑
}

const searchByDate = () => {
  //筛选日期逻辑
}

onMounted(() => {
  loading.value = false; // 页面加载时初始化
});

const clearConversation = () => {
  conversation.value = []
}
const analyzeEventChain = async () => {
  if (!store.keyword.id) {
    ElMessage.warning('关键词 ID 不存在')
    return
  }

  const response = await axios.get(`${constant.host}/keyword/analyze/${store.keyword.id}`);
  if (Utils.check_res(response)) {
    // 假设这里有一个 data 对象来保存分析结果
    analyzedData.value = response.data.data;
    console.log('分析事件链成功: ', analyzedData.value);
  } else {
    console.error('分析事件链失败: ', response.data.msg);
  }
}

const goBack = () => {
  router.go(-1)
}

const getData = async () => {
  const response = await axios.get(`${constant.host}/keyword/${store.keyword.id}/events`)
  if (Utils.check_res(response)) {
    displayData.value = response.data.data
  } else {
    console.error('获取事件失败: ', response.data.msg)
  }
}

const submitUserInput = async () => {
  const question = userInput.value.trim();
  if (!question) {
    ElMessage.warning('请输入您的问题');
    return;
  }

  conversation.value.push({ type: 'question', content: question }); // 显示用户问题
  userInput.value = ''; // 清空输入框
  await nextTick();
  scrollToBottom();

  conversation.value.push({ type: 'loading' });
  loading.value = true;
  await nextTick();
  scrollToBottom();

  try {
    const body = {
      keywordId: store.keyword.id,
      question: question,
    };

    const response = await axios.post(`${constant.host}/keyword/analyze/ask`, body);

    if (response.data.code === 1) {
      const fullAnswer = response.data.data;
      conversation.value.pop();
      conversation.value.push({ type: 'answer', content: '', rawContent: '' });
      displayAnswer(fullAnswer);
      ElMessage.success('获取 GPT 回复成功');
    } else {
      ElMessage.error(`获取 GPT 回复失败: ${response.data.message}`);
      conversation.value.pop();
    }
  } catch (error) {
    console.error('调用 GPT 接口出错:', error);
    ElMessage.error('调用 GPT 接口出错');
    conversation.value.pop();
  } finally {
    loading.value = false; // 关闭加载动画
    await nextTick();
    scrollToBottom();
  }
};

// 流式显示回答的函数
const displayAnswer = (fullAnswer) => {
  let currentIndex = 0;
  const answerLength = fullAnswer.length;
  const answerInterval = setInterval(() => {
    if (currentIndex < answerLength) {
      // 逐字增加 Markdown 内容
      conversation.value[conversation.value.length - 1].rawContent += fullAnswer[currentIndex];
      // 使用 marked 解析 Markdown，更新渲染内容
      conversation.value[conversation.value.length - 1].content = marked(conversation.value[conversation.value.length - 1].rawContent);
      currentIndex++;
    } else {
      clearInterval(answerInterval); // 显示完成后清除定时器
    }
  }, 25); // 每 50 毫秒添加一个字符，可以根据需要调整时间间隔
};

// 定义滚动函数
const scrollToBottom = async () => {
  await nextTick(); // 确保 DOM 更新完成
  const container = conversationRef.value;
  if (container) {
    container.scrollTop = container.scrollHeight; // 强制滚动到最底部
  }
};

const goToEventDetail = (event) => {
  store.event = event
  router.push("Detail")
};

const formatDate = (date) => {
  if (!date) return '无日期'
  const options = {year: 'numeric', month: '2-digit', day: '2-digit'}
  return new Date(date).toLocaleDateString('zh-CN', options)
}

getData() //抓取事件
</script>


<template>
  <!-- 右侧抽屉 -->
  <el-drawer
             v-model="drawerVisible"
             title="生成舆情分析报告"
             direction="rtl"
             size="40%">
    <template #title>
      <span>生成舆情分析报告</span>
      <el-button @click="clearConversation" circle size="large" style="display: flex;">
        <el-icon><Delete /></el-icon>
      </el-button>
    </template>
    <div ref="conversationRef" class="conversation">
      <div v-for="(item, index) in conversation" :key="index" :class="item.type">
        <!-- 用户问题 -->
        <div v-if="item.type === 'question'" class="question">{{ item.content }}</div>

        <!-- AI 回答 -->
        <div v-else-if="item.type === 'answer'" class="answer" v-html="item.content"></div>

        <!-- 加载中动画 -->
        <div v-else-if="item.type === 'loading'">
          <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
        </div>
      </div>
    </div>
    <!-- 输入框固定在底部 -->
    <div class="input-area-container">
      <!-- 文件附加按钮 -->
      <div class="filter-group">
        <button aria-label="附加文件"
                class="file-attach-button">
          <svg width="24"
               height="24"
               viewBox="0 0 24 24"
               fill="none"
               xmlns="http://www.w3.org/2000/svg">
            <path fill-rule="evenodd"
                  clip-rule="evenodd"
                  d="M9 7C9 4.23858 11.2386 2 14 2C16.7614 2 19 4.23858 19 7V15C19 18.866 15.866 22 12 22C8.13401 22 5 18.866 5 15V9C5 8.44772 5.44772 8 6 8C6.55228 8 7 8.44772 7 9V15C7 17.7614 9.23858 20 12 20C14.7614 20 17 17.7614 17 15V7C17 5.34315 15.6569 4 14 4C12.3431 4 11 5.34315 11 7V15C11 15.5523 11.4477 16 12 16C12.5523 16 13 15.5523 13 15V9C13 8.44772 13.4477 8 14 8C14.5523 8 15 8.44772 15 9V15C15 16.6569 13.6569 18 12 18C10.3431 18 9 16.6569 9 15V7Z"
                  fill="currentColor"></path>
          </svg>
        </button>
      </div>

      <!-- 可编辑文本输入框 -->
      <div class="input-text-area">
        <input type="text"
               class="text-input"
               v-model="userInput"
               @keydown.enter="submitUserInput"
               placeholder="请输入您的问题并提交。"/>
      </div>

      <!-- 发送按钮 -->
      <div class="filter-group">
        <button aria-label="发送提示"
                class="send-button"
                @click="submitUserInput">
          <svg width="24"
               height="24"
               viewBox="0 0 32 32"
               fill="none"
               xmlns="http://www.w3.org/2000/svg">
            <path fill-rule="evenodd"
                  clip-rule="evenodd"
                  d="M15.1918 8.90615C15.6381 8.45983 16.3618 8.45983 16.8081 8.90615L21.9509 14.049C22.3972 14.4953 22.3972 15.2189 21.9509 15.6652C21.5046 16.1116 20.781 16.1116 20.3347 15.6652L17.1428 12.4734V22.2857C17.1428 22.9169 16.6311 23.4286 15.9999 23.4286C15.3688 23.4286 14.8571 22.9169 14.8571 22.2857V12.4734L11.6652 15.6652C11.2189 16.1116 10.4953 16.1116 10.049 15.6652C9.60265 15.2189 9.60265 14.4953 10.049 14.049L15.1918 8.90615Z"
                  fill="currentColor"></path>
          </svg>
        </button>
      </div>
    </div>
  </el-drawer>

  <div class="main">
    <!-- 主界面事件链框 -->
    <div class="left">
      <el-timeline>
        <el-timeline-item v-for="(item, index) in displayData"
                          :size="large"
                          :color="rgb"
                          :key="index"
                          :timestamp="item.eventDatetime"
                          :class="index % 2 === 0 ? 'left-item' : 'right-item'">
          <el-card :class="index % 2 === 0 ? 'left-card' : 'right-card'" @click="goToEventDetail(item)">
            <h4>{{ item.description }}</h4> <!-- 关键词 --><!-- event.description.split(':')[0] -->
            <p>{{ item.description.split(':')[1] }}</p> <!-- 关键词 + 引申词 -->
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="right">
      <!-- 打开抽屉按钮 -->
      <div class="button-wrapper">
        <el-button class="button"
                   type="primary"
                   @click="analyzeEventChain">分析事件链
        </el-button>
        <el-button class="button"
                   type="primary"
                   @click="goBack">返回
        </el-button>
        <el-button type="primary"
                   @click="drawerVisible = true"
                   class="button">打开输入框
        </el-button>
      </div>
      <el-collapse v-model="activeName"
                   accordion>

        <!-- 标题筛选 -->
        <el-collapse-item title="&nbsp;&nbsp;&nbsp;&nbsp;标题筛选"
                          name="1">
          <div class="filter-group">
            <el-input v-model="titleFilter"
                      placeholder="请输入标题"></el-input>
            <el-button class="filter-button"
                       type="primary"
                       @click="searchByTitle">查询
            </el-button>
          </div>
        </el-collapse-item>

        <!-- 内容筛选 -->
        <el-collapse-item title="&nbsp;&nbsp;&nbsp;&nbsp;内容筛选"
                          name="2">
          <div class="filter-group">
            <el-input v-model="contentFilter"
                      placeholder="请输入内容"></el-input>
            <el-button class="filter-button"
                       type="primary"
                       @click="searchByContent">查询
            </el-button>
          </div>
        </el-collapse-item>

        <!-- 日期筛选 -->
        <el-collapse-item title="&nbsp;&nbsp;&nbsp;&nbsp;日期筛选"
                          name="3">
          <div class="filter-group">
            <el-date-picker v-model="dateFilter"
                            type="daterange"
                            range-separator="到"
                            start-placeholder="开始日期"
                            end-placeholder="结束日期"></el-date-picker>
            <el-button class="filter-button"
                       type="primary"
                       @click="searchByDate">查询
            </el-button>
          </div>
        </el-collapse-item>
      </el-collapse>

      <div v-if="analyzedData" class="analyzedData">
        {{ analyzedData }}
      </div>
    </div>
  </div>

</template>


<style scoped>
.conversation {
  max-height: 85%;
  overflow-y: auto;
}
.question {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 5px;
  margin: 10px 0;
  text-align: left;
}

.answer {
  background-color: #ffffff;
  border-radius: 8px;
  padding: 5px;
  margin: 10px 0;
  text-align: left;
}
.dot {
  animation: blink 1.5s infinite step-start both;
  font-size: 3em;
  color: #888;
  text-align: center;
}
.dot:nth-child(1) { animation-delay: 0s; }
.dot:nth-child(2) { animation-delay: 0.5s; }
.dot:nth-child(3) { animation-delay: 1s; }

@keyframes blink {
  0% { color: #888; }
  50% { color: #ccc; }
  100% { color: #888; }
}


.filter-group {
  display: flex;
  align-items: center; /* 垂直居中 */
}

.filter-group > * {
  margin-right: 10px;
}

.left {
  padding: 20px;
  width: 66%;
  height: 100%;
  overflow-y: auto;
}

.right {
  /* position: fixed; */
  top: 7%;
  right: 0;
  width: 34%; /* 设置合适的宽度 */
  height: 100vh; /* 让筛选器占满整个页面的高度 */
  background-color: #f5f5f5;
  padding: 20px;
  overflow-y: auto;
  z-index: 1000; /* 确保它在最上层 */
}

.button-wrapper {
  display: flex;
  height: 8%;
  width: 100%;
  justify-content: center;
  align-content: center;
}

.button {
  background-color: #3498db;
  border-color: #3498db;
}

.input-area-container {
  display: flex;
  align-items: center;
  background-color: #eeeeee;
  border-radius: 26px;
  padding: 10px;
  gap: 10px;
  width: 100%;
  bottom: 3%;
  left: 0;
  z-index: 100;
  position: absolute;
}

.file-attach-button {
  background-color: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  transition: background-color 0.3s ease;
}

.file-attach-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

/* 输入框样式 */
.input-text-area {
  flex-grow: 1;
}

h4 {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

p {
  font-size: 16px;
  color: #666;
}

.text-input {
  width: 100%;
  padding: 10px;
  border: none;
  outline: none;
  font-size: 16px;
  background-color: transparent;
  color: #333;
}

.send-button {
  background-color: rgba(0, 0, 0, 0.1);
  color: #333;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 40px;
  height: 40px;
  transition: background-color 0.3s ease;
}

.send-button:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.main {
  display: flex;
  height: 100%;
  width: 100%;
  justify-content: center;
  background-image: url('//lf-flow-web-cdn.doubao.com/obj/flow-doubao/doubao_ext/static/image/content-bg-3.ac2e7fc4.png');
  background-size: cover;
  background-position: center;
}

/* 左侧事件链容器 */
.right-align {
  text-align: right; /* 将文本靠右对齐 */
  margin-left: auto; /* 自动将内容推到容器的右边 */
}

/* 左侧事件的样式 */
.left-item {
  text-align: left;
  vertical-align: top;
}

.right-item {
  text-align: right;
  vertical-align: top;
}

.left-card {
  width: 40% !important;
  background-color: #fff;
  margin-right: auto; /* 使右半边为空 */
}

.right-card {
  width: 40% !important;
  background-color: #fdfdfd;
  margin-left: auto;
}

.timeline-container {
  padding: 20px;
}

.timeline-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
}
</style>
