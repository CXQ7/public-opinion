# 使用官方 OpenJDK 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制Maven构建产物
COPY target/gateway.jar /app/app.jar

# 暴露端口（根据项目调整）
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "app.jar"]
