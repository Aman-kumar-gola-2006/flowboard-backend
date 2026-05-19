FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/board-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
