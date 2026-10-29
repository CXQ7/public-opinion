<script setup>
import { Pie } from 'vue-chartjs';
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement } from 'chart.js';
import { ref, onMounted } from 'vue';
import { shallowRef, nextTick } from 'vue';
import '@fortawesome/fontawesome-free/css/all.min.css';
import {store} from "@/store/store";
import BreadcrumbNavigation from "@/components/BreadcrumbNavigation.vue";
import axios from "axios";
import {constant} from "@/store/constant";
import Utils from "@/utils/utils";
import Heat from "@/components/Heat.vue";


const displayData = ref([])
const positiveAvg = ref(0);
const negativeAvg = ref(0);
// 注册 Chart.js 模块
ChartJS.register(Title, Tooltip, Legend, ArcElement);

const selectedSentimentData = ref({
  labels: ["正面", "负面"],
  datasets: [
    {
      data: [0, 0],
      backgroundColor: ["#9acd32", "#ff6347"],
    },
  ],
});

onMounted(() => {
  const sentimentValue = store.sentimentValue ;
  selectedSentimentData.value = {
    labels: ["正面", "负面"],
    datasets: [
      {
         data: [sentimentValue, 1 - sentimentValue],
      },
    ],
  };
});

// const iframeUrl = ref('https://m.weibo.cn/detail/5079905249791688');  // 将 iframe 的 URL 设置为目标网址
</script>

<template>
  <div class="body">
    <!-- Content Section -->
    <div class="content-section">
      <!-- Left Section: Combined Iframe Section -->
      <div class="left-section">
        <iframe :src="Utils.parseHttpUrlToHttps(store.news.url)" class="embedded-iframe"></iframe> <!-- 使用 iframe 直接加载 URL -->
      </div>

      <!-- Right Section: Article Information, Sentiment Analysis, and Tags -->
      <div class="right-section">
        <!-- Article Information -->
        <div class="article-info">
          <h3>文章信息</h3>
          <h2 class="article-title">{{store.news.title}}</h2>
          <p class="article-meta">
            <span><el-icon><Aim /></el-icon> 来源：{{ store.news.source }}</span>
            <span><i class="fas fa-user"></i> 作者：{{ store.news.author }}</span>
            <span><i class="fas fa-clock"></i> 发布时间：{{ store.news.publishDatetime }} </span>
            <span><el-icon><Flag /></el-icon> 政治</span>
            <span><el-icon><Grid /></el-icon> 国际事件</span>
            <a :href="store.news.url"target="_blank"><el-icon><Select /></el-icon> 查看原文</a>
            <span><heat :heat="store.news.heat" :enlarge-ratio="1" /></span> <!-- 添加 heat 组件 -->
          </p>
        </div>

        <!-- Sentiment Analysis -->
        <div class="sentiment-analysis">
          <h3>情感分析</h3>
          <div class="chart">
            <Pie :data="selectedSentimentData" />
          </div>
          <div class="sentiment-summary-container">
            <div class="sentiment-summary positive">{{ (Math.round(selectedSentimentData.datasets[0].data[0] * 100)).toFixed(1) }}% 正面</div>
            <div class="sentiment-summary negative">{{ (Math.round(selectedSentimentData.datasets[0].data[1] * 100)).toFixed(1) }}% 负面</div>

          </div>
        </div>

        <!-- Tags Section -->

      </div>
    </div>
  </div>
</template>

<style scoped>
.body {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-evenly;
  /* padding: 20px; */
  background-color: #f4f6f9;
  font-family: Arial, sans-serif;
  /* gap: 20px; */
}

.content-section {
  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
  height: 96%;
  width: 100%;
}

.left-section {
  width: 62%;
  height: 100%;
}

.right-section {
  display: flex;
  flex-direction: column;
  width: 33%;
}

.embedded-iframe {
  width: 100%; /* 保持宽度为 100% */
  height: 100%; /* 将高度改为 100%，以填充整个父容器 */
  border: none;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.article-info, .sentiment-analysis, .tags-section {
  background-color: white;
  padding: 3%;
  height: 47%;
  margin-bottom: 5%;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.article-title {
  font-size: 20px;
  margin-bottom: 10px;
  color: #333;
}

.article-meta span, .article-meta a {
  display: block;
  margin-top: 5px;
  color: #777;
  font-size: 14px;
}

.sentiment-summary {
  text-align: center;
  margin-top: 10px;
  font-size: 18px;
}

.sentiment-summary-container {
  display: flex;
  gap: 20px;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.sentiment-summary.positive {
  color: #9acd32;
}

.sentiment-summary.negative {
  color: #ff6347;
}

.keyword, .tag {
  display: inline-block;
  background-color: #f0f0f0;
  color: #333;
  padding: 5px 10px;
  border-radius: 15px;
  margin: 5px;
}

.chart {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
</style>
