<template>
  <div class="user-profile">
    <!-- User Info Card -->
    <div class="user-header">
      <div class="avatar-ring">
        <van-icon name="user-circle-o" size="64" color="#333" />
        <div class="role-tag" v-if="userRole">{{ userRole }}</div>
      </div>
      <div class="user-info-text">
        <h2 class="username">{{ userName }}</h2>
        <p class="user-id">UID: {{ userId }}</p>
      </div>
    </div>

    <!-- Menu List -->
    <div class="menu-section">
        <div class="menu-card">
            <div class="menu-item" v-if="isAdmin" @click="router.push('/user-management')">
                <div class="menu-icon-box blue"><van-icon name="friends" /></div>
                <div class="menu-content">
                    <span class="menu-title">用户管理</span>
                    <van-icon name="arrow" color="#ccc" />
                </div>
            </div>

            <div class="menu-item">
                <div class="menu-icon-box purple"><van-icon name="lock" /></div>
                <div class="menu-content">
                    <span class="menu-title">修改密码</span>
                    <van-icon name="arrow" color="#ccc" />
                </div>
            </div>

            <div class="menu-item">
                <div class="menu-icon-box orange"><van-icon name="setting" /></div>
                <div class="menu-content">
                    <span class="menu-title">系统设置</span>
                    <van-icon name="arrow" color="#ccc" />
                </div>
            </div>

            <div class="menu-item no-border">
                <div class="menu-icon-box green"><van-icon name="info" /></div>
                <div class="menu-content">
                    <span class="menu-title">关于我们</span>
                    <span class="menu-value">v1.0.0</span>
                    <van-icon name="arrow" color="#ccc" />
                </div>
            </div>
        </div>

      <div class="logout-btn-container">
        <button class="logout-btn" @click="handleLogout">
          退出登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showDialog } from 'vant';

const router = useRouter();

const userName = ref('');
const userId = ref('');
const userRole = ref('');

const isAdmin = computed(() => {
  return userRole.value === 'ADMIN';
});

onMounted(() => {
  // Load user info from localStorage
  userName.value = localStorage.getItem('userName') || 'Guest';
  userId.value = localStorage.getItem('userId') || '-';
  // Check both keys due to potential inconsistency
  userRole.value = localStorage.getItem('userRole') || localStorage.getItem('role') || 'USER';
});

const handleLogout = () => {
  showDialog({
    title: '提示',
    message: '确认要退出登录吗？',
    showCancelButton: true,
  }).then((action) => {
    if (action === 'confirm') {
      localStorage.clear();
      router.replace('/login');
    }
  });
};
</script>

<style scoped>
.user-profile {
  padding-bottom: 20px;
  min-height: 100%;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

.user-header {
  display: flex;
  align-items: center;
  padding: 20px 0 40px;
}

.avatar-ring {
  position: relative;
  width: 80px; height: 80px;
  border-radius: 50%;
  background: #f7f8fa;
  display: flex; justify-content: center; align-items: center;
  margin-right: 20px;
  border: 4px solid #fff;
  box-shadow: 0 8px 20px rgba(0,0,0,0.06);
}

.role-tag {
  position: absolute;
  bottom: -4px;
  background: linear-gradient(135deg, #333, #000);
  color: white;
  font-size: 10px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}

.user-info-text {
    flex: 1;
}

.username {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
}

.user-id {
  margin: 0;
  font-size: 13px;
  color: #999;
  letter-spacing: 0.5px;
}

/* Menu Items */
.menu-card {
    background: #fff;
    border-radius: 24px;
    padding: 8px 16px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.04);
    border: 1px solid rgba(0,0,0,0.02);
}

.menu-item {
    display: flex;
    align-items: center;
    padding: 16px 0;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
    transition: background-color 0.2s;
}
.menu-item:active { opacity: 0.7; }
.menu-item.no-border { border-bottom: none; }

.menu-icon-box {
    width: 40px; height: 40px;
    border-radius: 12px;
    display: flex; justify-content: center; align-items: center;
    margin-right: 16px;
    font-size: 18px;
}
.menu-icon-box.blue { background: #e3f2fd; color: #1976d2; }
.menu-icon-box.purple { background: #f3e5f5; color: #7b1fa2; }
.menu-icon-box.orange { background: #fff3e0; color: #f57c00; }
.menu-icon-box.green { background: #e8f5e9; color: #388e3c; }

.menu-content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.menu-title {
    font-size: 15px;
    font-weight: 500;
    color: #333;
}

.menu-value {
    font-size: 12px;
    color: #999;
    margin-right: 8px;
}

/* Logout Button */
.logout-btn-container {
  margin: 40px 0 20px;
}
.logout-btn {
    width: 100%;
    height: 52px;
    border-radius: 16px;
    background: #fff;
    color: #ff3b30;
    font-size: 16px;
    font-weight: 600;
    border: 1px solid #ffebeb;
    cursor: pointer;
    transition: all 0.2s;
}
.logout-btn:active {
    background: #fff5f5;
    transform: scale(0.98);
}
</style>
