FROM gradle:8.9-jdk21 AS builder
WORKDIR /app
COPY . .
ARG SERVICE_NAME
RUN chmod +x ./gradlew && \
    ./gradlew :service:${SERVICE_NAME}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_NAME
COPY --from=builder /app/service/${SERVICE_NAME}/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
