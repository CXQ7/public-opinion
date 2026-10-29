<template>
  <div class="monitor-detail-page">
    <van-nav-bar
      title="监测任务详情"
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
          <div class="meta-tag blue">全网感知</div>
          <div class="meta-status-btn" @click="handleSwitchTask" :class="{ active: isActive(taskInfo.status) }">
            <div class="status-dot"></div>
            <span>{{ isActive(taskInfo.status) ? '监测中' : '已暂停' }}</span>
          </div>
        </div>
        <div class="keyword-section">
          <h1 class="keyword-title">
            {{ taskInfo.keywords ? taskInfo.keywords.join(', ') : '未命名任务' }}
          </h1>
          <div class="keyword-subtitle">实时舆情监测任务</div>
        </div>

        <!-- 情感分布饼图 -->
        <div class="sentiment-chart-section" v-if="sentimentStats.total > 0">
          <div class="chart-header">
            <div class="chart-title">
              <van-icon name="chart-trend-o" color="#1989fa" />
              <span>情感分布</span>
            </div>
            <!-- 情感标签 -->
            <div class="sentiment-labels">
              <div class="label-item" v-if="sentimentStats.positive > 0">
                <span class="dot positive"></span>
                <span class="label-text">正面</span>
              </div>
              <div class="label-item" v-if="sentimentStats.neutral > 0">
                <span class="dot neutral"></span>
                <span class="label-text">中性</span>
              </div>
              <div class="label-item" v-if="sentimentStats.negative > 0">
                <span class="dot negative"></span>
                <span class="label-text">负面</span>
              </div>
            </div>
          </div>

          <div class="chart-container">
            <div class="pie-2d-container">
              <svg viewBox="0 0 200 200" class="pie-2d-svg">
                <!-- 绘制每个扇形 -->
                <g v-for="(slice, index) in pieSlices" :key="index">
                  <path
                    v-if="slice.angle > 0"
                    :d="getPieSlicePath(slice)"
                    :fill="getSliceColor(slice.sentiment)"
                    class="pie-slice"
                    @mouseenter="showTooltip(slice, $event)"
                    @mouseleave="hideTooltip"
                    @mousemove="moveTooltip"
                  />
                </g>
                <circle cx="100" cy="100" r="20" fill="white" />
              </svg>

              <div v-if="tooltip.visible" class="pie-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
                <div class="tooltip-title">{{ tooltip.sentiment }}</div>
                <div class="tooltip-content">
                  <span class="tooltip-count">{{ tooltip.count }}个事件</span>
                  <span class="tooltip-percentage">{{ tooltip.percentage }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="meta-stats">
          <div class="stat-item">
            <span class="label">任务ID</span>
            <span class="value">{{ taskInfo.taskId || taskInfo.id }}</span>
          </div>
          <div class="divider"></div>
          <div class="stat-item">
            <span class="label">创建时间</span>
            <span class="value">{{ formatDate(taskInfo.createTime || taskInfo.created_at) }}</span>
          </div>
        </div>
      </div>

      <!-- 选项卡：事件链 & 报告 -->
      <van-tabs v-model:active="tabActive" class="custom-tabs" sticky>
        <!-- 事件链标签页 -->
        <van-tab title="事件链" name="event">
          <div class="section-title" style="margin-top: 16px;">
            <van-icon name="cluster-o" class="mr-2" color="#1989fa" />
            <span>事件链 (Event Chain)</span>
            <span class="count-badge" v-if="events.length > 0">({{ events.length }})</span>
          </div>

          <!-- 空状态 -->
          <van-empty v-if="events.length === 0" description="暂无事件数据" />

          <!-- 时间线区域 -->
          <div class="timeline-container" v-else>
            <div
              class="timeline"
              v-for="(event, index) in sortedEvents"
              :key="event.id"
            >
              <!-- 左侧时间轴 -->
              <div class="timeline-left">
                <div
                  class="timeline-node"
                  :class="{ first: index === 0, last: index === sortedEvents.length - 1 }"
                >
                  <div class="node-dot" :class="getSentimentClass(event.sentiment)"></div>
                  <div class="node-line" v-if="index !== sortedEvents.length - 1"></div>
                </div>
              </div>

              <!-- 右侧事件卡片 -->
              <div class="timeline-right">
                <div class="event-card" @click="handleSliceClick(event)">
                  <div class="event-header">
                    <h3>{{ event.title }}</h3>
                    <span class="time-tag">{{
                      formatDateShort(event.date || event.createTime)
                    }}</span>
                  </div>
                  <p class="event-summary">{{ event.summary || '暂无事件概要' }}</p>
                  <div class="event-footer">
                    <van-tag :type="getSentimentTagType(event.sentiment)">
                      {{ getSentimentText(event.sentiment) }}
                    </van-tag>
                    <span class="heat-info">🔥 热度: {{ event.heat || 0 }}</span>
                    <span class="status-info">{{ getStatusText(event.status) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </van-tab>

        <!-- 报告标签页 -->
        <van-tab title="报告" name="report">
          <div class="section-title" style="margin-top: 16px;">
            <van-icon name="notes-o" class="mr-2" color="#1989fa" />
            <span>历史报告</span>
            <span class="count-badge" v-if="reportTotal > 0">({{ reportTotal }})</span>
          </div>

          <!-- 报告列表 -->
          <div class="report-list-container">
            <van-list
              v-model:loading="reportListLoading"
              :finished="reportFinished"
              finished-text="没有更多了"
              @load="loadReportList"
            >
              <div
                v-for="item in reportList"
                :key="item.id || item.reportId"
                class="report-item"
                @click="goToReport(item.id || item.reportId)"
              >
                <div class="report-icon">
                  <van-icon
                    :name="getReportIcon(item.sentiment)"
                    :color="getReportIconColor(item.sentiment)"
                    size="24"
                  />
                </div>
                <div class="report-info">
                  <div class="report-title">{{ getReportName(item) }}</div>
                  <div class="report-meta">{{ formatDateTime(item.createTime) }}</div>
                </div>
                <div class="report-arrow">
                  <van-icon name="arrow" color="#ccc" />
                </div>
              </div>
              <van-empty v-if="reportList.length === 0 && !reportListLoading" description="暂时还没有报告生成" />
            </van-list>
          </div>
        </van-tab>
      </van-tabs>

      <div class="spacer"></div>
    </div>

    <!-- 关联新闻面板 -->
    <van-popup v-model:show="showNewsPanel" position="bottom" round :style="{ height: '70%' }" class="news-panel-popup">
      <div class="news-panel">
        <div class="panel-header">
          <h3>相关资讯</h3>
          <div class="panel-subtitle">事件: {{ currentSlice?.title }}</div>
          <van-icon name="cross" class="close-btn" @click="showNewsPanel = false" />
        </div>
        <div class="news-list" v-if="currentNewsList.length > 0">
          <div class="news-item" v-for="news in currentNewsList" :key="news.id" @click="openNewsUrl(news.url)">
            <div class="news-source-tag">{{ news.source }}</div>
            <h4 class="news-title">{{ news.title }}</h4>
            <p class="news-preview">{{ news.content?.substring(0, 80) }}...</p>
            <div class="news-meta">
              <span class="news-time">{{ formatDate(news.publishTime) }}</span>
              <van-tag
                size="mini"
                :type="getSentimentTagType(news.sentiment)"
                plain
              >
                {{ getSentimentText(news.sentiment) }}
              </van-tag>
            </div>
          </div>
        </div>
        <van-empty v-else description="暂无相关资讯详情" image="search" />
      </div>
    </van-popup>

    <!-- AI 助手悬浮图标 -->
    <div
      class="ai-float-icon"
      @click="toggleChatPanel"
      :class="{ active: showChatPanel }"
    >
      <van-icon name="chat-o" size="24" />
      <span class="ai-badge" v-if="hasNewMessage">●</span>
    </div>

    <!-- AI 聊天面板 -->
    <transition name="slide-right">
      <div class="ai-chat-panel" v-if="showChatPanel">
        <div class="chat-panel-header">
          <div class="header-left">
            <van-icon name="chat-o" size="18" color="#1989fa" />
            <span>AI 助手</span>
          </div>
          <div class="header-right">
            <span class="online-status">在线</span>
            <van-icon name="cross" class="close-btn" @click="showChatPanel = false" />
          </div>
        </div>

        <div class="chat-content" ref="qaContentRef">
          <div
            v-for="(msg, index) in chatHistory"
            :key="index"
            :class="['msg-item', msg.role]"
          >
            <div class="avatar" v-if="msg.role === 'ai'">
              <van-icon name="service-o" size="28" color="#1989fa" />
            </div>
            <div class="bubble-container">
              <div
                class="bubble markdown-body"
                v-if="msg.role === 'ai'"
                v-html="renderMarkdown(msg.content)"
              ></div>
              <div class="bubble" v-else>{{ msg.content }}</div>
              <div class="time" v-if="msg.time">{{ msg.time }}</div>
            </div>
            <div class="avatar user" v-if="msg.role === 'user'">
              <van-icon name="user-circle-o" size="28" color="#ccc" />
            </div>
          </div>

          <!-- 加载状态 -->
          <div class="loading-dots" v-if="asking">
            <span></span><span></span><span></span>
          </div>
        </div>

        <div class="chat-input-area">
          <van-field
            v-model="question"
            placeholder="针对监测事件提问..."
            class="chat-input"
            :border="false"
            @keyup.enter="handleAsk"
            :disabled="asking"
          >
            <template #button>
              <van-button
                round
                type="primary"
                size="small"
                class="send-btn"
                @click="handleAsk"
                :loading="asking"
                :disabled="!question.trim()"
              >
                <van-icon name="arrow" />
              </van-button>
            </template>
          </van-field>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import middlewareApi from '@/api/middleware'
import { showToast } from 'vant'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId

// 事件链相关
const events = ref([])
const activeStep = ref(-1)
const taskInfo = ref(null)
const currentNewsList = ref([])
const currentSlice = ref(null)
const showNewsPanel = ref(false)

// AI 问答
const qaContentRef = ref(null)
const question = ref('')
const asking = ref(false)
const chatHistory = ref([
  { role: 'ai', content: '您好，我是监测助手，您可以询问关于本次监测任务的细节。', time: '刚刚' }
])
const showChatPanel = ref(false)
const hasNewMessage = ref(false)

// 报告相关
const tabActive = ref('event')
const reportList = ref([])
const reportListLoading = ref(false)
const reportFinished = ref(false)
const reportPage = ref(1)
const reportTotal = ref(0)
const pageSize = 20


// 饼图
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  sentiment: '',
  count: 0,
  percentage: 0
})

// ---------- 计算属性 ----------
// 事件排序
const sortedEvents = computed(() => {
  const uniqueMap = new Map()
  events.value.forEach(event => {
    const key = event.id || `${event.title}_${event.eventTime || event.createTime || ''}`
    if (!uniqueMap.has(key)) {
      uniqueMap.set(key, {
        ...event,
        date: event.eventTime || event.date || event.createTime || new Date().toISOString()
      })
    }
  })

  const uniqueEvents = Array.from(uniqueMap.values())

  // 按时间降序排序
  return uniqueEvents.sort((a, b) => {
    const timeA = new Date(a.date || a.eventTime || a.createTime || 0).getTime()
    const timeB = new Date(b.date || b.eventTime || b.createTime || 0).getTime()
    return timeB - timeA
  })
})

// 情感统计
const sentimentStats = computed(() => {
  const stats = { positive: 0, negative: 0, neutral: 0, total: 0 }
  sortedEvents.value.forEach(event => {
    const s = String(event.sentiment || '').toLowerCase()
    if (s.includes('positive')) stats.positive++
    else if (s.includes('negative')) stats.negative++
    else stats.neutral++
    stats.total++
  })
  return stats
})

// 饼图切片数据
const pieSlices = computed(() => {
  const slices = []
  const total = sentimentStats.value.total
  if (total === 0) return slices

  const sentiments = [
    { type: 'positive', count: sentimentStats.value.positive },
    { type: 'neutral', count: sentimentStats.value.neutral },
    { type: 'negative', count: sentimentStats.value.negative }
  ]

  let startAngle = 0
  sentiments.forEach(s => {
    if (s.count > 0) {
      const angle = (s.count / total) * 360
      slices.push({
        sentiment: s.type,
        count: s.count,
        percentage: ((s.count / total) * 100).toFixed(1),
        startAngle,
        endAngle: startAngle + angle,
        angle
      })
      startAngle += angle
    }
  })
  return slices
})

// ---------- 工具函数 ----------
const formatDate = isoStr => {
  if (!isoStr) return '-'
  try {
    const date = new Date(isoStr)
    return `${date.getFullYear()}.${(date.getMonth() + 1)
      .toString()
      .padStart(2, '0')}.${date.getDate().toString().padStart(2, '0')}`
  } catch {
    return isoStr
  }
}

const formatDateTime = isoStr => {
  if (!isoStr) return '-'
  try {
    const date = new Date(isoStr)
    const pad = (n) => n.toString().padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  } catch {
    return isoStr
  }
}

const formatDateShort = isoStr => {
  if (!isoStr) return ''
  try {
    const date = new Date(isoStr)
    return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${date
      .getMinutes()
      .toString()
      .padStart(2, '0')}`
  } catch {
    return isoStr
  }
}

const isActive = status => {
  return status === 1 || status === '1' || status === 'active' || status === 'running'
}

const getSentimentClass = sentiment => {
  const s = String(sentiment).toLowerCase()
  if (s.includes('positive')) return 'positive'
  if (s.includes('negative')) return 'negative'
  return 'neutral'
}

const getSentimentText = sentiment => {
  const s = String(sentiment).toLowerCase()
  if (s.includes('positive')) return '正面'
  if (s.includes('negative')) return '负面'
  return '中性'
}

const getSentimentTagType = sentiment => {
  const s = String(sentiment).toLowerCase()
  if (s.includes('positive')) return 'success'
  if (s.includes('negative')) return 'danger'
  return 'primary'
}

const getStatusText = status => {
  if (!status) return '进行中'
  if (status === 'completed' || status === '已完成') return '已完成'
  if (status === 'processing' || status === '进行中') return '进行中'
  return status
}

// 报告名称
const getReportName = (item) => {
  return  item.name
}

// 报告图标相关
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

// 饼图颜色
const getSliceColor = sentiment => {
  const map = {
    positive: '#07c160',
    neutral: '#6a9eff',
    negative: '#ee0a24'
  }
  return map[sentiment] || '#ccc'
}

// 饼图路径计算
const degToRad = deg => (deg * Math.PI) / 180

const getPieSlicePath = slice => {
  const cx = 100
  const cy = 100
  const r = 80
  const { startAngle, endAngle } = slice

  if (endAngle - startAngle >= 359.9) {
    return `M ${cx - r} ${cy} A ${r} ${r} 0 1 1 ${cx + r} ${cy} A ${r} ${r} 0 1 1 ${cx - r} ${cy} Z`
  }

  const startRad = degToRad(startAngle - 90)
  const endRad = degToRad(endAngle - 90)

  const x1 = cx + r * Math.cos(startRad)
  const y1 = cy + r * Math.sin(startRad)
  const x2 = cx + r * Math.cos(endRad)
  const y2 = cy + r * Math.sin(endRad)

  const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0

  return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`
}

// tooltip 处理
const showTooltip = (slice, event) => {
  tooltip.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    sentiment: getSentimentText(slice.sentiment),
    count: slice.count,
    percentage: slice.percentage
  }
}
const hideTooltip = () => {
  tooltip.value.visible = false
}
const moveTooltip = event => {
  if (tooltip.value.visible) {
    tooltip.value.x = event.clientX
    tooltip.value.y = event.clientY
  }
}

// ---------- AI 问答 ----------
const renderMarkdown = text => {
  try {
    return marked.parse(text)
  } catch {
    return text
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (qaContentRef.value) {
    qaContentRef.value.scrollTop = qaContentRef.value.scrollHeight
  }
}

const toggleChatPanel = () => {
  showChatPanel.value = !showChatPanel.value
  if (showChatPanel.value) {
    hasNewMessage.value = false
    nextTick(() => scrollToBottom())
  }
}

const handleAsk = async () => {
  if (!question.value.trim()) return
  if (!showChatPanel.value) {
    showChatPanel.value = true
    await nextTick()
  }

  const q = question.value
  chatHistory.value.push({ role: 'user', content: q, time: '刚刚' })
  question.value = ''
  asking.value = true
  scrollToBottom()

  try {
    let currentEventId = null
    if (events.value.length > 0 && activeStep.value >= 0) {
      currentEventId = events.value[activeStep.value]?.id
    } else if (events.value.length > 0) {
      currentEventId = events.value[0].id
    }

    if (!currentEventId) {
      chatHistory.value.push({ role: 'ai', content: '当前没有关联的事件，无法回答具体问题。', time: '刚刚' })
      return
    }

    const res = await middlewareApi.askEvent({ question: q, eventId: currentEventId })
    chatHistory.value.push({ role: 'ai', content: res.answer, time: '刚刚' })
  } catch {
    chatHistory.value.push({ role: 'ai', content: '系统繁忙，请稍后再试。', time: '刚刚' })
  } finally {
    asking.value = false
    scrollToBottom()
  }
}

// ---------- 报告相关 ----------
const loadReportList = async () => {
  if (!taskId || taskId === 'undefined') {
    reportFinished.value = true
    return
  }

  reportListLoading.value = true
  try {
    const userId = localStorage.getItem('userId') || '1'
    // 只传递后端支持的参数
    const params = {
      taskId,
      taskType: 0,  // 0 表示普通监测任务，1 表示 Openclaw 任务
      page: reportPage.value,
      size: pageSize
    }
    const res = await middlewareApi.getReportList(params)

    const newList = res.list || []
    reportTotal.value = res.total || newList.length

    if (reportPage.value === 1) {
      reportList.value = newList
    } else {
      reportList.value = [...reportList.value, ...newList]
    }

    // 判断是否还有更多数据
    if (newList.length < pageSize) {
      reportFinished.value = true
    } else {
      reportFinished.value = false
      reportPage.value++
    }
  } catch (error) {
    console.error('加载报告列表失败', error)
    showToast('加载报告列表失败')
  } finally {
    reportListLoading.value = false
  }
}

// ---------- 业务逻辑 ----------
const handleSwitchTask = async () => {
  if (!taskInfo.value) return
  const newState = !isActive(taskInfo.value.status)
  try {
    await middlewareApi.switchTask(taskId, newState)
    taskInfo.value.status = newState ? 'active' : 'paused'
    showToast(newState ? '任务已恢复' : '任务已暂停')
  } catch {
    showToast('操作失败')
  }
}

const handleSliceClick = async slice => {
  currentSlice.value = slice
  showNewsPanel.value = true
  currentNewsList.value = []

  try {
    const res = await middlewareApi.getNewsList({
      sliceId: slice.id,
      taskId,
      taskType: 0,
      page: 1,
      size: 50
    })
    console.log('资讯列表响应:', res)

    if (res?.list && res.list.length > 0) {
      const uniqueMap = new Map()
      res.list.forEach(news => {
        const key = news.id || `${news.title}_${news.url || ''}_${news.publishTime || ''}`

        if (!uniqueMap.has(key)) {
          uniqueMap.set(key, {
            ...news,
            publishTime: news.publishTime || news.createTime || new Date().toISOString()
          })
        }
      })
      currentNewsList.value = Array.from(uniqueMap.values())

      // 按发布时间排序
      currentNewsList.value.sort((a, b) => {
        const timeA = new Date(a.publishTime || a.createTime || 0).getTime()
        const timeB = new Date(b.publishTime || b.createTime || 0).getTime()
        return timeB - timeA
      })
      slice.newsCount = currentNewsList.value.length
    } else {
      showToast('暂无相关资讯')
    }
  } catch (err) {
    console.error('获取资讯失败:', err)
    showToast('获取资讯失败')
  }
}

const openNewsUrl = url => {
  if (url) window.open(url, '_blank')
  else showToast('链接无效')
}

const resetReportList = () => {
  reportList.value = []
  reportPage.value = 1
  reportFinished.value = false
  if (tabActive.value === 'report') {
    loadReportList()
  }
}

const loadData = async () => {
  if (!taskId || taskId === 'undefined') {
    showToast('任务ID无效，无法加载数据')
    return
  }

  try {
    const userId = localStorage.getItem('userId') || '1'
    const [taskRes, slicesRes] = await Promise.all([
      middlewareApi.getTaskList({ userId, page: 1, size: 100 }),
      middlewareApi.getAnalysisSlices({ userId, taskId, page: 1, size: 100 })
    ])

    if (taskRes?.list) {
      const found = taskRes.list.find(t => String(t.taskId || t.id) === String(taskId))
      taskInfo.value = found || { taskId, status: 'active' }
    }

    if (slicesRes?.list) {
      const uniqueSlicesMap = new Map()
      slicesRes.list.forEach(slice => {
        const key = slice.id || `${slice.title}_${slice.eventTime || slice.createTime || ''}`
        if (!uniqueSlicesMap.has(key)) {
          uniqueSlicesMap.set(key, {
            ...slice,
            date: slice.eventTime || slice.date || slice.createTime || new Date().toISOString(),
            summary: slice.summary || `关于"${slice.title}"的相关舆情分析`
          })
        }
      })
      events.value = Array.from(uniqueSlicesMap.values())
      console.log('事件链数据已加载，去重后数量:', events.value.length)
      activeStep.value = 0
    } else {
      events.value = []
    }

    resetReportList()
  } catch {
    showToast('数据加载失败')
  }
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

// 跳转报告详情
const goToReport = (id) => {
  router.push(`/report/${id}`)
}

// 监听
watch(chatHistory, (newVal, oldVal) => {
  if (newVal.length > oldVal.length) {
    const last = newVal[newVal.length - 1]
    if (last.role === 'ai' && !showChatPanel.value) hasNewMessage.value = true
  }
  if (showChatPanel.value) scrollToBottom()
}, { deep: true })

// 监听选项卡变化
watch(tabActive, (newVal) => {
  if (newVal === 'report' && reportList.value.length === 0 && !reportListLoading.value) {
    loadReportList()
  }
})

watch(() => route.params.taskId, loadData)

onMounted(loadData)
</script>

<style scoped>
/* ===== 全局布局 ===== */
.monitor-detail-page {
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
.spacer {
  height: 120px;
}
.mr-2 {
  margin-right: 8px;
}
.count-badge {
  color: #999;
  font-weight: normal;
  font-size: 14px;
  margin-left: 4px;
}

/* ===== 自定义选项卡样式 ===== */
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

/* ===== 任务元信息卡片 ===== */
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

/* 饼图样式 */
.sentiment-chart-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  position: relative;
  z-index: 1;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.sentiment-labels {
  display: flex;
  gap: 12px;
}
.label-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot.positive {
  background-color: #07c160;
  box-shadow: 0 2px 4px rgba(7, 193, 96, 0.3);
}
.dot.neutral {
  background-color: #6a9eff;
  box-shadow: 0 2px 4px rgba(106, 158, 255, 0.3);
}
.dot.negative {
  background-color: #ee0a24;
  box-shadow: 0 2px 4px rgba(238, 10, 36, 0.3);
}
.label-text {
  font-size: 12px;
  color: #666;
}
.chart-container {
  display: flex;
  justify-content: center;
  align-items: center;
}
.pie-2d-container {
  position: relative;
  width: 200px;
  height: 200px;
}
.pie-2d-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1));
}
.pie-slice {
  cursor: pointer;
  transition: filter 0.2s ease;
}
.pie-slice:hover {
  filter: brightness(1.1);
}
.pie-tooltip {
  position: fixed;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  pointer-events: none;
  z-index: 9999;
  transform: translate(-50%, -100%);
  margin-top: -10px;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.pie-tooltip::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 5px solid rgba(0, 0, 0, 0.8);
}
.tooltip-title {
  font-weight: 600;
  margin-bottom: 4px;
  color: #fff;
}
.tooltip-content {
  display: flex;
  gap: 12px;
  align-items: center;
}
.tooltip-count {
  color: #ddd;
}
.tooltip-percentage {
  color: #07c160;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 12px;
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

/* ===== 时间线与事件卡片 ===== */
.timeline-container {
  margin-left: 4px;
}
.timeline {
  display: flex;
  margin-bottom: 16px;
  position: relative;
}
.timeline-left {
  width: 25px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
}
.node-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #1989fa;
  transition: all 0.3s;
}
.positive .node-dot {
  background-color: #07c160;
}
.negative .node-dot {
  background-color: #ee0a24;
}
.neutral .node-dot {
  background-color: #6a9eff;
}
.node-line {
  width: 1px;
  height: calc(100% + 216px);
  background-color: #d0defc;
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1;
}
.timeline:last-child .node-line {
  display: none;
}
.timeline-right {
  flex: 1;
  padding-left: 8px;
}

/* 事件卡片 */
.event-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  margin-bottom: 8px;
}
.event-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: #e0e0e0;
}
.event-header {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.event-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  flex: 1 1 auto;
  min-width: 200px;
  word-break: break-word;
}
.time-tag {
  font-size: 14px ;
  color: #666 ;
  display: inline-block;
  line-height: 1.4 ;
  flex: 0 0 auto ;
}
.event-summary {
  font-size: 14px;
  color: #555;
  margin: 0 0 16px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
.event-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.heat-info {
  color: #ff976a;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}
.status-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
}
.status-info::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #07c160;
  margin-right: 2px;
}

/* ===== 报告列表样式 ===== */
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

/* ===== 新闻面板 ===== */
.news-panel-popup {
  overflow: hidden;
  background: rgba(0, 0, 0, 0.3);
}
.news-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 30px rgba(0, 0, 0, 0.1);
}
.panel-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
  background: #fff;
  flex-shrink: 0;
}
.panel-header h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.panel-subtitle {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  padding-right: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  color: #999;
  font-size: 20px;
  cursor: pointer;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f8f9fa;
}
.close-btn:active {
  background: #f0f0f0;
  transform: scale(0.95);
}
.news-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f7f8fa;
}
.news-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}
.news-item:active {
  background: #f8f9fa;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
}
.news-source-tag {
  display: inline-flex;
  align-items: center;
  background: #f0f7ff;
  color: #1989fa;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  margin-bottom: 12px;
  font-weight: 600;
  width: fit-content;
}
.news-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 12px 0;
  line-height: 1.5;
  word-break: break-word;
}
.news-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: calc(1.6em * 2);
  word-break: break-word;
}
.news-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  padding-top: 12px;
  border-top: 1px solid #f8f8f8;
  margin-top: auto;
}
.news-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== AI 助手 ===== */
.ai-float-icon {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 56px;
  height: 56px;
  background: #1989fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(25, 137, 250, 0.4);
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 1000;
}
.ai-float-icon:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(25, 137, 250, 0.5);
}
.ai-float-icon.active {
  background: #07c160;
}
.ai-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  color: #ff4444;
  font-size: 12px;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}
.ai-chat-panel {
  position: fixed;
  right: 20px;
  bottom: 90px;
  width: 360px;
  height: 500px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 999;
  border: 1px solid #f0f0f0;
}
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
.chat-panel-header {
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #333;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.online-status {
  font-size: 11px;
  color: #07c160;
  background: rgba(7, 193, 96, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}
.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9f9f9;
}
.msg-item {
  display: flex;
  margin-bottom: 20px;
  gap: 10px;
  align-items: flex-start;
}
.msg-item.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar.user {
  border: none;
  background: transparent;
}
.bubble-container {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}
.msg-item.user .bubble-container {
  align-items: flex-end;
}
.bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.5;
  word-wrap: break-word;
}
.msg-item.ai .bubble {
  background: #fff;
  color: #333;
  border-top-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}
.msg-item.user .bubble {
  background: #1989fa;
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(25, 137, 250, 0.2);
}
.time {
  font-size: 10px;
  color: #bbb;
  margin-top: 4px;
  margin-left: 4px;
}
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 16px;
  width: fit-content;
  margin-left: 42px;
}
.loading-dots span {
  width: 6px;
  height: 6px;
  background: #999;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}
.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}
@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
.chat-input-area {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.chat-input {
  background: #f5f6f8;
  border-radius: 24px;
  padding: 6px 16px;
  font-size: 13px;
}
.send-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  min-width: 32px;
  border-radius: 50%;
  background: #1989fa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.send-btn .van-icon {
  font-size: 16px;
}

/* 移动端适配 */
@media (max-width: 480px) {
  .event-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  .event-header h3 {
    min-width: 100%;
  }
  .time-tag {
    align-self: flex-start;
    margin-left: 0;
  }
  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .pie-2d-container {
    width: 180px;
    height: 180px;
  }
  .ai-chat-panel {
    width: calc(100% - 40px);
    right: 20px;
    left: 20px;
    bottom: 90px;
    height: 60vh;
  }
  .ai-float-icon {
    width: 48px;
    height: 48px;
  }
  .ai-float-icon .van-icon {
    font-size: 20px;
  }
}
</style>
