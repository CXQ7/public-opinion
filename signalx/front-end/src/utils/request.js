import { showToast } from 'vant'

/**
 * 网络请求封装（App WebView适配，预留后端接口）
 */
// 从环境变量取接口基础地址（不同环境自动切换）
const BASE_URL = import.meta.env.VITE_API_BASE_URL

/**
 * 统一请求方法
 * @param {String} url 接口相对路径（如 /user/login）
 * @param {Object} options { method, data, header }
 * @returns {Promise}
 */
export const request = async (url, options = {}) => {
    const { method = 'GET', data = {}, header = {} } = options
    try {
        // 拼接完整接口地址
        let fullUrl = `${BASE_URL}${url}`
        // 构造请求配置
        const fetchOptions = {
            method,
            headers: {
                'Content-Type': 'application/json',
                // 预留Token：从本地存储获取（App WebView兼容）
                Authorization: __CONFIGURE_LOCALLY__
                ...header
            },
            timeout: 10000 // 适配App网络超时
        }

        // GET/POST参数处理
        // 获取本地存储的用户ID (User Management Requirement)
        const userId = localStorage.getItem('userId') || ''

        if (method.toUpperCase() === 'GET') {
            const params = new URLSearchParams(data)
            if (userId) {
                params.append('userId', userId)
            }
            fullUrl += `?${params.toString()}`
        } else {
            // 如果是POST，且userId存在，混入body中
            const payload = { ...data }
            if (userId) {
                payload.userId = userId
            }
            fetchOptions.body = JSON.stringify(payload)
        }

        // 发送请求
        const response = await fetch(fullUrl, fetchOptions)
        const res = await response.json()

        // 统一响应处理（预留后端状态码规则）
        if (res.code === 0) {
            return res.data
        } else {
            showToast(res.msg || '请求失败')
            throw new Error(res.msg)
        }
    } catch (err) {
        showToast(err.message || '网络异常')
        throw err
    }
}

// 简化GET/POST调用
export const get = (url, data, header) => request(url, { method: 'GET', data, header })
export const post = (url, data, header) => request(url, { method: 'POST', data, header })
