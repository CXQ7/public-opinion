<script setup>
import { ref } from 'vue';
import {store} from "@/store/store";

const timeOptions = [
  '24小时',
  '今天',
  '昨天',
  '3天',
  '7天',
  '15天',
  '30天',
  '自定义',
];
const sortOptions = ['时间降序', '时间升序', '热度降序', '热度升序', '匹配度'];
const selectedTime = ref('');
const selectedSort = ref('');
const isMerge = ref(false);
const activeNames = ref([]);

const selectTime = (time) => {
  selectedTime.value = time;
};

const toggleMerge = () => {
  isMerge.value =!isMerge.value;
};

const selectSort = (sort) => {
  selectedSort.value = sort;
};
</script>

<template>
  <div class="body-wrapper">
    <div class="body">
      <!-- 搜索输入框 -->
      <el-input
          v-model="store.homePageParams.filter.searchTerm"
          placeholder="输入搜索内容"
          prefix-icon="el-icon-search"
          clearable
          class="search-box"
      ></el-input>

      <!-- 时间范围筛选 -->
      <div class="filter-section">
        <p class="filter-title">时间范围</p>
        <el-tag
            v-for="(time, index) in timeOptions"
            :key="index"
            :type="selectedTime === time ? 'primary' : ''"
            @click="selectTime(time)"
            class="filter-tag"
        >
          {{ time }}
        </el-tag>
      </div>

      <!-- 相关文章合并 -->
      <div class="filter-section">
        <p class="filter-title">相关文章</p>
        <el-tag
            :type="isMerge ? 'primary' : ''"
            @click="toggleMerge"
            class="filter-tag"
        >
          合并文章
        </el-tag>
        <el-tag
            :type="!isMerge ? 'primary' : ''"
            @click="toggleMerge"
            class="filter-tag"
        >
          取消合并
        </el-tag>
      </div>

      <!-- 信息排序 -->
      <div class="filter-section">
        <p class="filter-title">信息排序</p>
        <el-tag
            v-for="(sort, index) in sortOptions"
            :key="index"
            :type="selectedSort === sort ? 'primary' : ''"
            @click="selectSort(sort)"
            class="filter-tag"
        >
          {{ sort }}
        </el-tag>
      </div>

      <!-- 可折叠部分 -->
      <el-collapse v-model="activeNames">
        <el-collapse-item title="情感属性" name="1">
          <div>筛选内容1...</div>
        </el-collapse-item>
        <el-collapse-item title="匹配方式" name="2">
          <div>筛选内容2...</div>
        </el-collapse-item>
        <el-collapse-item title="精准筛选" name="3">
          <div>筛选内容3...</div>
        </el-collapse-item>
        <el-collapse-item title="数据推送" name="4">
          <div>筛选内容4...</div>
        </el-collapse-item>
        <el-collapse-item title="数据导出" name="5">
          <div>筛选内容5...</div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<style scoped>
.body-wrapper{
  display: flex;
  justify-content: center;
  height: 100%;
  width: 100%;
}

.body {
  background-color: #fff;
  /* border: 1px solid #e0e0e0;
  border-radius: 8px; */
  margin-top: 5%;
  width: 90%;
}

.search-box {
  margin-bottom: 20px;
}

.filter-section {
  margin-bottom: 20px;
}

.filter-title {
  margin-bottom: 10px;
  font-size: 14px;
  color: #333;
}

.filter-tag {
  margin-right: 10px;
  margin-bottom: 10px;
  cursor: pointer;
}

.el-collapse-item {
  background-color: #fff;
  border: none;
}

.el-collapse-item__header {
  font-size: 14px;
  font-weight: bold;
}
</style>
