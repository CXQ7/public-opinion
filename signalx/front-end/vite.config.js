import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  // 注册Vue插件
  plugins: [vue()],
  // 路径别名：@指向src目录，方便后续导入文件
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  // 开发服务器配置（调试用）
  server: {
    port: 5173, // 启动端口
    host: '0.0.0.0', // 允许局域网访问（手机调试用）
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  // 打包配置（适配App WebView）
  build: {
    outDir: 'dist', // 打包输出目录
    assetsDir: 'static' // 静态资源目录
  }
})
