FROM eclipse-temurin:21-jre

WORKDIR /app

# compose-up task가 먼저 bootJar를 만들고, Docker는 완성된 jar만 복사한다.
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

# profile, DB, Redis, AI 설정은 docker-compose.yml의 environment에서 주입한다.
ENTRYPOINT ["java", "-jar", "app.jar"]
