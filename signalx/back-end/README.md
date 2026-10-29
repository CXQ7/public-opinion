# SignalX-backend 部署指南

本文档详细说明了如何在全新的服务器环境（Linux/Windows）上部署 SignalX-backend 服务。

## 1. 环境准备

在开始部署之前，请确保目标服务器已安装以下基础环境：

### 1.1 基础软件
*   **JDK**: Java Development Kit 17 或更高版本 (推荐 JDK 17 LTS)
*   **Maven**: Apache Maven 3.6+ (用于构建项目)
*   **MySQL**: MySQL 5.7 或 8.0+
*   **Git**: 用于拉取代码

### 1.2 环境变量检查
确保 `java -version` 和 `mvn -v` 可以在终端正确输出版本信息。

---

## 2. 数据库初始化

1.  登录 MySQL 数据库：
    ```bash
    mysql -u root -p
    ```

2.  创建数据库 `signalx` (如果脚本中未包含创建库的语句)：
    ```sql
    CREATE DATABASE IF NOT EXISTS signalx DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
    USE signalx;
    ```

3.  导入表结构：
    找到项目目录下的 `sql/schema.sql` 文件，执行导入：
    ```bash
    # 在项目根目录下执行
    mysql -u root -p signalx < sql/schema.sql
    ```
    或者在 MySQL 命令行中执行 `source /path/to/sql/schema.sql;`

---

## 3. 项目配置

在构建项目之前，需要修改配置文件以适配当前服务器环境。

1.  打开配置文件：
    `src/main/resources/application.yml` (或 `application.properties`)

2.  **数据库配置**：
    修改 `spring.datasource` 相关节点：
    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/signalx?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
        username: your_db_username  # 替换为实际数据库用户名
        password: your_db_password  # 替换为实际数据库密码
    ```

3.  **其他配置**：
    *   检查 Redis 配置（如果有）是否需要修改 host 和 port。
    *   检查日志输出路径 `logging.file.path` 是否有写入权限。
    *   检查服务端口 `server.port` (默认通常为 8080) 是否被占用。

---

## 4. 构建项目

在 `SignalX-backend` 项目根目录下，使用 Maven 进行打包：

```bash
# 清理并打包，跳过单元测试以加快构建速度
mvn clean package -DskipTests
```

构建成功后，会在 `target` 目录下生成一个 `.jar` 文件，例如 `signalx-backend-0.0.1-SNAPSHOT.jar`。

---

## 5. 启动服务

### 5.1 直接启动 (前台运行)
用于测试或调试：
```bash
java -jar target/signalx-backend-0.0.1-SNAPSHOT.jar
```

### 5.2 后台运行 (Linux 生产环境推荐)

使用 `nohup` 命令将服务在后台运行，并将日志输出到文件：

```bash
nohup java -jar target/signalx-backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

*   **查看日志**: `tail -f app.log`
*   **停止服务**:
    1. 查找进程 ID: `ps -ef | grep signalx`
    2. 杀掉进程: `kill -9 <PID>`

### 5.3 作为 Windows 服务运行 (可选)
如果是 Windows 服务器，可以使用 [WinSW](https://github.com/winsw/winsw) 将 jar 包包装为系统服务，实现开机自启。

---

## 6. 验证部署

1.  检查端口是否监听：
    ```bash
    netstat -nltp | grep 8080
    ```
2.  访问健康检查接口 (如果有) 或 Swagger 文档地址，例如：
    *   `http://localhost:8080/doc.html` (如果是 Knife4j)
    *   `http://localhost:8080/swagger-ui.html`

## 7. 常见问题排查

*   **数据库连接失败**: 检查防火墙是否允许 3306 端口，检查数据库用户名密码是否正确。
*   **Jar包运行报错**: 检查 JDK 版本是否与编译版本一致。
*   **各类 Timeout**: 检查服务器网络连通性，特别是连接外网 API 或中间件时。
