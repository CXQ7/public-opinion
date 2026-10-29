<script setup>
import { Pie } from 'vue-chartjs';
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement } from 'chart.js';
import { ref } from 'vue';
import '@fortawesome/fontawesome-free/css/all.min.css';
import {store} from "@/store/store";

// 注册 Chart.js 模块
ChartJS.register(Title, Tooltip, Legend, ArcElement);

const sentimentData = {
  labels: ["正面", "中性", "负面"],
  datasets: [
    {
      data: [10, 20, 70],
      backgroundColor: ["#4caf50", "#9e9e9e", "#f44336"],
    },
  ],
};

// const iframeUrl = ref('https://m.weibo.cn/detail/5079905249791688');  // 将 iframe 的 URL 设置为目标网址
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <div class="header">
      <span>数据监测</span> / <span>舆情监测</span> / <span>日常监测</span> / <span>文章详情</span>
    </div>

    <!-- Content Section -->
    <div class="content-section">
      <!-- Left Section: Combined Iframe Section -->
      <div class="left-section">
        <iframe :src="store.event.urls" class="embedded-iframe"></iframe> <!-- 使用 iframe 直接加载 URL -->
      </div>

      <!-- Right Section: Article Information, Sentiment Analysis, and Tags -->
      <div class="right-section">
        <!-- Article Information -->
        <div class="article-info">
          <h3>文章信息</h3>
          <h2 class="article-title">中疾健康 | 如何防治诺如病毒感染？专家详解</h2>
          <p class="article-meta">
            <span><el-icon><Aim /></el-icon> 来源：搜狗号</span>
            <span><i class="fas fa-user"></i> 作者：中新网上海</span>
            <span><i class="fas fa-clock"></i> 11分钟前 </span>
            <span><el-icon><Flag /></el-icon> 健康</span>
            <span><el-icon><Grid /></el-icon> 医疗行业</span>
            <a href="#"><el-icon><Select /></el-icon> 查看原文</a>
          </p>
        </div>

        <!-- Sentiment Analysis -->
        <div class="sentiment-analysis">
          <h3>情感分析</h3>
          <div class="chart">
            <Pie :data="sentimentData" />
          </div>
          <div class="sentiment-summary negative">70.0% 负面</div>
        </div>

        <!-- Tags Section -->
        <div class="tags-section">
          <h3>标签</h3>
          <div class="tags">
            <span class="tag">以后</span>
            <span class="tag">人群</span>
            <span class="tag">症状</span>
            <span class="tag">或者</span>
            <span class="tag">22日</span>
            <span class="tag">消毒</span>
            <span class="tag">做好</span>
            <span class="tag">注意</span>
            <span class="tag">规范</span>
            <span class="tag">3天</span>
            <span class="tag">人员</span>
            <span class="tag">休息</span>
            <span class="tag">感染</span>
            <span class="tag">个人</span>
            <span class="tag">不要</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  padding: 20px;
  background-color: #f4f6f9;
  font-family: Arial, sans-serif;
  gap: 20px;
}

.header {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.content-section {
  display: flex;
  flex-direction: row;
  gap: 20px;
  width: 100%;
}

.left-section {
  width: 65%;
  height: 1000px; /* 可以根据需要调整高度 */
}

.right-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 35%;
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
  padding: 15px;
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

.sentiment-summary.positive {
  color: #4caf50;
}

.sentiment-summary.negative {
  color: #f44336;
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
