# App Web Framework

这是一个基于 Vue 3 + Vite + Capacitor 的混合应用项目。

## 协作开发指南

### 1. 环境准备
- **Node.js**: v18+
- **Android Studio**: (如需开发 App)

### 2. 快速开始
```bash
# 安装依赖
npm install

# 启动 Web 开发
npm run dev

# 如果涉及原生开发：同步 Capacitor 配置
npx cap sync
npx cap open android
```

### 3. Git 提交规范
- 提交前请先拉取最新代码 `git pull`
- 尽量不在主分支 `main` 直接开发，使用特性分支
