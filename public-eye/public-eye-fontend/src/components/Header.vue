<script setup>
import {Expand, Fold, Setting} from "@element-plus/icons-vue";
import {store} from "@/store/store";
import {ref, watch} from "vue";
import {constant} from "@/store/constant";
import axios from "axios";
import {ElMessage} from "element-plus";
import Utils from "@/utils/utils";

// 获取父组件传递过来的数据
const showIcon = defineProps({
  isCollapse: Boolean,
  headerWidth: String
})
// 获取父组件自定义的事件
const emit = defineEmits(['changeAside'])
const collapseAside = () => {
  emit('changeAside')
}

const readCookies = () => {
  if($cookies.isKey('header') && !Utils.is_null($cookies.get('header')['Authorization'])) {
    for (let key in store){
      store[key] = JSON.parse(JSON.stringify($cookies.get(key)));
    }
    store.isAdmin = store.isAdmin === 'true'
    store.login = store.login === 'true';
    // store.isAdmin = ($cookies.get("isAdmin") === 'false')? false : true

    axios.defaults.headers['accept'] = 'application/json';
    axios.defaults.headers['Authorization'] = store.header.Authorization
    // axios.defaults.headers['user_id'] = store.user.id;
    // store.user.stu_id = '3722xxxxxxxxxx'
    // store.user.phone = '180xxxxxxxx'
    // store.user.email = 'xxxxxxxxx@qq.com'
    store.user.image_url = 'https://img0.baidu.com/it/u=962511916,1823173324&fm=253&fmt=auto&app=120&f=JPEG?w=501&h=500'
    console.log("读取cookie成功")
  }
  else {
    store.login = false
    store.isAdmin = false
    console.log("读取cookie失败或没有cookie")
  }
  console.log("store", store)
}

//清除用户信息cookies
const removeCookies = () => {
  for (let key in constant.origin_store) {
    store[key] = constant.origin_store[key]
  }
}

//监听登录信息，修改cookies
watch(() => store, () => {
  for (let key in store) {
    $cookies.set(key, store[key])
  }
}, {deep: true})


// 登出按钮
const logout = ()=>{
  store.login = false
  removeCookies()
  Object.keys(store.user).forEach((key) => delete(store.user[key]));
}

const loginAppear = ref(false);
const isAdmin = ref(false);
const username=ref('')
const password=ref('')

const login = () => {
  sendLoginRequest()
}

// // 定义一个异步函数来发送 POST 请求，更改对应id的用户信息
async function sendLoginRequest() {

  if (!username.value || !password.value) {
    ElMessage.error("请填写用户名和密码");
    return;
  }

  try {
    // 使用 Axios 发送 POST 请求，并包含 JSON 数据
    const response = await axios.post(`${constant.host}/users/login`, {
      username: username.value,
      password: password.value,
    });
    // 登录成功
    if (response.data.code === 1 && response.data.data.token) {
      const res = {
        data: {
          access_token: "__CONFIGURE_LOCALLY__",
          //token_type: "bearer",
        }
      }
      res.data.token = response.data.data.token
      ElMessage.success("登录成功");
      store.login = true
      store.user.username = username.value
      store.header.access_token = res.data.token
      //store.header.token_type = res.data.token_type
      store.header.Authorization = `${store.header.token_type} ${store.header.access_token}`
      axios.defaults.headers.common['Authorization'] = store.header.Authorization;

      loginAppear.value = false
    }
    else{
      // ElMessage.error(res.data.msg)
      ElMessage.error("用户名或密码错误")
    }
  } catch (error) {
    // 请求失败，捕获并处理错误
    console.error('Error login:', error);
  }
}

function handleRegister(){
  register()
}

async function register(){
  try {
    const body = {
      "username": "__CONFIGURE_LOCALLY__",
      "password": "__CONFIGURE_LOCALLY__"
    }

    const ret = await axios.post(`${constant.host}/register`, body, store.header);
    if (ret.data.code !== 1) {
      ElMessage.error(ret.data.msg);
    } else {
      ElMessage.success("注册成功");
      // addDialogVisible.value = false;
      // getTeamList();
    }
  } catch (error) {
    ElMessage.error(error);
  }
}

//读取用户cookies
readCookies()
</script>

<template>
  <div class="body" :style="{width: `${headerWidth}px`}">
    <!-- 左侧菜单图标 -->
    <div class="left">
      <el-icon v-show="!showIcon.isCollapse" @click="collapseAside" size="16px" style="color: #303133;">
        <Fold />
      </el-icon>
      <el-icon v-show="showIcon.isCollapse" @click="collapseAside" size="16px" style="color: #303133;">
        <Expand />
      </el-icon>
    </div>

    <!-- 右侧图标和用户名 -->
    <div class="right" v-if="store.login">
      <div class="block" style="margin-right: 10px;">
        <el-avatar :size="40" src="https://dimg04.c-ctrip.com/images/zc0a170000011f8t5F2C8.jpg" />
      </div>
      <span class="username">昭昭玉</span>
      <el-dropdown trigger="click">
        <el-icon style="margin-left: 18px; margin-top: 1px;color: #303133;">
          <setting />
        </el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>View</el-dropdown-item>
            <el-dropdown-item>Add</el-dropdown-item>
            <el-dropdown-item @click="logout()">Logout</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <!-- 未登录状态 -->
    <div class="header-login-button" v-if="!store.login">
      <button @click="loginAppear=true">Login</button>
    </div>
  </div>

  <!-- 登录界面 -->
  <div class="login-back" v-if="loginAppear">
    <div class="login-dialog">
      <div class="login-dialog-header">
        <h3>{{ isAdmin ? "Admin " : "User " }}Login</h3>
        <div class="close-button" @click="loginAppear = false">X</div>
      </div>
      <div class="login-dialog-content">
        <input
            v-model="username"
            class="username"
            type="text"
            placeholder="Please enter your account"
            @keyup.enter="login"
        />
        <input
            v-model="password"
            class="password"
            type="password"
            placeholder="Please enter your password"
            @keyup.enter="login"
        />
      </div>
      <el-button class="dialog-login-button" @click="login" type="primary" round>
        Login
      </el-button>
      <div class="footer">
        <a href="#" class="admin-login-switcher" @click="handleRegister">Register</a>
<!--        <a href="#" class="admin-login-switcher" @click="toggleRole">{{ isAdmin ? "User " : "Admin " }}Login</a>-->
      </div>
    </div>
  </div>
</template>


<style scoped>
.body {
  height: 8vh;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: width 0.7s ease;
}

.left{
  display: flex;
  width: 4%;
  justify-content: center;
  align-items: center;
}

.right {
  display: flex;
  align-items: center;
  margin-right: 2%;
}

.header-login-button button{
  width: 6vw;
  height: 4.5vh;
  background-color: #409EFF;
  color: white;
  font-size:16px;
  margin-right: 2vw;
  border:none;
  border-radius: 30px;
}

.header-login-button button:hover {
  background-color: #1D91E8;
  transition: background-color 0.3s, transform 0.3s, box-shadow 0.3s;
}

.header-login-button button:focus{
  outline:none;
}

.login-back {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1000; /* 一个较高的z-index值 */
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-dialog {
  display: flex;
  flex-direction: column;
  background: #fff;
  width: 30%;
  height: 45%;
  border-radius: 20px;
  justify-content: center;
  align-items: center;
}

.login-dialog-header{
  display: flex;
  margin-top: 0%;
  height: 20%;
  width: 90%;
  justify-content: space-between;
  align-items: center;
}

.login-dialog-header h3{
  font-size: 30px;
  font-weight:1000;
  line-height: 200%;
}

.close-button {
  font-size: 20px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.close-button:hover {
  background-color: red;
  color:white;
  transition: 0.3s;
}

.login-dialog-content{
  display: flex;
  flex-direction: column;
  width: 90%;
  margin-top: 5%;
  height: 30%;
  justify-content: center;
  align-items: center;
}

.login-dialog input{
  width: 400px;
  height: 40px;
  border-radius: 40px;
  font-size: 18px;
  padding: 10px 10px 10px 20px;
}

.password{
  margin-top: 5%;
}

.dialog-login-button{
  margin-top: 5%;
  margin-bottom: 5%;
  height: 15%;
  width: 35%;
  font-size: 1.5vw;
  background-color: #1D91E8;
}

.login-dialog .footer{
  height: 20%;
  width: 90%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-login-switcher{
  color: #1D91E8;
  width: 22%;
  height: 50%;
  text-align: center;
}
</style>
