<script setup>
import axios from "axios";
import {constant} from "@/store/constant";
import {ElMessage} from 'element-plus';
import {ref} from 'vue';
import {store} from "@/store/store";
import Utils from "../utils/utils";
import Heat from "@/components/Heat.vue";
import router from "@/router";
import BreadcrumbNavigation from "@/components/BreadcrumbNavigation.vue";

const state = ref({
  page: 1,
  pageSize: 10,
  total: 0,
  ids: store.event.newsIds,
  searchTerm: "",
  startDatetime: "",
  endDatetime: "",
});
const displayData_demo = ref([])
const displayData = ref(displayData_demo.value)
const bindingStartDate = ref(null)
const bindingEndDate = ref(null)

const goToDetail = (news) => {
  store.news = news
  store.sentimentValue = store.event.sentiment;
  router.push("Detail")
}

const getSourceLogo = (source) => {
  return source === "新浪新闻" ? "https://img2.baidu.com/it/u=3255055974,1621793350&fm=253&fmt=auto&app=138&f=JPEG?w=800&h=356"
      : source === "南方周末" ? "https://images.infzm.com/cms/medias/image/23/06/30/SPsZxLYp6P1Pr9aggAlpDlIvXsxZxZylFbBL9iWS.png"
          : source === "央视网" ? "https://img0.baidu.com/it/u=2700008398,741166420&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=211"
              : source === "腾讯网" ? "https://pic.rmb.bdstatic.com/6c59d4ecd4494249e030104f630ed012.jpeg"
                  : source === "抖音" ? "https://img0.baidu.com/it/u=2966620186,4162986458&fm=253&fmt=auto&app=138&f=JPEG?w=1150&h=500"
                    : "";
}

function handleSizeChange() {
  console.log("The size of page changed");
}

function handlePageChange(newPageNum) {
  state.value.page = newPageNum
  getData()
}

async function getData() {
  state.value.startDatetime = bindingStartDate.value ?
      Utils.parseDateToDatetime(bindingStartDate.value)
      : "";

  state.value.endDatetime = bindingEndDate.value ?
      Utils.parseDateToDatetime(bindingEndDate.value)
      : "";
  try {
    const ret = await axios.get(`${constant.host}/user/news/list`, {params: state.value});
    if (ret.status !== 200) {
      ElMessage.error(ret.data.detail);
    }
    displayData.value = ret.data.data.records;
    state.value.total = ret.data.data.total;
  } catch (error) {
    ElMessage.error(error);
  }
}

async function created() {
  getData()
}

created()

</script>

<template>
  <!-- 当前页展示的信息在 displayData、所有的信息在 historyData。主要区别在于是否经过搜索或分页 -->
  <div class="body">
    <div class="header">【{{ store.keyword.keyword }} --> {{ store.event.description }}】 的相关新闻</div>
    <div class="search-wrapper">
      <el-col :span="3" style="position: absolute; right: 360px;">
      </el-col>
      <div class="search">
        <el-date-picker
            v-model="bindingStartDate"
            @change="getData"
            type="date"
            placeholder="起始时间"
            style="margin-right: 5%">
        </el-date-picker>
        <el-date-picker
            v-model="bindingEndDate"
            @change="getData"
            type="date"
            placeholder="结束时间"
            style="margin-right: 5%">
        </el-date-picker>
        <input type="text" placeholder="搜索" @change="getData" v-model="state.searchTerm">
        <el-icon size="18">
          <Search></Search>
        </el-icon>
      </div>
    </div>

    <div class="table-wrapper">
      <table>
        <thead>
        <tr>
          <th>来源</th>
          <th>标题</th>
          <th>作者</th>
          <th>发布时间</th>
          <th>热度</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, index) in displayData" :key="item.id" @click="() => goToDetail(item)">
          <td>
            <img class="logo-img" v-show="getSourceLogo(item.source) !== ''" :src="getSourceLogo(item.source)">
            <span v-show="getSourceLogo(item.source) === ''">{{ item.source }}</span>
          </td>
          <td>{{ Utils.trimText(item.title, 24) }}</td>
          <td>{{ Utils.trimText(item.author, 12) }}</td>
          <td>{{ item.publishDatetime }}</td>
          <td>
            <Heat :heat="item.heat"></Heat>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :hide-on-single-page="false" v-model:current-page="state.page" :page-sizes="[10]"
                     v-model:page-size="state.pageSize"
                     layout="total, sizes, prev, pager, next, jumper" :total="state.total" @size-change="handleSizeChange"
                     @current-change="handlePageChange" style="float:right">
      </el-pagination>
    </div>
  </div>
</template>

<style scoped>
.body {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  align-items: center;
}

.header{
  width: 90%;
  font-size: 28px;
  font-weight: 500;
}

.search-wrapper {
  display: flex;
  width: 50%;
  height: 12%;
  justify-content: flex-end;
  align-items: center;
}

.search {
  display: flex;
  align-items: center;
}

.search input {
  font-size: 18px;
  padding: 10px;
  padding-left: 40px;
  width: 250px;
  border: 2px solid white;
  border-radius: 30px;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  margin-right: -240px;
}

.search img {
  width: 30px;
}

.table-wrapper {
  margin: auto auto;
  width: 90%;
  height: 500px;
  justify-content: center;
}

table {
  width: 100%;
}

table th {
  font-size: 20px;
  background-color: #F4F6F8;
  height: 60px;
}

table td {
  font-size: 18px;
  text-align: center;
  height: 40px;
}

table td:nth-child(1) {
  width: 10%;
}

table td:nth-child(2) {
  width: 40%;
}

table tr:hover {
  background-color: #F6F6F6;
}

.pagination-wrapper{
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 10%;
}

th {
  text-after-overflow: ellipsis
}

.logo-img{
  height: 100%;
  width: 100%;
  object-fit: contain;
  place-items: center;
}

</style>
