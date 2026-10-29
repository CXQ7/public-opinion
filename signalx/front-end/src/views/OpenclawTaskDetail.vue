<template>
  <div class="openclaw-detail-page">
    <van-nav-bar
      title="定时任务详情"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
      class="custom-nav-bar"
    />

    <div class="content">
      <!-- 任务元信息卡片 -->
      <div class="task-meta-card animate-fade-in" v-if="taskInfo">
        <div class="card-bg-decoration"></div>
        <div class="meta-header">
          <div class="meta-tag blue">Openclaw 定向采集</div>
          <div class="meta-status-btn" @click="handleSwitchTask" :class="{ active: taskInfo.status === 1 }">
            <div class="status-dot"></div>
            <span>{{ taskInfo.status === 1 ? '运行中' : '已停用' }}</span>
          </div>
        </div>
        <div class="keyword-section">
          <h1 class="keyword-title">{{ taskInfo.name }}</h1>
          <div class="keyword-subtitle">定时爬取任务</div>
        </div>

        <div class="meta-stats">
          <div class="stat-item">
            <span class="label">任务ID</span>
            <span class="value">{{ taskInfo.id }}</span>
          </div>
          <div class="divider"></div>
          <div class="stat-item">
            <span class="label">创建时间</span>
            <span class="value">{{ formatDate(taskInfo.createTime) }}</span>
          </div>
        </div>

        <div class="detail-info-grid">
          <div class="info-row">
            <div class="info-label">目标网址</div>
            <div class="info-value"><a :href="taskInfo.url" target="_blank">{{ taskInfo.url }}</a></div>
          </div>
          <div class="info-row">
            <div class="info-label">爬取逻辑</div>
            <div class="info-value">{{ taskInfo.crawlLogic || '未设置' }}</div>
          </div>
          <div class="info-row">
            <div class="info-label">时间范围</div>
            <div class="info-value">{{ formatDateRange(taskInfo.startTime, taskInfo.endTime) }}</div>
          </div>
          <div class="info-row">
            <div class="info-label">生成频率</div>
            <div class="info-value">{{ formatInterval(taskInfo.interval) }}</div>
          </div>
        </div>
      </div>

      <!-- 选项卡：报告 & 资讯 -->
      <van-tabs v-model:active="tabActive" class="custom-tabs" sticky>
        <!-- 报告标签页 -->
        <van-tab title="报告" name="reports">
          <div class="section-title" style="margin-top: 16px;">
            <van-icon name="notes-o" class="mr-2" color="#1989fa" />
            <span>历史报告</span>
            <span class="count-badge" v-if="reportTotal > 0">({{ reportTotal }})</span>
          </div>

          <div class="report-list-container">
            <van-list
              v-model:loading="reportListLoading"
              :finished="reportFinished"
              finished-text="没有更多了"
              @load="loadReportList"
            >
              <div
                v-for="item in reportList"
                :key="item.id"
                class="report-item"
                @click="goToReport(item.id)"
              >
                <div class="report-icon">
                  <van-icon
                    :name="getReportIcon(item.sentiment)"
                    :color="getReportIconColor(item.sentiment)"
                    size="24"
                  />
                </div>
                <div class="report-info">
                  <div class="report-title">{{ item.name || '未命名报告' }}</div>
                  <div class="report-meta">{{ formatDateTime(item.createTime) }}</div>
                </div>
                <div class="report-arrow">
                  <van-icon name="arrow" color="#ccc" />
                </div>
              </div>
              <van-empty v-if="reportList.length === 0 && !reportListLoading" description="暂无报告生成" />
            </van-list>
          </div>
        </van-tab>

        <!-- 资讯标签页 -->
        <van-tab title="资讯" name="records">
          <div class="section-title" style="margin-top: 16px;">
            <van-icon name="newspaper-o" class="mr-2" color="#1989fa" />
            <span>原始资讯</span>
            <span class="count-badge" v-if="records.length > 0">({{ records.length }})</span>
          </div>

          <van-empty v-if="records.length === 0 && !recordsLoading" description="暂无采集数据" />

          <div class="records-list" v-else>
            <div
              v-for="item in records"
              :key="item.id"
              class="record-item"
              @click="openUrl(item.url)"
            >
              <div class="record-header">
                <h4>{{ item.title }}</h4>
                <span class="record-time">{{ formatDateTime(item.publishTime) }}</span>
              </div>
              <p class="record-summary">{{ item.content?.substring(0, 120) }}...</p>
              <div class="record-footer">
                <van-tag size="mini" plain>{{ item.source }}</van-tag>
                <van-tag
                  :type="getSentimentTagType(item.sentiment)"
                  size="mini"
                  plain
                >
                  {{ getSentimentText(item.sentiment) }}
                </van-tag>
              </div>
            </div>
          </div>
        </van-tab>
      </van-tabs>

      <div class="spacer"></div>
    </div>

    <!-- 底部操作按钮 -->
    <div class="fixed-bottom-actions" v-if="taskInfo">
      <van-button
        plain
        @click="handleEdit"
        class="action-btn edit-btn"
      >
        <van-icon name="edit" /> 编辑任务
      </van-button>
      <van-button
        type="primary"
        color="#000"
        @click="handleDelete"
        class="action-btn delete-btn"
      >
        <van-icon name="delete-o" /> 删除任务
      </van-button>
    </div>

    <!-- 编辑任务弹窗 -->
    <van-dialog
      v-model:show="showEditDialog"
      title="编辑任务"
      show-cancel-button
      @confirm="handleSaveEdit"
      confirm-button-color="#1989fa"
    >
      <van-form>
        <van-field
          v-model="editingForm.name"
          name="name"
          label="任务名称"
          placeholder="请输入任务名称"
          :rules="[{ required: true, message: '请输入任务名称' }]"
        />
        <van-field
          v-model="editingForm.url"
          name="url"
          label="目标网址"
          placeholder="请输入要爬取的网站地址"
          type="url"
          :rules="[{ required: true, message: '请输入目标网址' }]"
        />
        <van-field
          v-model="editingForm.crawlLogic"
          name="crawlLogic"
          label="爬取逻辑"
          type="textarea"
          rows="3"
          placeholder="请描述爬取逻辑，例如：采集该页面下所有新闻标题、发布日期及详情页内容"
        />
        <van-field
          v-model="editingForm.interval"
          name="interval"
          label="生成频率"
          type="number"
          placeholder="请输入间隔分钟数"
          :rules="[{ required: true, message: '请输入生成频率' }]"
        >
          <template #right>
            <span style="color: #999; font-size: 12px;">分钟</span>
          </template>
        </van-field>
        <van-field
          v-model="editingForm.startTime"
          name="startTime"
          label="开始时间"
          type="date"
          placeholder="请选择开始时间"
        />
        <van-field
          v-model="editingForm.endTime"
          name="endTime"
          label="结束时间"
          type="date"
          placeholder="请选择结束时间"
        />
      </van-form>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import middlewareApi from '@/api/middleware'
import { formatInterval } from '@/utils'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId

// 页面数据
const taskInfo = ref(null)
const records = ref([])
const recordsLoading = ref(false)
const tabActive = ref('reports')

// 编辑弹窗相关
const showEditDialog = ref(false)
const editingForm = ref({
  name: '',
  url: '',
  crawlLogic: '',
  interval: 60,
  startTime: '',
  endTime: ''
})

// 报告相关
const reportList = ref([])
const reportListLoading = ref(false)
const reportFinished = ref(false)
const reportPage = ref(1)
const reportTotal = ref(0)
const pageSize = 20

// 工具函数
const formatDate = (isoStr) => {
  if (!isoStr) return '-'
  try {
    const date = new Date(isoStr)
    return `${date.getFullYear()}.${(date.getMonth() + 1).toString().padStart(2, '0')}.${date.getDate().toString().padStart(2, '0')}`
  } catch {
    return isoStr
  }
}

const formatDateTime = (isoStr) => {
  if (!isoStr) return '-'
  try {
    const date = new Date(isoStr)
    const pad = (n) => n.toString().padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  } catch {
    return isoStr
  }
}

const formatDateRange = (start, end) => {
  if (!start || !end) return '-'
  return `${formatDate(start)} 至 ${formatDate(end)}`
}



const getSentimentText = (sentiment) => {
  const s = String(sentiment).toLowerCase()
  if (s.includes('positive')) return '正面'
  if (s.includes('negative')) return '负面'
  return '中性'
}

const getSentimentTagType = (sentiment) => {
  const s = String(sentiment).toLowerCase()
  if (s.includes('positive')) return 'success'
  if (s.includes('negative')) return 'danger'
  return 'primary'
}

const getReportIcon = (sentiment) => {
  const s = String(sentiment || '').toLowerCase()
  if (s.includes('negative')) return 'warn-o'
  if (s.includes('positive')) return 'smile-o'
  return 'info-o'
}

const getReportIconColor = (sentiment) => {
  const s = String(sentiment || '').toLowerCase()
  if (s.includes('negative')) return '#ff605e'
  if (s.includes('positive')) return '#4caf50'
  return '#1989fa'
}

const openUrl = (url) => {
  if (url) window.open(url, '_blank')
  else showToast('链接无效')
}

// 加载任务详情
const loadTaskDetail = async () => {
  if (!taskId || taskId === 'undefined') {
    showToast('任务 ID 无效')
    return
  }

  try {
    const res = await middlewareApi.getOpenclawTaskList({ page: 1, size: 100 })

    if (!res || !res.list || res.list.length === 0) {
      showToast('暂无任务数据')
      return
    }

    const found = res.list.find(t => String(t.id) === String(taskId))

    if (found) {
      // 数据格式转换，确保字段存在
      taskInfo.value = {
        ...found,
        crawlLogic: found.crawlLogic || found.logic || found.description || '未设置'
      }
    } else {
      showToast('任务不存在或已被删除')
    }
  } catch (err) {
    showToast('加载失败：' + (err.message || '未知错误'))
  }
}

// 加载报告列表
const loadReportList = async () => {
  if (!taskId || taskId === 'undefined') {
    reportFinished.value = true
    return
  }

  reportListLoading.value = true
  try {
    const params = {
      taskId,
      taskType: 1,
      page: reportPage.value,
      size: pageSize
    }
    const res = await middlewareApi.getReportList(params)

    const newList = (res.list || []).map(item => ({
      ...item,
      id: item.id || item.reportId,
      name: item.name || item.reportName,
      createTime: item.createTime || item.created_at
    }))

    if (reportPage.value === 1) {
      reportList.value = newList
    } else {
      reportList.value = [...reportList.value, ...newList]
    }

    if (res.total !== undefined) {
      reportTotal.value = res.total
    }

    if (newList.length < pageSize) {
      reportFinished.value = true
    } else {
      reportPage.value++
    }
  } catch (error) {
    showToast('加载报告列表失败')
    reportFinished.value = true
  } finally {
    reportListLoading.value = false
  }
}

// 重置报告列表
const resetReportList = () => {
  reportList.value = []
  reportPage.value = 1
  reportFinished.value = false
  if (tabActive.value === 'reports') {
    loadReportList()
  }
}

// 加载原始资讯
const loadRecords = async () => {
  if (!taskId) return
  recordsLoading.value = true
  try {
    const res = await middlewareApi.getNewsList({
      taskId,
      taskType: 1,
      page: 1,
      size: 50
    })
    console.log('资讯列表响应:', res)

    const uniqueMap = new Map()
    ;(res.list || []).forEach(record => {
      const key = record.id || `${record.title}_${record.url || ''}_${record.publishTime || ''}`
      if (!uniqueMap.has(key)) {
        uniqueMap.set(key, {
          ...record,
          publishTime: record.publishTime || record.createTime || new Date().toISOString()
        })
      }
    })

    records.value = Array.from(uniqueMap.values())

    // 按发布时间排序
    records.value.sort((a, b) => {
      const timeA = new Date(a.publishTime || a.createTime || 0).getTime()
      const timeB = new Date(b.publishTime || b.createTime || 0).getTime()
      return timeB - timeA
    })

    records.value = records.value.map(record => ({
      ...record,
      source: record.source || '未知来源',
      sentiment: record.sentiment || 'neutral'
    }))

  } catch (err) {
    console.error('加载资讯失败', err)
    showToast('加载资讯失败')
  } finally {
    recordsLoading.value = false
  }
}

// 启停任务
const handleSwitchTask = async () => {
  if (!taskInfo.value) return
  const newEnable = taskInfo.value.status !== 1
  try {
    await middlewareApi.switchOpenclawTask(taskInfo.value.id, newEnable)
    taskInfo.value.status = newEnable ? 1 : 0
    showToast(newEnable ? '已启用' : '已停用')
    window.dispatchEvent(new CustomEvent('refresh-openclaw-list'))
  } catch (err) {
    showToast('操作失败')
  }
}

// 编辑任务
const handleEdit = () => {
  if (!taskInfo.value) return

  // 填充表单数据
  editingForm.value = {
    name: taskInfo.value.name || '',
    url: taskInfo.value.url || '',
    crawlLogic: taskInfo.value.crawlLogic || '',
    interval: taskInfo.value.interval || 60,
    startTime: taskInfo.value.startTime ? taskInfo.value.startTime.split('T')[0] : '',
    endTime: taskInfo.value.endTime ? taskInfo.value.endTime.split('T')[0] : ''
  }

  showEditDialog.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  // 简单验证
  if (!editingForm.value.name) {
    showToast('请输入任务名称')
    return
  }
  if (!editingForm.value.url) {
    showToast('请输入目标网址')
    return
  }

  try {
    await middlewareApi.updateOpenclawTask(taskInfo.value.id, {
      name: editingForm.value.name,
      url: editingForm.value.url,
      crawlLogic: editingForm.value.crawlLogic,
      interval: editingForm.value.interval
    })

    showToast('保存成功')
    showEditDialog.value = false

    // 重新加载任务详情
    await loadTaskDetail()
  } catch (err) {
    console.error('更新任务失败', err)
    showToast('更新失败：' + (err.message || '未知错误'))
  }
}

// 删除任务
const handleDelete = () => {
  showConfirmDialog({
    title: '确认删除',
    message: `确认删除任务「${taskInfo.value.name}」吗？`
  }).then(async () => {
    try {
      await middlewareApi.deleteOpenclawTask(taskInfo.value.id)
      showToast('删除成功')
      window.dispatchEvent(new CustomEvent('refresh-openclaw-list'))
      router.back()
    } catch (err) {
      showToast('删除失败')
    }
  }).catch(() => {})
}

// 跳转报告详情
const goToReport = (id) => {
  router.push(`/report/${id}`)
}

const onClickLeft = () => {
  const { fromTab, fromMode } = route.query
  if (fromTab && fromMode) {
    router.push({
      path: '/dashboard',
      query: { fromTab, fromMode }
    })
  } else {
    router.back()
  }
}

// 监听 tabActive 变化，当切换到报告页且无数据时加载
watch(tabActive, (newVal) => {
  if (newVal === 'reports' && reportList.value.length === 0 && !reportListLoading.value) {
    loadReportList()
  }
})

// 监听任务ID变化，重新加载所有数据
watch(() => route.params.taskId, () => {
  loadTaskDetail()
  resetReportList()
  loadRecords()
})

onMounted(() => {
  loadTaskDetail()
  loadReportList()
  loadRecords()
})
</script>

<style scoped>
.openclaw-detail-page {
  background-color: #f7f8fa;
  min-height: 100vh;
  position: relative;
  padding-bottom: 0;
}
.custom-nav-bar {
  background: #fff;
  --van-nav-bar-icon-color: #333;
  --van-nav-bar-title-text-color: #333;
  --van-nav-bar-title-font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}
.content {
  padding: 20px;
  background: #f7f8fa;
  min-height: 80vh;
  padding-bottom: 100px;
}
.task-meta-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 24px 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.03);
  position: relative;
  overflow: hidden;
}
.card-bg-decoration {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(25, 137, 250, 0.05) 0%, rgba(255, 255, 255, 0) 70%);
  border-radius: 50%;
  z-index: 0;
}
.meta-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}
.meta-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 600;
}
.meta-tag.blue {
  background: #e3f2fd;
  color: #1989fa;
}
.meta-status-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: #f0f9ff;
  color: #1989fa;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
  cursor: pointer;
  border: 1px solid rgba(25, 137, 250, 0.2);
}
.meta-status-btn .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #1989fa;
}
.meta-status-btn.active {
  background-color: #e8f5e9;
  color: #07c160;
  border-color: rgba(7, 193, 96, 0.2);
}
.meta-status-btn.active .status-dot {
  background-color: #07c160;
  animation: blink 2s infinite;
}
@keyframes blink {
  0% { opacity: 1; }
  50% { opacity: 0.6; }
  100% { opacity: 1; }
}
.keyword-section {
  position: relative;
  z-index: 1;
  margin-bottom: 20px;
}
.keyword-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  line-height: 1.4;
}
.keyword-subtitle {
  font-size: 13px;
  color: #666;
}
.meta-stats {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
  background: #f8f9fa;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  margin-bottom: 16px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
.stat-item .label {
  font-size: 12px;
  color: #999;
}
.stat-item .value {
  font-size: 15px;
  color: #333;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, sans-serif;
}
.divider {
  width: 1px;
  height: 24px;
  background: #eee;
  margin: 0 16px;
}
.detail-info-grid {
  position: relative;
  z-index: 1;
  background: #f8f9fa;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  padding: 12px 16px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  font-size: 13px;
  color: #999;
  width: 80px;
  flex-shrink: 0;
}
.info-value {
  font-size: 14px;
  color: #333;
  flex: 1;
  word-break: break-word;
  text-align: right;
}
.info-value a {
  color: #1989fa;
  text-decoration: none;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 24px 0 16px 0;
  padding-left: 12px;
  border-left: 4px solid #1989fa;
  color: #333;
  display: flex;
  align-items: center;
}
.count-badge {
  color: #999;
  font-weight: normal;
  font-size: 14px;
  margin-left: 4px;
}
.custom-tabs {
  margin-top: 20px;
}
:deep(.van-tabs__line) {
  background-color: #1989fa;
}
:deep(.van-tab) {
  font-size: 15px;
}
:deep(.van-tabs__content) {
  padding-top: 8px;
}
/* 报告列表样式 */
.report-list-container {
  margin-top: 8px;
}
.report-item {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: all 0.2s;
  cursor: pointer;
}
.report-item:active {
  background: #f5f5f5;
  transform: scale(0.98);
}
.report-icon {
  width: 48px;
  height: 48px;
  background: #f7f8fa;
  border-radius: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 16px;
}
.report-info {
  flex: 1;
}
.report-title {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 6px;
  color: #333;
}
.report-meta {
  font-size: 13px;
  color: #999;
}
.report-arrow {
  margin-left: 8px;
}
/* 资讯样式 */
.records-list {
  padding: 0 4px;
}
.record-item {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}
.record-item:active {
  background: #f8f9fa;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.record-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}
.record-header h4 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #333;
  flex: 1;
}
.record-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.record-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin: 8px 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.record-footer {
  display: flex;
  gap: 12px;
  align-items: center;
}
/* 底部按钮 */
.fixed-bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 16px 20px 24px;
  display: flex;
  gap: 12px;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.05);
  border-top: 1px solid #f0f0f0;
  z-index: 10;
}
.action-btn {
  flex: 1;
  border-radius: 40px;
  height: 48px;
  font-weight: 500;
}
.edit-btn {
  border: 1px solid #ddd;
  color: #333;
}
.delete-btn {
  background: linear-gradient(to right, #242424, #1a1a1a);
  border: none;
  color: #fff;
}
.spacer {
  height: 80px;
}
.mr-2 {
  margin-right: 8px;
}
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
</style>
