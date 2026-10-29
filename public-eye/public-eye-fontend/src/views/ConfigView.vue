<script setup>
import axios from "axios";
import { constant } from "@/store/constant";
import { ElMessage } from 'element-plus';
import { ref } from 'vue';

const isAdd = ref(false);
const state = ref({
  page: 1,
  page_size: 10,
  total_pages: 3,
  total_items: 24,
  createTimeEnd: "",
  createTimeStart: "",
  searchValue: "",
});
const displayData_demo = ref([
  {
    id: 1,
    name: '施工安全',
    keywords: '安全帽',
    frame_interval_ms: 1000,
    created_at: '2024-9-25 17:00:00',
    // 添加图片为背景
    backgroundImg: constant.backgroundImg[1]
  },
  {
    id: 2,
    name: '道路识别',
    keywords: '柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路 柏油马路',
    frame_interval_ms: 500,
    created_at: '2024-9-24 9:00:00',
    backgroundImg: constant.backgroundImg[2]
  },
])
const displayData = ref(displayData_demo.value)
const frame_interval_options = ref([
  {
    value: 500,
    label: '500ms',
  },
  {
    value: 1000,
    label: '1000ms',
  },
  {
    value: 2000,
    label: '2000ms',
  },
  {
    value: 5000,
    label: '5000ms',
  },
])
const addDialogVisible = ref(false);
const addForm = ref({
  id: -1,
  name: '',
  keywords: '',
  frame_interval_ms: 1000,
});
const addFormRef = ref(null)
const addFormRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  keywords: [{ required: true, message: '请输入检索关键词', trigger: 'blur' }],
  frame_interval_ms: [{ required: true, message: '请选择检索抽帧间隔', trigger: 'blur' }],
};

function handleEditing(index, event) {
  addDialogVisible.value = true;
  isAdd.value = false;
  // 不可以直接 addForm.value = displayData.value[index]，否则会引起数据双向绑定
  addForm.value.id = displayData.value[index].id
  addForm.value.name = displayData.value[index].name
  addForm.value.keywords = displayData.value[index].keywords
  addForm.value.frame_interval_ms = displayData.value[index].frame_interval_ms
}

function handleAdd(event) {
  addDialogVisible.value = true;
  isAdd.value = true;
}

function handleSizeChange() {
  console.log("The size of page changed");
}

function handlePageChange(newPageNum) {
  state.value.pageNum = newPageNum
  getData()
}

const loadData = () => {
  // 这里可以根据当前页码和每页条数从实际数据源获取数据并更新 tableData
  const startIndex = (state.value.page - 1) * state.value.page_size
  displayData.value = generateMockData(startIndex, startIndex + state.value.page_size)
};

// 生成模拟数据，测试分页
const generateMockData = (start, end) => {
  const data = [];
  for (let i = start; i < end; i++) {
    if(i < displayData_demo.value.length)
      data.push(displayData_demo.value[i])
  }
  return data;
};

function handleSubmit() {
  if (isAdd.value)
    add();
  else
    update();
}

function trimText(text, maxLength) {
  if (text.length > maxLength) {
    return text.slice(0, maxLength) + '...';
  }
  return text;
}

async function getData() {
  try {
    const ret = await axios.get(`${constant.host}/seek_configs`, {params: state.value});
    if (ret.status !== 200) {
      ElMessage.error(ret.data.detail);
    }
    displayData.value = ret.data.data.items;
    state.value.total_items = ret.data.data.total_items;
    state.value.total_pages = ret.data.data.total_pages;
  } catch (error) {
    ElMessage.error(error);
  }
}

// 点击按钮，添加新配置
async function add() {
  await addFormRef.value.validate().then(async (valid) => {
    if (!valid) return;
    try {
      const ret = await axios.post(`${constant.host}/seek_configs`, addForm.value)
      console.log(ret)
      if (ret.status === 200) {
        ElMessage.success("添加成功");
        addDialogVisible.value = false;
        getData();
      } else {
        ElMessage.error(ret.data.msg);
      }
    } catch (error) {
      ElMessage.error(error);
    }
  });
}

// 修改用户
async function update() {
  await addFormRef.value.validate().then(async (valid) => {
    if (!valid) return;
    try {
      console.log("id = ", addForm.value.id)
      const ret = await axios.put(`${constant.host}/seek_configs/${addForm.value.id}`, addForm.value)
      if (ret.status === 200) {
        ElMessage.success("修改成功");
        addDialogVisible.value = false;
        getData();
      } else {
        ElMessage.error(ret.data.msg);
      }
    } catch (error) {
      ElMessage.error(error);
    }
  });
}

async function deleteData(targetId) {
  try {
    const ret = await axios.delete(`${constant.host}/seek_configs/${targetId}`);
    if (ret.status === 200) {
      ElMessage.success("删除成功");
      getData()
    } else {
      ElMessage.error(ret.data.msg);
    }
  } catch (error) {
    ElMessage.error("error");
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
    <div class="find">
      <div class="select">
        <p style="font-size: 28px">检索配置</p>
      </div>
      <el-dialog :title="isAdd? '添加配置' : '修改配置'" v-model="addDialogVisible" width="30%" :style="{ height: '37%' }"
                 @close="addDialogClosed">
        <!-- 内容主体区 -->
        <el-form :model="addForm" :rules="addFormRules" ref="addFormRef" label-width="100px">
          <el-form-item label="配置名称" prop="name">
            <!-- prop 是验证规则属性 -->
            <el-input v-model="addForm.name" @input="change"></el-input>
          </el-form-item>
          <el-form-item label="检索关键词" prop="keywords">
            <!-- prop 是验证规则属性 -->
            <el-input v-model="addForm.keywords" @input="change"></el-input>
          </el-form-item>
          <el-form-item label="抽帧间隔" prop="frame_interval_ms">
            <!-- prop 是验证规则属性 -->
            <el-select v-model="addForm.frame_interval_ms" placeholder="Select" style="width: 240px">
              <el-option
                  v-for="item in frame_interval_options"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </el-form-item>

        </el-form>
        <!-- 底部区 -->
        <span slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </span>
      </el-dialog>

      <el-col :span="3" style="position: absolute; right: 360px;">
        <el-button type="primary" @click="handleAdd" @mousedown="e => e.preventDefault()">添加配置</el-button>
      </el-col>
      <div class="search">
        <input type="text" placeholder="搜索" v-model="state.searchValue">
        <el-icon size="18"><Search></Search></el-icon>
      </div>
    </div>

    <div class="table-wrapper">
      <table>
        <thead>
        <tr>
          <th>名称</th>
          <th>检索关键词</th>
          <th>抽帧间隔</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, index) in displayData" :key="item.id">
          <td>{{ trimText(item.name, 16) }}</td>
          <td>{{ trimText(item.keywords, 30) }}</td>
          <td>{{ `${item.frame_interval_ms} ms` }}</td>
          <td>{{ item.created_at }}</td>

          <td>
            <a href="#" @click.prevent="handleEditing(index)" style="color: #87CEFA;">
              <el-icon><Edit></Edit></el-icon> 修改
            </a>
            <span>&nbsp;</span>
            <!-- 添加一个空格 -->
            <span>&nbsp;</span>
            <!-- 添加一个空格 -->
            <span>&nbsp;</span>
            <!-- 添加一个空格 -->
            <span>&nbsp;</span>
            <!-- 添加一个空格 -->
            <span>&nbsp;</span>
            <!-- 添加一个空格 -->
            <span>&nbsp;</span>
            <!-- 添加一个空格 -->
            <a href="#" @click="deleteData(item.id)" style="color: #87CEFA;">
              <el-icon><Delete></Delete></el-icon>  删除
            </a>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <el-pagination :hide-on-single-page="false" v-model:current-page="state.page" :page-sizes="[10]" v-model:page-size="state.page_size"
                   layout="total, sizes, prev, pager, next, jumper" :total="state.total_items" @size-change="handleSizeChange"
                   @current-change="handlePageChange" style="float:right">
    </el-pagination>
  </div>
</template>

<style scoped>
.body{
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

.find{
  display: flex;
  /* justify-content: space-between; */
  align-items: center;
  margin: 70px;
  margin-bottom:10px;
}

.find .select{
  display: flex;
  position: absolute;
  left: 170px;
}

.find .select p{
  font-size:18px;
}

.select-opt{
  font-size:18px;
  height:33px;
  margin-left:20px;
  border: 1px solid grey;
  border-radius: 5px;
  padding-left:10px;
  padding-right:10px;
}

.select-opt:hover{
  background-color: #E1E1E1;
  transition: 0.3s;
}

.active{
  background-color: #409EFF;
  color:#fff;
}

.active:hover{
  background-color: #1D91E8;
}

.search{
  display: flex;
  align-items: center;
  /* margin-right:240px; */
  position: absolute;
  right: 300px;
}

.search input{
  font-size: 18px;
  padding: 10px;
  padding-left:40px;
  width: 250px;
  border: 2px solid white;
  border-radius: 30px;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  margin-right: -240px;
}

.search img{
  width: 30px;
}

.table-wrapper{
  margin: auto auto;
  width: 90%;
  height: 500px;
  justify-content: center;
}

table{
  width: 100%;
}

table th{
  font-size: 20px;
  background-color: #F4F6F8;
  height: 60px;
}

table td{
  font-size: 18px;
  text-align: center;
  height: 40px;
}

table td:nth-child(1){
  width: 20%;
}

table td:nth-child(2){
  width: 40%;
}

table tr:hover{
  background-color: #F6F6F6;
}

.el-pagination {
  margin: auto;
  margin-top: 20px;
}
img {
  place-items: center;
  max-width: 80%;
  max-height: 80%;
}

.img-td{
  display: flex;
  justify-content: center;
  align-items: center;
}

el-input {
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  height: 20%;
}

th{
  text-after-overflow: ellipsis
}

</style>
