/**
 * JSBridge封装：前端 ↔ App原生通信
 */
class Bridge {
    /**
     * 调用原生方法（如打开相机、跳转原生页面）
     * @param {String} method 原生方法名
     * @param {Object} params 传参
     * @param {Function} callback 回调
     */
    callNative(method, params = {}, callback = () => { }) {
        // 适配不同App的原生注入对象
        const native = window.App || window.webkit?.messageHandlers?.App
        if (!native) {
            callback({ code: -1, msg: '当前环境不支持原生调用' })
            return
        }

        try {
            // Android直接调用，iOS用postMessage
            if (typeof native[method] === 'function') {
                native[method](JSON.stringify(params), (res) => callback(JSON.parse(res)))
            } else if (window.webkit?.messageHandlers?.App) {
                window.webkit.messageHandlers.App.postMessage({ method, params })
            }
        } catch (err) {
            callback({ code: -1, msg: `调用${method}失败` })
        }
    }

    /**
     * 注册供原生调用的前端方法
     * @param {String} method 方法名
     * @param {Function} handler 处理函数
     */
    registerHandler(method, handler) {
        window[`bridge_${method}`] = (params) => {
            handler(typeof params === 'string' ? JSON.parse(params) : params)
        }
    }
}

// 导出单例（全局唯一）
export const bridge = new Bridge()

// 示例：注册原生调用的方法（预留）
bridge.registerHandler('onTokenExpire', () => {
    localStorage.removeItem('token')
    uni.showToast({ title: '登录失效，请重新登录', icon: 'none' })
})
