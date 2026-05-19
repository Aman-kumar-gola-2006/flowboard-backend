FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/card-service-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
