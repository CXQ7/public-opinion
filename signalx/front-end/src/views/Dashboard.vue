<template>
  <div class="tiimo-dashboard">
    <!-- Header -->
    <header class="header" :class="{ 'header-scrolled': isScrolled }" v-if="activeTab !== 'mine'">
      <div class="header-left">
        <div class="status-pill" @click="goToUserCenter">
          <van-icon name="user-circle-o" />
          <span>{{ currentUserDisplay }}</span>
        </div>
      </div>
      <div class="header-center">
        <div class="date-display">
          <div class="date-text">
            <div class="weekday">舆情看板</div>
            <div class="full-date">{{ currentDateStr }}</div>
          </div>
        </div>
      </div>
      <div class="header-right"></div>
    </header>

    <!-- User Profile Section -->
    <UserProfile v-if="activeTab === 'mine'" />

    <!-- Widget Cards (Stats) -->
    <div class="widget-scroll-container animate-fade-in" v-if="activeTab === 'overview'">
      <div class="widget-card purple">
        <div class="widget-icon-circle"><van-icon name="fire-o" size="24" /></div>
        <div class="widget-text">
          <div class="stat-num">{{ monitoringCount }}</div>
          <div class="stat-label">正在监测</div>
        </div>
      </div>
      <div class="widget-card blue">
        <div class="widget-icon-circle"><van-icon name="chart-trending-o" size="24" /></div>
        <div class="widget-text">
          <div class="stat-num">{{ todayReportCount }}</div>
          <div class="stat-label">今日报告</div>
        </div>
      </div>
    </div>

    <!-- Main Content Sections -->
    <div class="task-sections">
      <!-- Tab 1: Monitor (Analysis Flow) -->
      <div v-if="activeTab === 'monitor'" class="animate-fade-in">
        <!-- 模式切换按钮 -->
        <div class="monitor-mode-buttons">
          <button class="mode-btn" :class="{ active: monitorMode === 'normal' }" @click="monitorMode = 'normal'">
            新词监测
          </button>
          <button class="mode-btn" :class="{ active: monitorMode === 'openclaw' }" @click="monitorMode = 'openclaw'">
            定时任务
          </button>
        </div>

        <!-- 新词监测模式 -->
        <div v-show="monitorMode === 'normal'">
          <div class="section-header mb-3">
            <van-icon name="aim" class="mr-2" />
            <span class="section-title">新词监测 (Analysis)</span>
          </div>

          <div class="custom-card-content">
            <van-cell-group inset :border="false" style="margin:0; padding:0;">
              <van-field
                v-model="keyword"
                placeholder="输入关键词 (如: 计算机)"
                label="关键词"
                left-icon="search"
                class="custom-field"
              />
              <van-field
                v-model="timeDisplayName"
                is-link
                readonly
                name="picker"
                label="时间范围"
                left-icon="clock-o"
                placeholder="选择时间范围"
                @click="showPicker = true"
                class="custom-field"
              />
              <van-field
                v-model="intervalDisplayName"
                is-link
                readonly
                name="picker"
                label="生成频率"
                left-icon="replay"
                placeholder="选择报告生成间隔"
                @click="showIntervalPicker = true"
                class="custom-field"
              />
            </van-cell-group>
            <van-button
              type="primary"
              block
              color="#000"
              class="mt-3 action-btn"
              :loading="loading"
              @click="handleTriggerAnalysis"
            >
              <van-icon name="play-circle-o" class="mr-1" /> 开始感知
            </van-button>
          </div>

          <div class="animate-list-container">
            <div class="report-list-container">
              <div class="monitor-task-list">
                <div class="section-header mb-3">
                  <van-icon name="clock-o" class="mr-2" />
                  <span class="section-title">监测任务列表</span>
                </div>
                <div
                  v-for="task in taskList"
                  :key="task.taskId || task.id"
                  class="report-item"
                  @click="goToMonitorDetail(task.taskId || task.id)"
                >
                  <div class="report-icon">
                    <van-icon name="aim" color="#1989fa" size="24" />
                  </div>
                  <div class="report-info">
                    <div class="report-title">{{ task.keywords.join(', ') }}</div>
                    <div class="report-meta">
                      <div>ID: {{ task.taskId || task.id }} | 状态: {{ formatStatus(task.status) }}</div>
                      <div style="margin-top: 4px;">创建: {{ formatTimeStr(task.createTime || task.created_at) }}</div>
                    </div>
                  </div>
                  <div class="task-actions" @click.stop>
                    <van-icon name="edit" size="18" color="#1989fa" style="margin-right: 12px;" @click.stop="editTask(task)" />
                    <van-icon name="delete-o" size="18" color="#ff4444" @click.stop="deleteTask(task)" />
                  </div>
                </div>
                <van-empty v-if="taskList.length === 0" description="暂无监测任务" />
              </div>
            </div>
          </div>
        </div>

        <!-- Openclaw 定时任务模式 -->
        <div v-show="monitorMode === 'openclaw'">
          <OpenclawTasks @viewDetail="handleOpenclawDetail" />
        </div>
      </div>

      <!-- Overview List: Reports -->
      <div v-if="activeTab === 'overview'">
        <div class="section-header mb-3 mt-4 animate-fade-in">
          <van-icon name="orders-o" class="mr-2" />
          <span class="section-title">趋势报告 (Reports)</span>
          <span class="count-badge">({{ list.length }})</span>
        </div>

        <div class="animate-list-container">
          <div class="report-list-container">
            <van-list
              v-model:loading="listLoading"
              :finished="finished"
              finished-text="没有更多了"
              @load="onLoad"
            >
              <div
                v-for="item in list"
                :key="item.id"
                class="report-item"
                @click="goToReport(item.id)"
              >
                <div class="report-icon">
                  <van-icon
                    :name="(item.sentiment === 'NEGATIVE' || item.sentiment === 'negative') ? 'warn-o' : ((item.sentiment === 'POSITIVE' || item.sentiment === 'positive') ? 'smile-o' : 'info-o')"
                    :color="(item.sentiment === 'NEGATIVE' || item.sentiment === 'negative') ? '#ff605e' : ((item.sentiment === 'POSITIVE' || item.sentiment === 'positive') ? '#4caf50' : '#1989fa')"
                    size="24"
                  />
                </div>
                <div class="report-info">
                  <div class="report-title">{{ item.title }}</div>
                  <div class="report-meta">
                    {{ formatDate(item.createTime) }}
                  </div>
                </div>
                <div class="report-arrow">
                  <van-icon name="arrow" color="#ccc" />
                </div>
              </div>
            </van-list>
          </div>
        </div>
      </div>
    </div>

    <!-- Popups -->
    <van-popup v-model:show="showPicker" position="bottom" round>
      <van-picker
        :columns="timeOptions"
        @confirm="onConfirmTime"
        @cancel="showPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showIntervalPicker" position="bottom" round>
      <van-picker
        :columns="intervalOptions"
        @confirm="onConfirmInterval"
        @cancel="showIntervalPicker = false"
      />
    </van-popup>

    <!-- 编辑任务弹窗 -->
    <van-popup v-model:show="showEditPopup" position="bottom" round style="height: 70%;">
      <div style="padding: 16px;">
        <h3 style="text-align: center; margin-bottom: 20px;">编辑任务</h3>
        <van-cell-group inset :border="false">
          <van-field
            v-model="editKeyword"
            placeholder="输入关键词 (如: 计算机)"
            label="关键词"
            left-icon="search"
          />
          <van-field
            v-model="editTimeDisplayName"
            is-link
            readonly
            label="时间范围"
            left-icon="clock-o"
            placeholder="选择时间范围"
            @click="showEditTimePicker = true"
          />
          <van-field
            v-model="editIntervalDisplayName"
            is-link
            readonly
            label="生成频率"
            left-icon="replay"
            placeholder="选择报告生成间隔"
            @click="showEditIntervalPicker = true"
          />
        </van-cell-group>
        <div style="display: flex; gap: 12px; margin-top: 20px;">
          <van-button type="default" block @click="showEditPopup = false">取消</van-button>
          <van-button type="primary" color="#000" block :loading="editLoading" @click="handleSaveEdit">保存</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 编辑时的时间选择器 -->
    <van-popup v-model:show="showEditTimePicker" position="bottom" round>
      <van-picker
        :columns="timeOptions"
        @confirm="onConfirmEditTime"
        @cancel="showEditTimePicker = false"
      />
    </van-popup>

    <!-- 编辑时的频率选择器 -->
    <van-popup v-model:show="showEditIntervalPicker" position="bottom" round>
      <van-picker
        :columns="intervalOptions"
        @confirm="onConfirmEditInterval"
        @cancel="showEditIntervalPicker = false"
      />
    </van-popup>

    <!-- Bottom Tabbar -->
    <van-tabbar
      v-model="activeTab"
      active-color="#000"
      inactive-color="#ccc"
      :border="false"
      class="custom-tabbar"
      fixed
      safe-area-inset-bottom
    >
      <van-tabbar-item icon="apps-o" name="overview" @click="scrollToTop">概览</van-tabbar-item>
      <van-tabbar-item icon="search" name="monitor" @click="scrollToTop">监测</van-tabbar-item>
      <van-tabbar-item icon="user-o" name="mine" @click="goToUserCenter">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import middlewareApi from '@/api/middleware'
import UserProfile from './UserProfile.vue'
import OpenclawTasks from './OpenclawTasks.vue'
import { formatTimeStr, formatStatus } from '@/utils'

const route = useRoute()
const router = useRouter()

// UI States
const activeTab = ref('overview')
const monitoringCount = ref(0)
const todayReportCount = ref(0)
const monitorMode = ref('normal')
const isScrolled = ref(false)

const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

// Scroll Reveal Logic
let itemObserver = null
let statsTimer = null

const initObserver = () => {
  itemObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.remove('report-item-hidden')
      } else {
        entry.target.classList.add('report-item-hidden')
      }
    })
  }, { threshold: 0.1, rootMargin: '0px' })
}

// 根据 URL 参数恢复状态
const restoreStateFromQuery = () => {
  const { fromTab, fromMode } = route.query
  if (fromTab && fromMode) {
    activeTab.value = fromTab
    monitorMode.value = fromMode
    // 清除参数，避免下次刷新误用
    router.replace({ query: {} })
    return true
  }
  return false
}

// 监听路由变化
watch(() => route.query, () => {
  restoreStateFromQuery()
}, { deep: true })

onMounted(() => {
  // 恢复状态
  restoreStateFromQuery()

  window.addEventListener('scroll', handleScroll)
  initObserver()
  fetchDashboardStats()
  statsTimer = setInterval(fetchDashboardStats, 30000)
})

onUnmounted(() => {
  if (statsTimer) clearInterval(statsTimer)
  window.removeEventListener('scroll', handleScroll)
  if (itemObserver) itemObserver.disconnect()
})

// 日期相关函数
const getFormattedDate = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekDay = weekDays[date.getDay()]
  return `${year}年${month}月${day}日 ${weekDay}`
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    const pad = (n) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch (e) {
    return dateStr
  }
}


const currentDateStr = ref(getFormattedDate())
const currentUserDisplay = ref(localStorage.getItem('userName') || localStorage.getItem('userRole') || 'Admin')

// Business States
const keyword = ref('')
const timeRange = ref('24h')
const timeDisplayName = ref('最近24小时')
const interval = ref(60)
const intervalDisplayName = ref('1小时')
const showPicker = ref(false)
const showIntervalPicker = ref(false)
const loading = ref(false)
const listLoading = ref(false)
const finished = ref(false)
const list = ref([])
const taskList = ref([])
const currentTaskId = ref('')

// 编辑任务相关状态
const showEditPopup = ref(false)
const showEditTimePicker = ref(false)
const showEditIntervalPicker = ref(false)
const editLoading = ref(false)
const editingTask = ref(null)
const editKeyword = ref('')
const editTimeRange = ref('24h')
const editTimeDisplayName = ref('最近24小时')
const editInterval = ref(60)
const editIntervalDisplayName = ref('1小时')

// 监听列表变化，为每个报告项添加滚动动画
watch(list, () => {
  nextTick(() => {
    const items = document.querySelectorAll('.animate-list-container .report-item')
    items.forEach((item) => {
      item.classList.add('report-item-hidden')
      if (itemObserver) {
        itemObserver.observe(item)
      }
    })
  })
}, { deep: true })

const timeOptions = [
  { text: '最近24小时', value: '24h' },
  { text: '最近7天', value: '7d' },
  { text: '最近30天', value: '30d' },
]

const intervalOptions = [
  { text: '2分钟(测试)', value: 2 },
  { text: '1小时', value: 60 },
  { text: '6小时', value: 360 },
  { text: '12小时', value: 720 },
  { text: '24小时', value: 1440 },
]

const onConfirmTime = ({ selectedOptions }) => {
  timeRange.value = selectedOptions[0].value
  timeDisplayName.value = selectedOptions[0].text
  showPicker.value = false
}

const onConfirmInterval = ({ selectedOptions }) => {
  interval.value = selectedOptions[0].value
  intervalDisplayName.value = selectedOptions[0].text
  showIntervalPicker.value = false
}

const handleTriggerAnalysis = async () => {
  if (!keyword.value) return showToast('请输入关键词')
  loading.value = true
  try {
    const res = await middlewareApi.createTask({
      userId: localStorage.getItem('userId') || '1',
      keywords: [keyword.value],
      time_range: timeRange.value,
      interval: interval.value
    })
    if (res && (res.taskId || res.id)) {
      currentTaskId.value = res.taskId || res.id
    }
    showToast({ type: 'success', message: '任务已下发' })
    fetchDashboardStats()
    list.value = []
    finished.value = false
    listLoading.value = true
    onLoad()
  } catch (err) {
    showToast('任务触发失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const onLoad = async () => {
  try {
    const userId = localStorage.getItem('userId') || '1'
    const taskRes = await middlewareApi.getTaskList({ userId, page: 1, size: 100 })
    const tasks = taskRes.list || []
    if (tasks.length === 0) {
      list.value = []
      listLoading.value = false
      finished.value = true
      return
    }
    const reportPromises = tasks.map(task =>
      middlewareApi.getReportList({
        taskId: task.taskId || task.id,
        userId: userId,
        page: 1,
        size: 20
      }).catch(() => null)
    )
    const results = await Promise.all(reportPromises)
    let aggregatedReports = []
    results.forEach(res => {
      if (res && res.list) {
        aggregatedReports = aggregatedReports.concat(res.list)
      }
    })
    aggregatedReports.sort((a, b) => {
      const timeA = new Date(a.createTime || a.created_at || 0).getTime()
      const timeB = new Date(b.createTime || b.created_at || 0).getTime()
      return timeB - timeA
    })
    if (aggregatedReports.length > 0) {
      list.value = aggregatedReports.map(item => ({
        ...item,
        id: item.id || item.reportId,
        title: item.name || item.reportName,
        status: item.status,
        heat: 0,
        sentiment: 'neutral',
        createTime: item.createTime || item.created_at
      }))
    } else {
      list.value = []
    }
  } catch (err) {
    console.error(err)
  } finally {
    listLoading.value = false
    finished.value = true
  }
}

const goToReport = (id) => {
  router.push(`/report/${id}`)
}

const goToMonitorDetail = (taskId) => {
  router.push({
    path: `/monitor/${taskId}`,
    query: {
      fromTab: activeTab.value,
      fromMode: monitorMode.value
    }
  })
}

// 处理 Openclaw 任务跳转
const handleOpenclawDetail = (task) => {
  router.push({
    path: `/openclaw-task/${task.id}`,
    query: {
      fromTab: activeTab.value,
      fromMode: monitorMode.value
    }
  })
}

// 编辑任务
const editTask = (task) => {
  editingTask.value = task
  editKeyword.value = task.keywords ? task.keywords[0] : ''
  editTimeRange.value = task.timeRange || task.time_range || '24h'
  const timeOption = timeOptions.find(opt => opt.value === editTimeRange.value)
  editTimeDisplayName.value = timeOption ? timeOption.text : '最近24小时'
  editInterval.value = task.interval || 60
  const intervalOption = intervalOptions.find(opt => opt.value === editInterval.value)
  editIntervalDisplayName.value = intervalOption ? intervalOption.text : '1小时'
  showEditPopup.value = true
}

// 编辑时确认时间范围
const onConfirmEditTime = ({ selectedOptions }) => {
  editTimeRange.value = selectedOptions[0].value
  editTimeDisplayName.value = selectedOptions[0].text
  showEditTimePicker.value = false
}

// 编辑时确认频率
const onConfirmEditInterval = ({ selectedOptions }) => {
  editInterval.value = selectedOptions[0].value
  editIntervalDisplayName.value = selectedOptions[0].text
  showEditIntervalPicker.value = false
}

// 保存编辑
const handleSaveEdit = async () => {
  if (!editKeyword.value) return showToast('请输入关键词')
  editLoading.value = true
  try {
    const taskId = editingTask.value.taskId || editingTask.value.id
    await middlewareApi.updateTask(taskId, {
      keywords: [editKeyword.value],
      time_range: editTimeRange.value,
      interval: editInterval.value
    })
    showToast({ type: 'success', message: '更新成功' })
    showEditPopup.value = false
    // 重新加载任务列表
    const userId = localStorage.getItem('userId') || '1'
    const res = await middlewareApi.getTaskList({ userId, page: 1, size: 100 })
    taskList.value = res.list || []
    fetchDashboardStats()
  } catch (err) {
    showToast('更新失败: ' + (err.message || '未知错误'))
  } finally {
    editLoading.value = false
  }
}

// 删除任务
const deleteTask = async (task) => {
  const taskId = task.taskId || task.id
  showConfirmDialog({
    title: '确认删除',
    message: `确认删除任务「${task.keywords.join(', ')}」吗？`
  }).then(async () => {
    try {
      await middlewareApi.deleteTask(taskId)
      showToast('删除成功')
      // 重新加载任务列表
      const userId = localStorage.getItem('userId') || '1'
      const res = await middlewareApi.getTaskList({ userId, page: 1, size: 100 })
      taskList.value = res.list || []
      fetchDashboardStats()
    } catch (err) {
      showToast('删除失败')
    }
  }).catch(() => {})
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToUserCenter = () => {
  activeTab.value = 'mine'
}

const fetchDashboardStats = async () => {
  const userId = localStorage.getItem('userId') || '1'
  try {
    const taskRes = await middlewareApi.getTaskList({ userId, page: 1, size: 100 })
    let allTasks = []
    if (taskRes && taskRes.total !== undefined) {
      monitoringCount.value = taskRes.total
      allTasks = taskRes.list || []
    } else if (taskRes && taskRes.list) {
      monitoringCount.value = taskRes.list.length
      allTasks = taskRes.list
    }
    if (taskRes && taskRes.list) {
      taskList.value = taskRes.list
    }
    if (allTasks.length > 0) {
      const today = new Date().toDateString()
      const reportPromises = allTasks.map(task =>
        middlewareApi.getReportList({
          taskId: task.taskId || task.id,
          userId,
          page: 1,
          size: 50
        }).catch(() => ({ list: [] }))
      )
      const results = await Promise.all(reportPromises)
      let totalToday = 0
      results.forEach(res => {
        if (res && res.list) {
          const count = res.list.filter(item => {
            if (!item.createTime && !item.created_at) return false
            const t = item.createTime || item.created_at
            const itemDate = new Date(t).toDateString()
            return itemDate === today
          }).length
          totalToday += count
        }
      })
      todayReportCount.value = totalToday
    } else {
      todayReportCount.value = 0
    }
  } catch (e) {
    console.error("Failed to fetch dashboard stats", e)
  }
}
</script>

<style scoped>
.tiimo-dashboard {
  padding: 24px 20px 0 20px;
  background-color: #ffffff;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, Segoe UI, Arial, Roboto, 'PingFang SC', 'miui', 'Hiragino Sans GB', 'Microsoft Yahei', sans-serif;
}

/* Header */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: transparent;
  padding: 10px 0;
  transition: all 0.3s ease-in-out;
}

.header.header-scrolled {
  padding: 5px 0;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  margin-bottom: 24px;
}
.status-pill {
  background-color: #ffffff;
  padding: 6px 8px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  transition: all 0.3s ease;
  opacity: 1;
  transform: translateX(0);
}

.status-pill :deep(.van-icon) {
  font-size: 22px;
}

.header.header-scrolled .status-pill {
  opacity: 0;
  transform: translateX(-20px);
  pointer-events: none;
}

.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
}
.date-text { display: flex; flex-direction: column; align-items: center; }
.weekday {
    font-size: 18px;
    font-weight: bold;
    color: #000;
    transition: all 0.3s ease;
    max-height: 30px;
    opacity: 1;
}
.header.header-scrolled .weekday {
    max-height: 0;
    opacity: 0;
    margin: 0;
    transform: translateY(-10px);
}
.full-date { font-size: 12px; color: #666; margin-top: 2px; }
.ml-2 { margin-left: 16px; }
/* 模式切换按钮样式 */
.monitor-mode-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 40px;
  width: fit-content;
}
.mode-btn {
  padding: 8px 24px;
  border-radius: 32px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}
.mode-btn.active {
  background: #fff;
  color: #000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
/* Animations */
.animate-fade-in {
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Widgets */
.widget-scroll-container {
  display: flex;
  overflow-x: auto;
  gap: 16px;
  padding-bottom: 24px;
  padding: 4px;
  margin-bottom: 24px;
  -ms-overflow-style: none; scrollbar-width: none;
}
.widget-scroll-container::-webkit-scrollbar { display: none; }

.widget-card {
  flex: 1;
  min-width: 45%;
  height: 140px;
  border-radius: 28px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
  text-align: left;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(0,0,0,0.04);
  background-color: #fff;
  transition: transform 0.2s;
}
.widget-card:active {
  transform: scale(0.98);
}

.widget-card.purple {
    background: linear-gradient(135deg, #fdfbfd 0%, #f3e5f5 100%);
    border: 1px solid rgba(243, 229, 245, 0.5);
}
.widget-card.purple .widget-icon-circle {
    background-color: #ede7f6;
    color: #7e57c2;
}
.widget-card.purple .stat-num { color: #512da8; }

.widget-card.blue {
    background: linear-gradient(135deg, #fbfdff 0%, #e3f2fd 100%);
    border: 1px solid rgba(227, 242, 253, 0.5);
}
.widget-card.blue .widget-icon-circle {
    background-color: #e1f5fe;
    color: #039be5;
}

.widget-card.blue .stat-num { color: #0277bd; }
.widget-icon-circle {
  width: 44px; height: 44px;
  border-radius: 14px;
  display: flex; justify-content: center; align-items: center;
  margin-bottom: 0px;
}

.widget-text {
    width: 100%;
}

.stat-num { font-size: 32px; font-weight: 800; line-height: 1.1; margin-bottom: 2px; }
.text-risk { color: #d32f2f; }
.stat-label { font-size: 14px; color: #666; font-weight: 500;}

/* Sections */
.task-sections { margin-top: 24px; padding-bottom: 90px;}
.section-header {
  display: flex; align-items: center;
  font-size: 18px; font-weight: 600; color: #333; width: 100%;
  margin-bottom: 16px;
}
.section-title { margin-right: 4px; }
.count-badge { color: #999; font-weight: normal; font-size: 14px; }
.mr-2 { margin-right: 8px; }

/* 监测任务列表容器间距 */
.animate-list-container {
  margin-top: 24px;
}

/* Custom Vant Overrides */
:deep(.van-cell) { padding-left: 0; padding-right: 0; background: transparent; }
:deep(.van-collapse-item__content) { padding: 0 0 16px 0; background: transparent; }

/* Custom Card inside Collapse */
.custom-card-content {
    background: #fff;
    border-radius: 24px;
    padding: 24px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.04);
    border: 1px solid rgba(0,0,0,0.03);
}
.custom-field {
    background: #f7f8fa;
    border-radius: 16px;
    margin-bottom: 16px;
    padding: 16px 20px;
}
.action-btn {
    border-radius: 16px;
    font-weight: 600;
    height: 52px;
    font-size: 16px;
    background: linear-gradient(to right, #242424, #1a1a1a);
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    border: none;
    transition: transform 0.2s;
}
.action-btn:active {
    transform: scale(0.98);
}

/* List Items */
.report-item {
    display: flex;
    align-items: center;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 20px;
    padding: 20px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.02);
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.6s cubic-bezier(0.22, 1, 0.36, 1), transform 0.6s cubic-bezier(0.22, 1, 0.36, 1), background-color 0.2s, box-shadow 0.2s;
}

.report-item-hidden {
    opacity: 0;
    transform: translateY(40px);
}

.report-item:active { background: #f5f5f5; }
.report-icon {
    width: 48px; height: 48px;
    background: #f7f8fa;
    border-radius: 16px;
    display: flex; justify-content: center; align-items: center;
    margin-right: 16px;
}
.report-info { flex: 1; }
.report-title { font-weight: 600; font-size: 16px; margin-bottom: 6px; color: #333; }
.report-meta { font-size: 13px; color: #999; }
.task-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
}

/* Tabbar */
:deep(.custom-tabbar) {
    margin: 0 24px 24px;
    width: auto !important;
    left: 0;
    right: 0;
    border-radius: 32px;
    box-shadow: 0 4px 24px rgba(0,0,0,0.06);
    height: 64px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(20px);
}

:deep(.van-tabbar-item) {
    background: transparent;
    transition: all 0.3s;
    border-radius: 24px;
    margin: 6px;
}

:deep(.van-tabbar-item--active) {
    background-color: #f0f0f0;
    font-weight: 600;
}
</style>
