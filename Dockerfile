# 运行镜像:CI 先在 runner 上 `./gradlew build`(含全部测试)出 bootJar,这里只做装箱。
# 本地手工等价物:bash scripts/bootstrap-commons.sh && ./gradlew build && docker build -t snb-platform .
FROM eclipse-temurin:25-jre

WORKDIR /app
COPY snb-boot/build/libs/snb-boot-*.jar /app/app.jar

# 堆上限跟随容器 mem_limit(75%),部署侧只需给 mem_limit 一个旋钮;要覆盖用 JAVA_TOOL_OPTIONS
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
