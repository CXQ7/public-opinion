<script setup>
import axios from 'axios'
import {ref} from 'vue'
import {ElMessage} from 'element-plus'
import {constant} from '@/store/constant'
import { shallowRef,onMounted, nextTick } from 'vue';
import { marked } from 'marked';
import Utils from "@/utils/utils";
import {store} from "@/store/store";
import router from "@/router";
import Heat from "@/components/Heat.vue";
import { Pie } from 'vue-chartjs';
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement } from 'chart.js';

const displayData = ref([])
const drawerVisible = ref(false)
const userInput = ref('')
const loading = ref(false)
const conversation = ref([])
const conversationRef = ref(null) // 对话容器的引用
const tempData = ref("");
const loading1 = ref(false)
const isAnalyzing = ref(false);
const positiveAvg = ref(0);
const neutralAvg = ref(0);
const negativeAvg = ref(0);

ChartJS.register(Title, Tooltip, Legend, ArcElement);


const sentimentData = shallowRef({
  labels: ["正面", "中性", "负面"],
  datasets: [
    {
      data: [0, 0, 0],
      backgroundColor: [" #9acd32", "#4682b4", "#ff6347"],
    },
  ],
});
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
  loading1.value = true;
  isAnalyzing.value = true;
  try {
    const response = await axios.get(`${constant.host}/user/keyword/analyze/${store.keyword.id}`);
    console.log(JSON.stringify(response))
    if (Utils.check_res(response)) {
      tempData.value = response.data.data; // 将完整结果存储到临时变量
      store.keyword.analysis = ""; // 清空显示的分析内容
      displayEventChain(tempData.value); // 调用流式输出方法
      console.log('分析事件链成功: ', tempData.value);
    } else {
      console.error('分析事件链失败: ', response.data.msg);
      ElMessage.error('事件链分析失败');
    }
  } catch (error) {
    console.error('分析事件链出错:', error);
    ElMessage.error('分析事件链出错');
  } finally {
    loading1.value = false;
  }
}

const goBack = () => {
  router.go(-1)
}

const getData = async () => {
  const response = await axios.get(`${constant.host}/user/keyword/${store.keyword.id}/events`)
  if (Utils.check_res(response)) {
    displayData.value = response.data.data
    const sentimentScores = {
      positiveCount: 0,
      neutralCount: 0,
      negativeCount: 0
    };

    displayData.value.forEach(event => {
      const sentimentValue = event.sentiment;
      if (sentimentValue >= 0.7) {
        sentimentScores.positiveCount++;
      } else if (sentimentValue >= 0.3) {
        sentimentScores.neutralCount++;
      } else {
        sentimentScores.negativeCount++;
      }
    });
    const total = sentimentScores.positiveCount + sentimentScores.neutralCount + sentimentScores.negativeCount;
    positiveAvg.value = sentimentScores.positiveCount ? (sentimentScores.positiveCount / total) : 0;
    neutralAvg.value = sentimentScores.neutralCount ? (sentimentScores.neutralCount / total) : 0;
    negativeAvg.value = sentimentScores.negativeCount ? (sentimentScores.negativeCount / total) : 0;

    sentimentData.value = {
      labels: ["正面", "中性", "负面"],
      datasets: [
        {
          data: [positiveAvg.value.toFixed(2), neutralAvg.value.toFixed(2), negativeAvg.value.toFixed(2)],
        },
      ],
    };
    await nextTick();

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

    const response = await axios.post(`${constant.host}/user/keyword/analyze/ask`, body);

    if (Utils.check_res(response)) {
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

const displayEventChain = (fullContent) => {
  let currentIndex = 0;
  const contentLength = fullContent.length;
  const interval = setInterval(() => {
    if (currentIndex < contentLength) {
      store.keyword.analysis += fullContent[currentIndex]; // 逐字增加到 store.keyword.analysis
      currentIndex++;
    } else {
      clearInterval(interval); // 显示完成后清除定时器
    }
  }, 25); // 每 25 毫秒添加一个字符
};
// 定义滚动函数
const scrollToBottom = async () => {
  await nextTick(); // 确保 DOM 更新完成
  const container = conversationRef.value;
  if (container) {
    container.scrollTop = container.scrollHeight; // 强制滚动到最底部
  }
};

const goToNewsList = (event) => {
  store.event = event
  router.push("NewsList")
};

const formatDate = (date) => {
  if (!date) return '无日期'
  const options = {year: 'numeric', month: '2-digit', day: '2-digit'}
  return new Date(date).toLocaleDateString('zh-CN', options)
}

getData();
</script>


<template>
  <!-- 右侧抽屉 -->
  <el-drawer
      v-model="drawerVisible"
      title="对事件链提问"
      direction="rtl"
      size="40%">
    <template #title>
      <span>对事件链提问</span>
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

  <div class="body">
    <!-- 主界面事件链框 -->

    <div class="left">
      <div class="header">【{{ store.keyword.keyword }}】——  舆情时间轴</div>
      <el-timeline>
        <el-timeline-item v-for="(item, index) in displayData"
                          :size="large"
                          :color="rgb"
                          :key="index"
                          :timestamp="item.eventDatetime"
                          :class="index % 2 === 0 ? 'left-item' : 'right-item'">
          <el-card :class="index % 2 === 0 ? 'left-card' : 'right-card'" @click="goToNewsList(item)">
            <h4>{{ item.description }}</h4> <!-- 关键词 --><!-- event.description.split(':')[0] -->
            <Heat :heat="item.heat"></Heat>
            <!--            <p>{{ item.description.split(':')[1] }}</p> &lt;!&ndash; 关键词 + 引申词 &ndash;&gt;-->
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="right">
      <!-- 打开抽屉按钮 -->
      <div class="right-first">
        <el-button class="button"
                   type="primary"
                   @click="goBack">返回
        </el-button>
        <el-button class="button"
                   v-show="Utils.isNull(store.keyword.analysis)"
                   type="primary"
                   :disabled="isAnalyzing"
                   @click="analyzeEventChain">分析事件链
        </el-button>
        <el-button type="primary"
                   v-show="!Utils.isNull(store.keyword.analysis)"
                   @click="drawerVisible = true"
                   class="button">对事件链提问
        </el-button>
      </div>
      <div class="right-second">
        <div v-if="loading1" >
            <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
        </div>
        <div v-else-if="store.keyword.analysis">
          <div v-html="store.keyword.analysis"></div>
        </div>
        <div v-else>
          <p>点击“分析事件链”按钮以生成事件链结果。</p>
        </div>
      </div>

      <!-- 情感分析结果 -->
      <div class="right-third">
        <div class="chart-title">情感分析</div>
        <div class="chart">
          <Pie :data="sentimentData" />
          <div class="sentiment-summary-container">
            <div class="sentiment-summary positive">{{ (Math.round(positiveAvg * 1000) / 10 ).toFixed(1) }}% 正面</div>
            <div class="sentiment-summary neutral">{{ (Math.round(neutralAvg * 1000) / 10 ).toFixed(1)}}% 中性 </div>
            <div class="sentiment-summary negative">{{ (Math.round(negativeAvg * 1000) / 10).toFixed(1) }}% 负面</div>
          </div>
      </div>
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

.left {
  padding: 20px;
  width: 66%;
  height: 100%;
  overflow-y: auto;
}

.right {
  /* position: fixed; */
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  top: 0%;
  right: 0;
  width: 34%; /* 设置合适的宽度 */
  height: 100%;
  background-color: #f5f5f5;
  padding: 20px;
  overflow-y: auto;
  z-index: 1000; /* 确保它在最上层 */
}

.header{
  display: flex;
  font-size: 4vh;
  font-weight: 500;
  line-height: 200%;
  margin-left: 3%;
  border-bottom: 1px solid rgba(0, 0, 0, 0.15);
  margin-bottom: 3%;
}

.right-first {
  display: flex;
  height: 5%;
  width: 100%;
  justify-content: center;
  align-content: center;
  margin-top: 0; /* 确保按钮区域紧贴顶部 */
  align-self: flex-start; /* 将按钮区域定位到父容器顶部 */
}
.right-second {
  display: block;
  flex-direction: column; /* 子元素垂直排列 */
  justify-content: flex-start; /* 将内容对齐到顶部 */
  align-items: center; /* 居中对齐内容 */
  text-align: left;
  height: 45%;
  width: 100%;
  background-color: #ffffff;
  overflow-y: auto; /* 使内容可滚动 */
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
.right-third {
  width: 100%;
  height: 45%;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
.chart-title {
  display: flex;
  height: 15%;
  width: 100%;
  margin-left: 5%;
  align-items: flex-end;
  font-size: 20px;
  align-self: flex-start; /* 将 h3 元素左对齐 */
}

.chart {
  max-width: 300px; /* 设置图表的最大宽度 */
  max-height: 300px; /* 设置图表的最大高度 */
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 70%;
  width: 100%;
  /* //height: 200px; */
/* //width: 100%; */
  margin-top: 0;
}

.sentiment-summary {
  display: flex;
  height: 10%;
  width: 100%;
  justify-content: center;
  align-items: center;
  text-align: center;
  font-size: 18px;
}

.sentiment-summary-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.sentiment-summary.positive {
  color: #9acd32;
}

.sentiment-summary.neutral {
  color: #4682b4;
}

.sentiment-summary.negative {
  color: #ff6347;
}

.right-fourth {
  background-color: #f5f5f5;
  height: 5%;
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

.body {
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
