<template>
  <div class="report-page">
    <van-nav-bar
      :title="reportData.title || '舆情报告详情'"
      left-arrow
      @click-left="onClickLeft"
      class="custom-nav-bar"
      :border="false"
    />

    <!-- 加载中 -->
    <div v-if="loading" class="loading-box">
      <div class="loading-content">
        <van-loading color="#1989fa" size="36px" vertical>AI 正在深度分析中...</van-loading>
        <div class="loading-tips">这可能需要几秒钟时间</div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div v-else class="content-container">

      <!-- 报告元信息卡片 -->
      <div class="meta-card">
         <div class="meta-header">
            <div class="meta-tag blue">智能生成</div>
            <div class="meta-date">{{ formatDate(reportData.createTime) }}</div>
         </div>
         <h1 class="report-title-large">{{ reportData.title }}</h1>
         <div class="meta-stats">
            <div class="stat-item">
              <span class="label">热度指数</span>
              <span class="value">92.1</span>
            </div>
            <div class="divider"></div>
            <div class="stat-item">
              <span class="label">情感倾向</span>
              <span class="value positive">积极</span>
            </div>
         </div>
      </div>

      <!-- WF2: 综合研判 (Markdown) -->
      <div class="report-body-card">
        <div class="card-header">
            <van-icon name="description" color="#1989fa" size="18" />
            <span class="title-text">智能综述</span>
        </div>
        <div class="markdown-body" v-html="renderedMarkdown"></div>
      </div>

      <!-- 垫高底部，防止被悬浮面板遮挡 -->
      <div class="spacer"></div>
    </div>

    <!-- WF3: 智能问答 (Q&A) - 优化后的悬浮面板 -->
    <van-floating-panel v-if="!loading" :anchors="anchors" v-model:height="panelHeight">
      <div class="qa-panel">
        <div class="qa-drag-handle"></div>
        <div class="qa-header-row">
            <div class="qa-title">
                <van-icon name="chat-o" size="18" />
                <span>AI 助手</span>
            </div>
            <div class="qa-status">在线</div>
        </div>

        <div class="qa-content" ref="qaContentRef">
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['msg-item', msg.role]">
            <div class="avatar" v-if="msg.role === 'ai'">
                <van-icon name="service-o" size="32" color="#1989fa" />
            </div>
            <div class="bubble-container">
                <div class="bubble">{{ msg.content }}</div>
                <div class="time" v-if="msg.time">{{ msg.time }}</div>
            </div>
            <div class="avatar user" v-if="msg.role === 'user'">
                <van-icon name="user-circle-o" size="32" color="#ccc" />
            </div>
          </div>
        </div>

        <div class="qa-input-area">
          <van-field
            v-model="question"
            center
            clearable
            placeholder="针对本报告提问..."
            class="custom-chat-input"
            :border="false"
          >
            <template #button>
              <van-button
                size="small"
                type="primary"
                color="#1989fa"
                class="send-btn"
                @click="handleAsk"
                :loading="asking"
                :disabled="!question"
              >
                <van-icon name="guide-o" />
              </van-button>
            </template>
          </van-field>
        </div>
      </div>
    </van-floating-panel>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { marked } from 'marked'
import mermaid from 'mermaid'
import middlewareApi from '@/api/middleware'

const route = useRoute()
const router = useRouter()
const screenHeight = window.innerHeight

// 悬浮面板锚点: 收起(120px) 和 展开(60%屏幕高度)
const anchors = [120, Math.round(screenHeight * 0.6)]
const panelHeight = ref(anchors[0])
const qaContentRef = ref(null)

const loading = ref(true)
const reportData = ref({})
const question = ref('')
const asking = ref(false)
const chatHistory = ref([
  { role: 'ai', content: '您好，我是基于本报告的AI助手，有什么想了解的细节吗？', time: '刚刚' }
])

// 初始化 mermaid
mermaid.initialize({
  startOnLoad: false,
  theme: 'default',
  securityLevel: 'loose',
});

const renderedMarkdown = computed(() => {
  if (!reportData.value.summary) return '<div class="empty-state">暂无内容</div>'
  try {
    return marked.parse(reportData.value.summary)
  } catch (e) {
    console.error('Markdown parse error:', e)
    return reportData.value.summary
  }
})

// 监听渲染后的 HTML 内容变化
watch(renderedMarkdown, async () => {
  await nextTick()
  renderMermaid()
})

// 只有当面板展开时才自动滚动到底部
watch(chatHistory.value, () => {
    if (panelHeight.value > anchors[0]) {
        scrollToBottom()
    }
})

// 监听高度变化，如果是展开状态，也尝试滚动到底部
watch(panelHeight, (newH) => {
    if (newH > anchors[0]) {
        scrollToBottom()
    }
})

const scrollToBottom = async () => {
    await nextTick()
    if (qaContentRef.value) {
        qaContentRef.value.scrollTop = qaContentRef.value.scrollHeight
    }
}

const renderMermaid = async () => {
  try {
    const nodes = document.querySelectorAll('.language-mermaid, .mermaid')
    if (nodes.length > 0) {
      await mermaid.run({
        nodes: nodes
      })
    }
  } catch (err) {
    console.error('Mermaid render error:', err)
  }
}

onMounted(async () => {
  const id = route.params.id
  await fetchReport(id)
})

const onClickLeft = () => router.back()

const formatDate = (isoStr) => {
    if(!isoStr) return ''
    const date = new Date(isoStr)
    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes()}`
}

// WF2: 获取报告
const fetchReport = async (id) => {
  loading.value = true
  try {
    const res = await middlewareApi.getReportDetail(id)

    // Fix potential Mermaid render error caused by empty x-axis
    let content = res.summary || ''
    // Replace x-axis [""] with x-axis ["无数据"] or similar to avoid parser error
    content = content.replace(/x-axis\s*\[""\]/g, 'x-axis ["无数据"]')

    reportData.value = {
        summary: content,
        title: res.reportName || '未知舆情报告',
        createTime: res.createTime || new Date().toISOString(),
    }
  } catch (err) {
    console.error(err)
    showToast('获取报告失败')
  } finally {
    loading.value = false
  }
}

// WF3: 提问
const handleAsk = async () => {
  if (!question.value) return

  // 发送时自动展开面板
  if (panelHeight.value < anchors[1]) {
      panelHeight.value = anchors[1]
  }

  const q = question.value
  chatHistory.value.push({ role: 'user', content: q, time: '刚刚' })
  question.value = ''
  asking.value = true

  scrollToBottom()

  try {
    const res = await middlewareApi.askReport({
      question: q,
      reportId: route.params.id
    })
    chatHistory.value.push({ role: 'ai', content: res.answer, time: '刚刚' })
  } catch (err) {
    chatHistory.value.push({ role: 'ai', content: '系统繁忙，请稍后再试。', time: '刚刚' })
  } finally {
    asking.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: #f7f8fa;
  position: relative;
}

/* Nav Bar */
.custom-nav-bar {
    background: #fff;
    --van-nav-bar-icon-color: #333;
    --van-nav-bar-title-text-color: #333;
    --van-nav-bar-title-font-weight: 600;
}

/* Loading */
.loading-box {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 80vh;
}
.loading-content {
    text-align: center;
}
.loading-tips {
    margin-top: 16px;
    font-size: 14px;
    color: #999;
}

/* Content */
.content-container {
    padding: 16px;
    padding-bottom: 20px;
}
.spacer {
    height: 120px;
}

/* Meta Card */
.meta-card {
    background: #fff;
    border-radius: 20px;
    padding: 24px 20px;
    margin-bottom: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.meta-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}
.meta-tag {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
}
.meta-tag.blue { background: #e3f2fd; color: #1989fa; }
.meta-date { font-size: 13px; color: #999; }

.report-title-large {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.4;
    color: #333;
    margin-bottom: 24px;
}
.meta-stats {
    display: flex;
    align-items: center;
    background: #f9f9f9;
    padding: 16px;
    border-radius: 12px;
}
.stat-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
}
.divider {
    width: 1px;
    height: 24px;
    background: #eee;
}
.stat-item .label { font-size: 12px; color: #999; margin-bottom: 4px; }
.stat-item .value { font-size: 18px; font-weight: 700; color: #333; }
.stat-item .value.positive { color: #07c160; }

/* Report Body Card */
.report-body-card {
    background: #fff;
    border-radius: 20px;
    padding: 20px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.card-header {
    display: flex;
    align-items: center;
    padding-bottom: 16px;
    border-bottom: 1px solid #f5f5f5;
    margin-bottom: 16px;
}
.title-text {
    font-size: 16px;
    font-weight: 600;
    margin-left: 8px;
    color: #333;
}
/* Markdown Styles - Minimal override */
.markdown-body {
  font-size: 15px;
  line-height: 1.8;
  color: #444;
}
:deep(h1), :deep(h2), :deep(h3) { margin-top: 1.5em; margin-bottom: 0.8em; color: #222; }
:deep(p) { margin-bottom: 1em; }
:deep(img) { border-radius: 8px; max-width: 100%; }

/* QA Panel Styled */
.qa-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  /*
     van-floating-panel 本身有圆角，这里不需要重复
  */
}
.qa-drag-handle {
    width: 36px;
    height: 5px;
    background: #e0e0e0;
    border-radius: 3px;
    margin: 10px auto 4px auto;
}
.qa-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid #f5f5f5;
}
.qa-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 14px;
    color: #333;
}
.qa-status {
    font-size: 12px;
    color: #07c160;
    position: relative;
    padding-left: 10px;
}
.qa-status::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 6px;
    height: 6px;
    background: #07c160;
    border-radius: 50%;
}

.qa-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f9f9faf0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.msg-item {
  display: flex;
  gap: 12px;
  max-width: 100%;
}
.msg-item.user {
    flex-direction: row-reverse;
}

.avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    overflow: hidden;
    flex-shrink: 0;
    background: #fff;
    border: 1px solid #eee;
    display: flex;
    align-items: center;
    justify-content: center;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar.user { border: none; background: transparent; }

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
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
  word-wrap: break-word;
}
/* AI Bubble */
.msg-item.ai .bubble {
    background: #fff;
    color: #333;
    border-top-left-radius: 2px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
/* User Bubble */
.msg-item.user .bubble {
    background: #1989fa;
    color: #fff;
    border-top-right-radius: 2px;
    box-shadow: 0 2px 8px rgba(25, 137, 250, 0.2);
}

.time {
    font-size: 10px;
    color: #bbb;
    margin-top: 4px;
}

.qa-input-area {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.custom-chat-input {
    background: #f5f6f8;
    border-radius: 24px;
    padding: 6px 16px; /* Reduced vertical padding */
}
.send-btn {
    border-radius: 50%;
    width: 32px;
    height: 32px;
    padding: 0;
    min-width: 32px; /* Override Vant default */
    display: flex;
    align-items: center;
    justify-content: center;
}
</style>
