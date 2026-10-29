<script setup>
import axios from "axios";
import FilterPanel from "@/components/FilterPanel.vue";
import SearchFilterBar from "@/components/SearchFilterBar.vue";
import SentimentItems from "@/components/SentimentItems.vue";
import {constant} from "@/store/constant";
import {ref, watch} from "vue";
import {store} from "@/store/store";
import Utils from "@/utils/utils";

const displayData = ref([]);
const state = ref({
  page: 1,
  pageSize: 10,
  total: 0,
  searchTerm: store.homePageParams.filter.searchTerm,
});

watch(() => store.homePageParams.filter.searchTerm, (newValue) => {
  state.value.searchTerm = newValue
  getData()
}, {deep: true});

const getData = async () => {
  const response = await axios.get(`${constant.host}/keyword/list`, {
    params: state.value,
  });
  if (Utils.check_res(response)) {
    displayData.value = response.data.data.records;
    state.value.total = response.data.data.total;
  } else {
    console.error("获取数据失败: ", response.data.msg);
  }
};

const handleSizeChange = (size) => {
  state.value.pageSize = size;
  getData();
};

const handleCurrentChange = (page) => {
  state.value.page = page;
  getData();
};

// OnCreated
getData();
</script>

<template>
  <div class="body">
    <div class="main-content">
      <el-scrollbar class="content-scroll">
        <div class="content-wrapper">
          <el-row :gutter="0">
            <!-- 过滤面板 -->
            <div class="filter-panel">
              <filter-panel></filter-panel>
            </div>

            <!-- 内容区域 -->
            <el-col :span="24" class="content">
              <el-row>
                <el-col
                    v-for="keyword in displayData"
                    :key="keyword.id"
                    :span="24"
                >
                  <sentiment-items :keyword="keyword"></sentiment-items>
                </el-col>
              </el-row>
            </el-col>
          </el-row>
        </div>
        <!-- 分页组件 -->
        <div class="pagination-wrapper">
          <el-pagination
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              :current-page="state.page"
              :page-sizes="[10, 20, 30, 40]"
              :page-size="state.pageSize"
              layout="total, sizes, prev, pager, next"
              :total="state.total"
              style="text-align: center; margin-top: 20px"
          >
          </el-pagination>
        </div>
      </el-scrollbar>
      <br/>
    </div>
    <!-- 右侧搜索栏 -->
    <div class="right-sidebar" style="z-index: 999;">
      <search-filter-bar></search-filter-bar>
    </div>
  </div>
</template>

<style scoped>
/* 整体布局，左侧是侧边栏，右侧是导航栏和主内容 */
.body {
//height: 100vh; /* 占满整个视窗高度 */ display: flex;
  width: 100%;
  height: 100%;
  background-color: #eef5f9;
  justify-content: space-between;
}

/* 主内容区域 */
.main-content {
  display: flex;
  flex-direction: column;
  width: 76%;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-color: #eef5f9;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  width: 95%;
  height: 100%;
}

.content-scroll{
  margin-left: 5%;
}

.content {
  width: 100%;
  height: 100%;
}

.pagination-wrapper{
  display: flex;
  width: 100%;
  justify-content: center;
  align-items: center;
}

.filter-panel,
.sentiment-items {
  width: 97.6%; /* 确保占满父容器 */
}

.filter-panel {
  margin-top: 40px;
  margin-bottom: 40px; /* 过滤面板与主内容之间添加20px的空隙 */
}

.right-sidebar {
  grid-area: search-sidebar;
  background-color: #fff;
  overflow-y: auto;
  width: 24%;
  height: 100%;
}
</style>
