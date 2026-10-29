<script setup>
import router from "@/router";
import {store} from "@/store/store";
import Heat from "@/components/Heat.vue";

const props = defineProps({
  keyword: {
    type: Object,
    required: true,
  },
});

const goDetail = () => {
  store.keyword = props.keyword
  router.push("EventTimeline")
};

const getSentimentClass = (sentiment) => {
  if (sentiment >= 0.7 && sentiment <= 1) {
    return 'sentiment-positive'; // 正面情感
  } else if (sentiment >= 0 && sentiment < 0.3) {
    return 'sentiment-negative'; // 负面情感
  } else {
    return 'sentiment-neutral'; // 中性情感
  }
};

const getSentimentLabel = (sentiment) => {
  if (sentiment >= 0.7 && sentiment <= 1) {
    return '正面';
  } else if (sentiment >= 0 && sentiment < 0.3) {
    return '负面';
  } else {
    return '中性';
  }
};
</script>

<template>
  <div class="sentiment-item-container" @click="goDetail">
    <el-row :gutter="20">
      <el-card shadow="hover" style="max-width: 100%; min-width: 100%;">
    <!-- 标题 -->
    <div slot="header" class="clearfix" style="font-size: 21px;">
      <div class="header-wrapper">
         <span>{{ keyword.keyword }}</span>
         <Heat :heat="keyword.heat" enlarge-ratio="0.5"></Heat>
      </div>
    </div>

        <!-- 简介 -->
        <div>
          <p style="font-size: 90%; color: gray; opacity: 0.6;">{{ keyword.representativeEvent.description }}</p>
        </div>
        <el-row type="flex" justify="space-between">
          <el-col :span="18">
            <div class="info-row">

           <!-- 涉及词部分 -->
              <span>
                <div style="display: flex; align-items: center;">
                  <el-icon><Aim /></el-icon>&nbsp;
                     <span><strong>涉及词：</strong>{{ keyword.keyword }}</span>
                </div>
              </span>

              <!-- 来源-->
              <span>
                <div style="display: flex; align-items: center;">
                     <el-icon><LocationFilled /></el-icon>&nbsp;
                     <span>起始时间：{{ keyword.createTime }}</span>
                </div>
              </span>

              <!-- 相似文章数-->
              <span>
                <div style="display: flex; align-items: center;">
                  <el-icon><Timer /></el-icon>&nbsp;
                     <span><strong>更新时间：</strong>{{ keyword.representativeEvent.eventDatetime }}</span>
                </div>
              </span>
            </div>
          </el-col>

          <!-- 情感分析 -->
          <div>
            <span
              :class="
                getSentimentClass(keyword.representativeEvent.sentiment)
              "
            >
              {{ getSentimentLabel(keyword.representativeEvent.sentiment) }}
            </span>
            <!-- <button class="custom-button" @click="goDetail(keyword.id)">
              详情
            </button> -->
          </div>
        </el-row>
      </el-card>
    </el-row>
  </div>
</template>

<style scoped>
.sentiment-item-container {
  width: 100%; /* 宽度根据父容器调整 */
  height: auto; /* 高度自适应内容 */
}

.el-card {
  margin-bottom: 10px; /* 为卡片间添加一些间距 */
}
.el-col {
  padding: 0 !important; /* 移除内部默认填充 */
}
.info-row {
  display: flex;
  align-items: center; /* 确保内容在行中垂直居中 */
  font-size: 0.85em;
  color: #757575;
  margin-top: 10px;
}

.info-row span {
  margin-right: 20px; /* 每个元素之间增加20px的间距，可调整 */
}

.info-row span:last-child {
  margin-right: 0; /* 最后一个元素不需要右边距 */
}

/* 自定义情感标签样式 */
.sentiment-positive {
  background-color: #67c23a; /* 绿色表示正面情感 */
  color: white;
  padding: 5px 10px;
  border-radius: 5px;
}

.sentiment-neutral {
  background-color: #e6a23c; /* 黄色表示中性情感 */
  color: white;
  padding: 5px 10px;
  border-radius: 5px;
}

.sentiment-negative {
  background-color: #f56c6c; /* 红色表示负面情感 */
  color: white;
  padding: 5px 10px;
  border-radius: 5px;
}

/* 自定义按钮样式 */
.custom-button {
  background-color: #409eff; /* 蓝色背景 */
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}


.custom-button:hover {
  background-color: #66b1ff; /* 悬停时背景颜色 */
}

.header-wrapper{
  display: flex;
  justify-content: space-between;
  align-items: flex-start; /* 确保元素在上方对齐 */

}




</style>
