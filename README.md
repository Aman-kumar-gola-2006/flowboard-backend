# FlowBoard Backend - Microservices Architecture

This project is a Trello-like project management tool built with Spring Boot Microservices.

## 🏗️ Architecture Diagram

```mermaid
graph TD
    Client[Frontend / Client] --> Gateway[Gateway Service :8080]
    Gateway --> Auth[Auth Service :8081]
    Gateway --> Board[Board Service :8082]
    Gateway --> Card[Card Service :8084]
    Gateway --> Workspace[Workspace Service :8086]
    
    Auth -.-> DB1[(MySQL: Auth DB)]
    Board -.-> DB2[(MySQL: Board DB)]
    Card -.-> DB3[(MySQL: Card DB)]
    
    Auth -- RabbitMQ --> Notification[Notification Service :8085]
    Notification -- Mail --> User((User Email))
    
    Board -- Feign --> Workspace
    Card -- Feign --> Board
    
    Registry[Eureka Server :8761] --- Auth
    Registry --- Board
    Registry --- Card
    Registry --- Notification
    Registry --- Gateway
```

## 🛠️ Mandatory Requirements Implementation

### 1. Security & Microservices
- **JWT**: Implemented in `auth-service` and validated at `gateway-service`.
- **Discovery**: Eureka Server for service registration.

### 2. Messaging (RabbitMQ)
- Used for sending asynchronous notifications (OTP, Welcome Email) to decouple `auth-service` from email delivery.

### 3. Payment Integration
- **RazorPay**: Integrated in `payment-service` for PRO membership upgrades.

### 4. Code Quality & Coverage
- **Jacoco**: Integrated for Unit Test coverage.
- **SonarQube**: Configured in root `pom.xml` for static code analysis.
- **Goal**: > 80% coverage.

### 5. API Documentation
- **Swagger/OpenAPI**: Available at `/swagger-ui.html` for each service.

## 🚀 How to Run Analysis

To generate code coverage and run SonarQube analysis:
```bash
mvn clean verify sonar:sonar -Dsonar.login=YOUR_SONAR_TOKEN
```
