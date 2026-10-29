# InsightMon · 智眸

面向舆情监测的服务化迭代版本，拆分业务、网关、采集、模型与前端模块。

## 模块

| 目录 | 用途与技术 |
| --- | --- |
| [insightmon_backend](./insightmon_backend/) | Java / Spring Boot / Spring Cloud，业务服务与网关 |
| [insightmon_frontend](./insightmon_frontend/) | Vue 3 / Vite / Element Plus / ECharts，Web管理与可视化 |
| [insightmon_crawler](./insightmon_crawler/) | Python，多源内容采集 |
| [insightmon_model](./insightmon_model/) | Python / HanLP，内容清洗与分析 |

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

## 模型资源

预训练模型权重与原始训练数据未收录。模型代码引用HanLP本地资源时，需要自行准备对应资源并调整路径。

## 第三方组件

第三方依赖与引用组件遵循各自许可证；本仓库不改变其使用条件。
