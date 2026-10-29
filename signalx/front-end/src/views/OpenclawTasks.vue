<template>
  <div class="openclaw-tasks">
    <!-- 创建表单卡片 -->
    <div class="section-header mb-3">
      <van-icon name="gem-o" class="mr-2" color="#ff976a" />
      <span class="section-title">Openclaw 智能任务</span>
      <van-tag type="warning" size="mini">定向采集</van-tag>
    </div>
    <div class="custom-card-content animate-fade-in">
      <van-cell-group inset :border="false" style="margin:0; padding:0;">
        <van-field
          v-model="form.name"
          label="任务名称"
          placeholder="请输入任务名称"
          left-icon="edit"
          class="custom-field"
        />
        <van-field
          v-model="form.url"
          label="定制网站URL"
          placeholder="请输入网站URL"
          left-icon="link-o"
          class="custom-field"
        />
        <van-field
          v-model="form.crawlLogic"
          type="textarea"
          rows="1"
          label="爬取逻辑"
          placeholder="请输入爬取逻辑描述"
          autosize
          left-icon="description"
          class="custom-field"
        />
        <van-field
          v-model="form.startDateDisplay"
          label="开始日期"
          is-link
          readonly
          left-icon="calendar-o"
          placeholder="请选择开始日期"
          @click="openStartDatePicker"
          class="custom-field date-field"
        />
        <van-field
          v-model="form.endDateDisplay"
          label="结束日期"
          is-link
          readonly
          left-icon="calendar-o"
          placeholder="请选择结束日期"
          @click="openEndDatePicker"
          class="custom-field date-field"
        />
        <van-field
          v-model="form.intervalDisplayName"
          label="生成频率"
          is-link
          readonly
          left-icon="replay"
          placeholder="请选择生成频率"
          @click="showIntervalPicker = true"
          class="custom-field"
        />
      </van-cell-group>
      <van-button
        type="primary"
        block
        color="#000"
        class="mt-3 action-btn"
        :loading="creating"
        @click="handleCreate"
      >
        <van-icon name="play-circle-o" class="mr-1" /> 创建 Openclaw 任务
      </van-button>
    </div>

    <!-- 任务列表 -->
    <div class="section-header mb-3 mt-4">
      <van-icon name="clock-o" class="mr-2" />
      <span class="section-title">Openclaw 任务列表</span>
    </div>
    <div class="animate-list-container">
      <div class="report-list-container">
        <div class="monitor-task-list">
          <div
            v-for="task in tasks"
            :key="task.id"
            class="report-item"
            @click="goToDetail(task)"
          >
            <div class="report-icon">
              <van-icon name="clock-o" color="#ff976a" size="24" />
            </div>
            <div class="report-info">
              <div class="report-title">{{ task.name }}</div>
              <div class="report-meta">
                <div>ID: {{ task.id }} | 状态: {{ formatStatus(task.status) }}</div>
                <div style="margin-top: 4px;">创建: {{ formatTimeStr(task.createTime || task.created_at) }}</div>
              </div>
            </div>
            <div class="task-actions" @click.stop>
              <van-icon name="edit" size="18" color="#1989fa" style="margin-right: 12px;" @click.stop="openDetail(task)" />
              <van-icon name="delete-o" size="18" color="#ff4444" @click.stop="deleteTask(task)" />
            </div>
          </div>
          <van-empty v-if="tasks.length === 0" description="暂无 Openclaw 任务" />
        </div>
      </div>
    </div>

    <!-- 创建表单的日期选择器 -->
    <van-popup
      v-model:show="showStartDatePicker"
      position="bottom"
      round
      teleport="body"
    >
      <van-date-picker
        :model-value="startDatePickerValue"
        title="选择开始日期"
        :show-toolbar="true"
        @confirm="onConfirmStartDate"
        @cancel="showStartDatePicker = false"
      />
    </van-popup>

    <van-popup
      v-model:show="showEndDatePicker"
      position="bottom"
      round
      teleport="body"
    >
      <van-date-picker
        :model-value="endDatePickerValue"
        title="选择结束日期"
        :show-toolbar="true"
        @confirm="onConfirmEndDate"
        @cancel="showEndDatePicker = false"
      />
    </van-popup>

    <van-popup
      v-model:show="showIntervalPicker"
      position="bottom"
      round
      teleport="body"
    >
      <van-picker :columns="intervalOptions" @confirm="onConfirmInterval" @cancel="showIntervalPicker = false" />
    </van-popup>

    <!-- 编辑任务弹窗 -->
    <van-popup v-model:show="showEditPopup" position="bottom" round teleport="body" style="height: 70%;">
      <div style="height: 100%; display: flex; flex-direction: column;">
        <div style="padding: 16px; flex-shrink: 0;">
          <h3 style="text-align: center; margin: 0; margin-bottom: 20px;">编辑任务</h3>
        </div>
        <div style="flex: 1; overflow-y: auto; padding: 0 16px;">
          <van-cell-group inset :border="false">
            <van-field
              v-model="editForm.name"
              label="任务名称"
              left-icon="edit"
              placeholder="请输入任务名称"
            />
            <van-field
              v-model="editForm.url"
              label="定制网站URL"
              left-icon="link-o"
              placeholder="https://..."
            />
            <van-field
              v-model="editForm.crawlLogic"
              type="textarea"
              rows="2"
              label="爬取业务逻辑"
              left-icon="description"
              placeholder="描述需要采集的内容"
              autosize
            />
            <van-field
              v-model="editForm.startDateDisplay"
              label="开始日期"
              readonly
              left-icon="calendar-o"
              disabled
            />
            <van-field
              v-model="editForm.endDateDisplay"
              label="结束日期"
              readonly
              left-icon="calendar-o"
              disabled
            />
            <van-field
              v-model="editForm.intervalDisplayName"
              is-link
              readonly
              label="生成频率"
              left-icon="replay"
              placeholder="选择报告生成间隔"
              @click="showEditIntervalPicker = true"
            />
          </van-cell-group>
        </div>
        <div style="padding: 16px; flex-shrink: 0; display: flex; gap: 12px;">
          <van-button type="default" block @click="showEditPopup = false">取消</van-button>
          <van-button type="primary" color="#000" block :loading="editLoading" @click="saveDetail">保存</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 编辑时的频率选择器 -->
    <van-popup v-model:show="showEditIntervalPicker" position="bottom" round teleport="body">
      <van-picker
        :columns="intervalOptions"
        @confirm="onConfirmEditInterval"
        @cancel="showEditIntervalPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import middlewareApi from '@/api/middleware'
import { formatTimeStr, formatStatus, formatInterval } from '@/utils'

// 定义事件
const emit = defineEmits(['viewDetail'])

// 频率选项
const intervalOptions = [
  { text: '2分钟(测试)', value: 2 },
  { text: '1小时', value: 60 },
  { text: '6小时', value: 360 },
  { text: '12小时', value: 720 },
  { text: '24小时', value: 1440 },
]

// 创建表单
const form = reactive({
  name: '',
  url: '',
  crawlLogic: '',
  startDate: null,
  startDateDisplay: '',
  endDate: null,
  endDateDisplay: '',
  interval: 60,
  intervalDisplayName: '1小时'
})

const tasks = ref([])

// UI 状态
const creating = ref(false)
const showStartDatePicker = ref(false)
const showEndDatePicker = ref(false)
const showIntervalPicker = ref(false)

// 日期选择器绑定值
const startDatePickerValue = ref([])
const endDatePickerValue = ref([])

// 编辑任务相关状态
const showEditPopup = ref(false)
const showEditIntervalPicker = ref(false)
const editLoading = ref(false)
const editingTask = ref(null)
const editForm = reactive({
  name: '',
  url: '',
  crawlLogic: '',
  startDateDisplay: '',
  endDateDisplay: '',
  interval: 60,
  intervalDisplayName: '1小时'
})

// 辅助函数
const formatDateSimple = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseDateArray = (arr) => {
  if (!arr || arr.length < 3) return null
  return new Date(arr[0], arr[1] - 1, arr[2])
}

const formatDateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toISOString()
}

const openStartDatePicker = () => {
  if (form.startDate) {
    const d = new Date(form.startDate)
    startDatePickerValue.value = [d.getFullYear(), d.getMonth() + 1, d.getDate()]
  } else {
    const today = new Date()
    startDatePickerValue.value = [today.getFullYear(), today.getMonth() + 1, today.getDate()]
  }
  showStartDatePicker.value = true
}

const openEndDatePicker = () => {
  if (form.endDate) {
    const d = new Date(form.endDate)
    endDatePickerValue.value = [d.getFullYear(), d.getMonth() + 1, d.getDate()]
  } else {
    const today = new Date()
    endDatePickerValue.value = [today.getFullYear(), today.getMonth() + 1, today.getDate()]
  }
  showEndDatePicker.value = true
}

const onConfirmStartDate = ({ selectedValues }) => {
  form.startDate = parseDateArray(selectedValues)
  form.startDateDisplay = formatDateSimple(form.startDate)
  showStartDatePicker.value = false
}

const onConfirmEndDate = ({ selectedValues }) => {
  form.endDate = parseDateArray(selectedValues)
  form.endDateDisplay = formatDateSimple(form.endDate)
  showEndDatePicker.value = false
}

const onConfirmInterval = ({ selectedOptions }) => {
  form.interval = selectedOptions[0].value
  form.intervalDisplayName = selectedOptions[0].text
  showIntervalPicker.value = false
}

// 编辑时确认频率
const onConfirmEditInterval = ({ selectedOptions }) => {
  editForm.interval = selectedOptions[0].value
  editForm.intervalDisplayName = selectedOptions[0].text
  showEditIntervalPicker.value = false
}

const handleCreate = async () => {
  if (!form.name) return showToast('请输入任务名称')
  if (!form.url) return showToast('请输入网站URL')
  if (!form.startDate || !form.endDate) return showToast('请选择时间范围')
  if (form.startDate > form.endDate) return showToast('开始日期不能晚于结束日期')

  const params = {
    name: form.name,
    url: form.url,
    crawlLogic: form.crawlLogic,
    startTime: formatDateTime(form.startDate),
    endTime: formatDateTime(form.endDate),
    interval: form.interval
  }

  creating.value = true
  try {
    await middlewareApi.createOpenclawTask(params)
    showToast('创建成功')
    form.name = ''
    form.url = ''
    form.crawlLogic = ''
    form.startDate = null
    form.startDateDisplay = ''
    form.endDate = null
    form.endDateDisplay = ''
    form.interval = 60
    form.intervalDisplayName = '1小时'
    fetchTasks()
  } catch (err) {
    showToast('创建失败: ' + err.message)
  } finally {
    creating.value = false
  }
}

const fetchTasks = async () => {
  try {
    const res = await middlewareApi.getOpenclawTaskList({ page: 1, size: 100 })
    tasks.value = res.list
  } catch (err) {
    console.error(err)
  }
}

const goToDetail = (task) => {
  emit('viewDetail', task)
}

// 编辑任务
const openDetail = (task) => {
  editingTask.value = task
  editForm.name = task.name
  editForm.url = task.url || ''
  editForm.crawlLogic = task.crawlLogic || ''
  editForm.startDateDisplay = task.startTime ? formatDateSimple(task.startTime) : ''
  editForm.endDateDisplay = task.endTime ? formatDateSimple(task.endTime) : ''
  editForm.interval = task.interval
  // 找到对应的显示名称
  const intervalOption = intervalOptions.find(opt => opt.value === editForm.interval)
  editForm.intervalDisplayName = intervalOption ? intervalOption.text : '1小时'
  showEditPopup.value = true
}

// 保存编辑
const saveDetail = async () => {
  if (!editForm.name) return showToast('请输入任务名称')
  if (!editForm.url) return showToast('请输入网站 URL')
  const params = {
    name: editForm.name,
    url: editForm.url,
    crawlLogic: editForm.crawlLogic,
    interval: editForm.interval
  }

  editLoading.value = true
  try {
    await middlewareApi.updateOpenclawTask(editingTask.value.id, params)
    showToast('保存成功')
    showEditPopup.value = false
    await fetchTasks()
  } catch (err) {
    showToast('保存失败: ' + err.message)
  } finally {
    editLoading.value = false
  }
}

const deleteTask = (task) => {
  showConfirmDialog({
    title: '确认删除',
    message: `确认删除任务「${task.name}」吗？`
  }).then(async () => {
    try {
      await middlewareApi.deleteOpenclawTask(task.id)
      showToast('删除成功')
      fetchTasks()
    } catch (err) {
      showToast('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchTasks()
})

defineExpose({ fetchTasks })
</script>

<style scoped>
/* 基础样式 */
.custom-card-content {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.03);
  margin-bottom: 16px;
}
.custom-field {
  background: #f7f8fa;
  border-radius: 16px;
  margin-bottom: 16px;
  padding: 16px 20px;
}
.date-field :deep(.van-field__value) {
  overflow: visible;
  white-space: normal;
  word-break: keep-all;
}
.action-btn {
  border-radius: 16px;
  font-weight: 600;
  height: 52px;
  font-size: 16px;
  background: linear-gradient(to right, #242424, #1a1a1a);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  border: none;
  transition: transform 0.2s;
}
.action-btn:active {
  transform: scale(0.98);
}
.report-item {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  cursor: pointer;
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
  flex-shrink: 0;
}
.report-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.report-title {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}
.report-meta {
  font-size: 13px;
  color: #999;
}
.task-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 动画 */
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

/* 辅助类 */
.mb-3 { margin-bottom: 16px; }
.mt-4 { margin-top: 24px; }
.mr-2 { margin-right: 8px; }
.mr-1 { margin-right: 4px; }
.section-header {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  width: 100%;
  margin-bottom: 16px;
}
.section-title { margin-right: 4px; }
.report-list-container { margin-bottom: 20px; }
</style>
