<script setup>
import axios from "axios";
import {constant} from "@/store/constant";
import {ElMessage} from 'element-plus';
import {ref, watch} from 'vue';
import {store} from "@/store/store";
import Utils from "@/utils/utils";

const isAdd = ref(false);
const state = ref({
  page: 1,
  pageSize: 10,
  total: 0,
  ids: store.event.newsIds,
  searchTerm: "",
  startDatetime: "",
  endDatetime: "",
});
const config_demo = ref([
  {
    id: 1,
    userId: 'djxtsg',
    name: '猪猪',
    searchKey: '猪',
    expireDate: '2024-9-25 17:00:00',
    createTime: '2024-9-24 17:00:00'
  },
]);
const displayData = ref([]);
const addDialogVisible = ref(false);
const addForm = ref({
  id: 0,
  name: '',
  searchKey: '',
  expireDate: '',
});
const addFormRef = ref(null);
const addFormRules = {
  name: [{required: true, message: '请输入名称', trigger: 'blur'}],
  searchKey: [{required: true, message: '请输入搜索关键词', trigger: 'blur'}],
  expireDate: [{required: true, message: '请选择到期日期', trigger: 'change'}],
};

watch(() => store.login, () => {
  if(store.login === true)
    getData()
}, {deep: true});

function handleEditing(index) {
  addDialogVisible.value = true;
  isAdd.value = false;
  addForm.value.id = displayData.value[index].id;
  addForm.value.userId = displayData.value[index].userId; // 保留用户ID的自动填充
  addForm.value.name = displayData.value[index].name;
  addForm.value.searchKey = displayData.value[index].searchKey;
  addForm.value.expireDate = displayData.value[index].expireDate;
}

function handleAdd() {
  addDialogVisible.value = true;
  isAdd.value = true;
}

function handleSizeChange() {
  console.log("The size of page changed");
}

function handlePageChange(newPageNum) {
  state.value.pageNum = newPageNum;
  getData();
}

const loadData = () => {
  const startIndex = (state.value.page - 1) * state.value.pageSize;
  displayData.value = generateMockData(startIndex, startIndex + state.value.pageSize);
};

const generateMockData = (start, end) => {
  const data = [];
  for (let i = start; i < end; i++) {
    if (i < config_demo.value.length) data.push(config_demo.value[i]);
  }
  return data;
};

function formatDateWithoutTime(date) {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateWithTime(date) {
  if (!date) return '';
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function handleSubmit() {
  addForm.value.expireDate = formatDateWithoutTime(addForm.value.expireDate);

  if (isAdd.value) {
    add();
  } else {
    update();
  }
}

function trimText(text, maxLength) {
  if (text.length > maxLength) {
    return text.slice(0, maxLength) + '...';
  }
  return text;
}

async function getData() {
  const ret = await axios.get(`${constant.host}/user/config/list`, {
    params: {
      page: state.value.page,
      pageSize: state.value.pageSize,
      searchTerm: state.value.searchTerm,
    },
  });
  if (Utils.check_res(ret)) {
    displayData.value = ret.data.data.records;
    state.value.total = ret.data.data.total;
  }
}

async function add() {
  await addFormRef.value.validate().then(async (valid) => {
    if (!valid) return;

    const ret = await axios.post(`${constant.host}/user/config`, addForm.value);
    if (Utils.check_res(ret)) {
      ElMessage.success("添加成功");
      addDialogVisible.value = false;
      getData();
    }
  });
}

async function update() {
  await addFormRef.value.validate().then(async (valid) => {
    if (!valid) return;

    const ret = await axios.put(`${constant.host}/user/config/${addForm.value.id}`, addForm.value);
    if (Utils.check_res(ret)) {
      ElMessage.success("修改成功");
      addDialogVisible.value = false;
      getData();
    }
  });
}

async function deleteData(targetId) {
  const ret = await axios.delete(`${constant.host}/user/config/${targetId}`);
  if (Utils.check_res(ret)) {
    ElMessage.success("删除成功");
    getData();
  }
}

async function created() {
  getData();
}

created();
</script>

<template>
  <div class="body">
    <div class="find">
      <div class="select">
        <p style="font-size: 28px">搜索配置</p>
      </div>
      <el-dialog :title="isAdd ? '添加配置' : '修改配置'" v-model="addDialogVisible" width="30%">
        <el-form :model="addForm" :rules="addFormRules" ref="addFormRef" label-width="100px">
          <el-form-item></el-form-item>

          <el-form-item label="名称" prop="name">
            <el-input v-model="addForm.name"></el-input>
          </el-form-item>
          <el-form-item label="搜索关键词" prop="searchKey">
            <el-input v-model="addForm.searchKey"></el-input>
          </el-form-item>
          <el-form-item label="到期日期" prop="expireDate">
            <el-date-picker v-model="addForm.expireDate" type="datetime"></el-date-picker>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button type="primary" @click="handleSubmit">确定</el-button>
          <el-button @click="addDialogVisible = false">取消</el-button>
        </span>
      </el-dialog>

      <el-col :span="3" style="position: absolute; right: 360px;">
        <el-button type="primary" @click="handleAdd" @mousedown="e => e.preventDefault()">添加配置</el-button>
      </el-col>
      <div class="search">
        <input type="text" placeholder="搜索" v-model="state.searchTerm">
        <el-icon size="18">
          <Search></Search>
        </el-icon>
      </div>
    </div>

    <div class="table-wrapper">
      <table style="width: 100%; table-layout: fixed;">
        <thead>
        <tr>
<!--          <th style="width: 10%;">序号</th>-->
          <th style="width: 15%;">名称</th>
          <th style="width: 15%;">搜索关键词</th>
          <th style="width: 15%;">到期日期</th>
          <th style="width: 15%;">创建日期</th>
          <th style="width: 20%;">操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, index) in displayData" :key="item.id">
<!--          <td>{{ trimText(item.id, 5) }}</td>-->
          <td>{{ trimText(item.name, 20) }}</td>
          <td>{{ trimText(item.searchKey, 20) }}</td>
          <td>{{ trimText(item.expireDate, 20) }}</td>
          <td>{{ trimText(item.createTime, 20) }}</td>
          <td>
            <a href="#" @click.prevent="handleEditing(index)" style="color: #00a1ff;">
              <el-icon>
                <Edit></Edit>
              </el-icon>
              修改
            </a>
            &nbsp;&nbsp;&nbsp;
            <a href="#" @click="deleteData(item.id)" style="color: #00a1ff;">
              <el-icon>
                <Delete></Delete>
              </el-icon>
              删除
            </a>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <el-pagination :hide-on-single-page="false" v-model:current-page="state.page" :page-sizes="[10]"
                   v-model:page-size="state.pageSize"
                   layout="total, sizes, prev, pager, next, jumper" :total="state.total" @size-change="handleSizeChange"
                   @current-change="handlePageChange" style="float:right">
    </el-pagination>
  </div>
</template>

<style scoped>
.body {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

.find {
  display: flex;
  align-items: center;
  margin: 70px;
  margin-bottom: 10px;
}

.find .select {
  display: flex;
  position: absolute;
  left: 170px;
}

.find .select p {
  font-size: 18px;
}

.search {
  display: flex;
  align-items: center;
  position: absolute;
  right: 300px;
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

table tr:hover {
  background-color: #F6F6F6;
}

.el-pagination {
  margin: auto;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  height: 20%;
}

th {
  text-after-overflow: ellipsis;
}
</style>
