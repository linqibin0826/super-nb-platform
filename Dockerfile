# 多阶段:build 阶段 bootstrap commons + 打 fat jar,runtime 用 JRE 25。
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY . .
RUN bash scripts/bootstrap-commons.sh && ./gradlew :snb-boot:bootJar -x test --console=plain
RUN cp snb-boot/build/libs/snb-boot.jar /app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
