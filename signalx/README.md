# SignalX · 讯息流谱

面向监测任务、报告与智能问答的多端应用版本，包含Android混合应用及采集调度相关工程。

## 模块

| 目录 | 用途与技术 |
| --- | --- |
| [back-end](./back-end/) | Java 17 / Spring Boot 3 / MyBatis-Plus / Redis，任务、内容、报告与问答接口 |
| [front-end](./front-end/) | Vue 3 / Vant / ECharts / Capacitor，Web与Android混合应用 |
| [crawler](./crawler/) | Python，数据源适配、采集调度与入库 |
| [platform](./platform/) | Flask，模型调用与分析页面 |
| [idl](./idl/) | 共享枚举与数据结构 |
| [proxypool](./proxypool/) | 代理池组件，见本目录许可证与上游说明 |

## 运行准备

- 前端使用Node.js与npm；具体依赖以`package.json`为准。
- Java服务使用JDK与Maven；具体版本以`pom.xml`为准。
- Python模块需独立安装其第三方依赖，按源码中的导入和模块说明准备运行环境。
- 配置本地数据库、缓存和模型服务。实际凭据、采集Cookie及部署连接参数不随源码提供。

## 配置与启动

前端在对应模块执行：

```bash
npm ci
npm run dev
```

Java服务在对应模块使用Maven构建：

```bash
mvn package -DskipTests
```

运行前请设置服务配置中的环境变量，并替换`__CONFIGURE_LOCALLY__`等配置占位值。各服务端口和依赖连接应按本地环境统一配置。构建命令仅提供入口，不代表外部服务已配置完成。

## Android

在`front-end`完成Web构建后，可运行`npx cap sync android`同步至Android工程，再使用Android Studio构建。发布签名与本机SDK路径需自行配置。

## 第三方组件

第三方依赖与引用组件遵循各自许可证；本仓库不改变其使用条件。
