import { request } from '@/utils/request'
import mockApi from './mock'

// 开发环境默认启用 mock，也支持通过 VITE_USE_MOCK 显式覆盖。
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true' || (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK !== 'false')

/**
 * 中台调度层接口 (Python Middleware)
 * 对应架构中的核心业务流程
 */
export default {

    // ==========================================
    // 2.1 用户管理 (User Management)
    // ==========================================

    /**
     * 登录
     * @param {Object} data { username, password }
     */
    login(data) {
        if (USE_MOCK) return mockApi.login(data)
        return request('/user/login', {
            method: 'POST',
            data
        })
    },

    /**
     * 获取用户列表
     * @param {Object} params { userId, page, size }
     */
    getUserList(params) {
        if (USE_MOCK) return mockApi.getUserList(params)
        return request('/user/list', {
            method: 'GET',
            data: params
        })
    },

    /**
     * 添加/注册用户
     * @param {Object} data { username, password, email, role }
     */
    addUser(data) {
        if (USE_MOCK) return mockApi.addUser(data)
        return request('/user/add', {
            method: 'POST',
            data
        })
    },

    /**
     * 更新用户信息
     * @param {String} id 用户ID
     * @param {Object} data 更新内容 { username, email, role }
     */
    updateUser(id, data) {
        if (USE_MOCK) return mockApi.updateUser(id, data)
        return request(`/user/update`, {
            method: 'POST',
            data: { id, ...data }
        })
    },

    /**
     * 删除用户
     * @param {String} id
     */
    deleteUser(id) {
        if (USE_MOCK) return mockApi.deleteUser(id)
        return request('/user/delete', {
            method: 'POST',
            data: { id }
        })
    },

    // ==========================================
    // 2.2 监测任务管理 (Task Management)
    // ==========================================

    /**
     * 创建新的监测任务
     * @param {Object} data { userId, keywords, time_range, interval }
     */
    createTask(data) {
        if (USE_MOCK) return mockApi.createTask(data)

        // Transform data for backend
        const now = new Date();
        let startTime = new Date();

        if (data.time_range === '7d') {
            startTime.setDate(now.getDate() - 7);
        } else if (data.time_range === '30d') {
            startTime.setDate(now.getDate() - 30);
        } else {
            // Default 24h
            startTime.setHours(now.getHours() - 24);
        }

        const format = (d) => {
            const pad = (n) => n.toString().padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
        }

        const payload = {
            userId: data.userId, // Add userId
            keywords: data.keywords,
            startTime: format(startTime),
            endTime: format(now),
            interval: data.interval ? parseInt(data.interval) : 60 // Use provided interval or default 60
        }

        return request('/task/create', {
            method: 'POST',
            data: payload
        })
    },

    /**
     * 获取监测任务列表
     * @param {Object} params { userId, page, size }
     */
    getTaskList(params) {
        if (USE_MOCK) return mockApi.getTaskList(params)
        return request('/task/list', {
            method: 'GET',
            data: params
        })
    },

    /**
     * 更新指定任务的配置
     * @param {String} taskId 任务ID
     * @param {Object} data { keywords, time_range, interval }
     */
    updateTask(taskId, data) {
        if (USE_MOCK) return mockApi.updateTask(taskId, data)

        const payload = { ...data } // Copy original data

        // If time_range is provided, convert to startTime/endTime
        if (data.time_range) {
            const now = new Date();
            let startTime = new Date();

            if (data.time_range === '7d') {
                startTime.setDate(now.getDate() - 7);
            } else if (data.time_range === '30d') {
                startTime.setDate(now.getDate() - 30);
            } else {
                startTime.setHours(now.getHours() - 24);
            }

            const format = (d) => {
                const pad = (n) => n.toString().padStart(2, '0');
                return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
            }

            payload.startTime = format(startTime);
            payload.endTime = format(now);
            delete payload.time_range; // Remove client-side field
        }

        // Map interval
        if (payload.interval === '1h') payload.interval = 60;

        return request(`/task/${taskId}/update`, {
            method: 'POST',
            data: payload
        })
    },

    /**
     * 启停指定监测任务
     * @param {String} taskId 任务ID
     * @param {Boolean} enable true/false
     */
    switchTask(taskId, enable) {
        if (USE_MOCK) return mockApi.switchTask(taskId, enable)
        return request(`/task/${taskId}/switch`, {
            method: 'POST',
            data: { enable }
        })
    },

    /**
     * 删除监测任务
     * @param {String} taskId
     */
    deleteTask(taskId) {
        if (USE_MOCK) return mockApi.deleteTask(taskId)
        return request(`/task/${taskId}/delete`, {
            method: 'POST'
        })
    },

    /**
     * 获取指定任务下的切片(事件)
     * @param {Object} params { taskId, userId, page, size }
     */
    getAnalysisSlices(params) {
        if (USE_MOCK) return mockApi.getAnalysisSlices(params)
        return request('/analysis/slices', {
            method: 'GET',
            data: params
        })
    },

    // ==========================================
    // 2.3 Openclaw 定时任务管理
    // ==========================================

    /**
     * 创建 Openclaw 定时任务
     * @param {Object} data { name, url, crawlLogic, startTime, endTime, interval }
     */
    createOpenclawTask(data) {
        if (USE_MOCK) return mockApi.createOpenclawTask?.(data) || Promise.resolve({ data: 1 })
        return request('/openclaw-task/create', {
            method: 'POST',
            data
        })
    },

    /**
     * 获取 Openclaw 任务列表
     * @param {Object} params { page, size }
     */
    getOpenclawTaskList(params) {
        if (USE_MOCK) return mockApi.getOpenclawTaskList?.(params) || Promise.resolve({ list: [], total: 0 })
        return request('/openclaw-task/list', {
            method: 'GET',
            data: params
        })
    },

    /**
     * 更新 Openclaw 任务
     * @param {Number|String} taskId
     * @param {Object} data { name, url, crawlLogic, interval }
     */
    updateOpenclawTask(taskId, data) {
        if (USE_MOCK) return mockApi.updateOpenclawTask?.(taskId, data) || Promise.resolve(true)
        return request(`/openclaw-task/${taskId}/update`, {
            method: 'POST',
            data
        })
    },

    /**
     * 启停 Openclaw 任务
     * @param {Number|String} taskId
     * @param {Boolean} enable true/false
     */
    switchOpenclawTask(taskId, enable) {
        if (USE_MOCK) return mockApi.switchOpenclawTask?.(taskId, enable) || Promise.resolve({ status: enable ? 'ACTIVE' : 'INACTIVE' })
        return request(`/openclaw-task/${taskId}/switch`, {
            method: 'POST',
            data: { enable }
        })
    },

    /**
     * 删除 Openclaw 任务
     * @param {Number|String} taskId
     */
    deleteOpenclawTask(taskId) {
        if (USE_MOCK) return mockApi.deleteOpenclawTask?.(taskId) || Promise.resolve(true)
        return request(`/openclaw-task/${taskId}/delete`, {
            method: 'POST'
        })
    },


    // ==========================================
    // 2.4 资讯数据 (News / Record)
    // ==========================================

    /**
     * 获取资讯列表
     * @param {Object} params { sliceId, taskId, userId, keyword, page, size }
     */
    getNewsList(params) {
        if (USE_MOCK) return mockApi.getNewsList(params)
        // 将 eventId 转换为 sliceId（兼容旧代码）
        const normalizedParams = { ...params }
        if (normalizedParams.eventId && !normalizedParams.sliceId) {
            normalizedParams.sliceId = normalizedParams.eventId
            delete normalizedParams.eventId
        }
        return request('/news/list', {
            method: 'GET',
            data: normalizedParams
        })
    },

    // ==========================================
    // 2.5 报告管理 (Report)
    // ==========================================

    /**
     * 获取指定任务下的报告历史列表
     * @param {Object} params { userId, taskId, page, size }
     */
    getReportList(params) {
        if (USE_MOCK) return mockApi.getReportList(params)
        return request('/report/list', {
            method: 'GET',
            data: params
        })
    },

    /**
     * 获取单份报告详情
     * @param {String} id reportId
     */
    getReportDetail(id) {
        if (USE_MOCK) return mockApi.getReportDetail(id)
        return request(`/report/${id}`, {
            method: 'GET'
        })
    },

    /**
     * 删除报告
     * @param {String} id reportId
     */
    deleteReport(id) {
        if (USE_MOCK) return mockApi.deleteReport(id)
        return request(`/report/${id}`, {
            method: 'DELETE'
        })
    },

    // ==========================================
    // 2.6 AI 智能问答 (AI Q&A)
    // ==========================================

    /**
     * 基于报告问答
     * @param {Object} data { question, reportId }
     */
    askReport(data) {
        if (USE_MOCK) return mockApi.askReport(data)
        return request('/qa/ask/report', {
            method: 'POST',
            data
        })
    },

    /**
     * 基于事件链分析问答
     * @param {Object} data { question, eventId }
     */
    askEvent(data) {
        if (USE_MOCK) return mockApi.askEvent(data)
        return request('/qa/ask/event', {
            method: 'POST',
            data
        })
    }
}
