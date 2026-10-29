<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="logo-section">
           <div class="app-logo">
             <van-icon name="chart-trending-o" size="48" color="#fff" />
           </div>
           <h1 class="app-name">SignalX</h1>
           <p class="app-slogan">全网舆情 智能感知</p>
        </div>

        <div class="form-section">
          <div class="welcome-text">{{ isRegister ? '创建新账号' : '欢迎回来' }}</div>

          <van-form @submit="onSubmit">
            <div class="custom-input-group">
              <van-field
                v-model="username"
                name="username"
                placeholder="请输入用户名"
                :rules="[{ required: true, message: '' }]"
                class="custom-field"
                :border="false"
              >
                 <template #left-icon>
                    <van-icon name="manager-o" class="field-icon" />
                 </template>
              </van-field>

              <van-field
                v-if="isRegister"
                v-model="email"
                name="email"
                placeholder="请输入邮箱"
                :rules="[{ required: false }]"
                class="custom-field"
                :border="false"
              >
                 <template #left-icon>
                    <van-icon name="envelop-o" class="field-icon" />
                 </template>
              </van-field>

              <van-field
                v-model="password"
                type="password"
                name="password"
                placeholder="请输入密码"
                :rules="[{ required: true, message: '' }]"
                class="custom-field"
                :border="false"
              >
                 <template #left-icon>
                    <van-icon name="lock" class="field-icon" />
                 </template>
              </van-field>
            </div>

            <div class="action-area">
              <van-button
                round
                block
                type="primary"
                native-type="submit"
                :loading="loading"
                class="login-btn"
                color="linear-gradient(to right, #1989fa, #0575e6)"
              >
                {{ isRegister ? '立即注册' : '登 录' }}
                <van-icon name="arrow" class="btn-icon" />
              </van-button>
            </div>

            <div class="toggle-box">
              <span class="toggle-text" @click="isRegister = !isRegister">
                {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
              </span>
            </div>
          </van-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import middleware from '@/api/middleware';

const router = useRouter();
const username = ref('');
const password = ref('');
const email = ref('');
const loading = ref(false);
const isRegister = ref(false);

const onSubmit = async (values) => {
  if(!values.username || !values.password) {
      showToast('请填写完整信息');
      return;
  }

  loading.value = true;
  try {
    if (isRegister.value) {
      // 注册逻辑
      await middleware.addUser({
        username: values.username,
        password: values.password,
        email: values.email,
        role: 'USER'
      });
      showToast('注册成功，请直接登录');
      isRegister.value = false;
    } else {
      // 登录逻辑
      const res = await middleware.login(values);
      // 登录成功，存储信息
      localStorage.setItem('token', res.token);
      localStorage.setItem('userId', res.id);
      localStorage.setItem('userName', res.username);
      localStorage.setItem('userRole', res.role);

      showToast('登录成功');
      router.replace('/dashboard');
    }
  } catch (error) {
    showToast((isRegister.value ? '注册失败: ' : '登录失败: ') + (error.message || '网络错误'));
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
  overflow: hidden;
}

.login-container {
  width: 100%;
  max-width: 380px;
  padding: 20px;
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes fadeInUp {
  0% { opacity: 0; transform: translateY(40px); }
  100% { opacity: 1; transform: translateY(0); }
}

.login-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 20px 40px rgba(0,0,0,0.2);
}

/* Header Section */
.logo-section {
    background: linear-gradient(135deg, #1989fa 0%, #0575e6 100%);
    padding: 40px 20px 30px;
    text-align: center;
    color: #fff;
    position: relative;
}
.logo-section::after {
    content: '';
    position: absolute;
    bottom: -20px;
    left: 0;
    right: 0;
    height: 40px;
    background: #fff;
    border-radius: 50% 50% 0 0 / 100% 100% 0 0;
    transform: scaleX(1.5);
}

.app-logo {
    width: 64px;
    height: 64px;
    background: rgba(255,255,255,0.2);
    border-radius: 16px;
    margin: 0 auto 16px;
    display: flex;
    justify-content: center;
    align-items: center;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255,255,255,0.3);
    box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}

.app-name {
    margin: 0;
    font-size: 28px;
    font-weight: 800;
    letter-spacing: 1px;
    text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.app-slogan {
    margin: 8px 0 0;
    font-size: 13px;
    opacity: 0.8;
    letter-spacing: 2px;
    text-transform: uppercase;
}

/* Form Section */
.form-section {
    padding: 30px 24px 34px;
    background: #fff;
}

.welcome-text {
    text-align: center;
    font-size: 18px;
    color: #333;
    font-weight: 600;
    margin-bottom: 24px;
}

.custom-input-group {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 32px;
}

.custom-field {
    background: #f5f6fa;
    border-radius: 12px;
    padding: 12px 16px;
    transition: all 0.3s;
}
.custom-field:focus-within {
    background: #fff;
    box-shadow: 0 0 0 2px rgba(25, 137, 250, 0.2);
}
.field-icon {
    font-size: 18px;
    color: #999;
    margin-right: 8px;
}

.login-btn {
    height: 50px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 2px;
    box-shadow: 0 8px 16px rgba(25, 137, 250, 0.24);
    display: flex;
    align-items: center;
    justify-content: center;
}
.btn-icon {
    margin-left: 8px;
    font-size: 18px;
}

.toggle-box {
    text-align: center;
    margin-top: 24px;
}
.toggle-text {
    color: #666;
    font-size: 13px;
    cursor: pointer;
    transition: color 0.2s;
}
.toggle-text:hover {
    color: #1989fa;
    text-decoration: underline;
}
</style>
