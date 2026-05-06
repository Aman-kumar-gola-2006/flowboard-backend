# 📘 FlowBoard - Task Management & Collaboration Tool

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green) ![Angular](https://img.shields.io/badge/Angular-16-red) ![MySQL](https://img.shields.io/badge/MySQL-8.4-blue) ![AWS](https://img.shields.io/badge/AWS-EC2-orange) ![License](https://img.shields.io/badge/License-MIT-yellow)

**FlowBoard** is a full-stack, microservices-based **Kanban Task Management Tool** inspired by Trello. It enables teams to organize work visually, collaborate in real-time, and manage projects efficiently.

---

## 🚀 Features

### 🔐 Authentication
- Email/Password Login & Registration (BCrypt hashed)
- Google OAuth2 & GitHub OAuth2
- JWT Token based security (24h expiry)
- Role-based access (MEMBER, ADMIN)

### 📋 Kanban Board
- Drag & Drop cards between lists (Angular CDK)
- Create/Edit/Delete workspaces, boards, lists, cards
- Card metadata: Priority, Status, Due Date, Assignee, Cover Color
- Archive/Restore cards and lists

### 💬 Collaboration
- Threaded comments with replies
- @Mention detection & notification
- File attachments upload
- Color-coded labels

### ✅ Task Management
- Checklists with progress bar
- Card activity log (who did what & when)
- Board analytics (card counts, overdue, completion rate)

### 🔔 Notifications
- Real-time WebSocket (STOMP) notifications
- Email notifications (Gmail SMTP)
- Due date reminders (1 day & 1 hour before)
- In-app notification bell with unread badge

### 🛡️ Admin Panel
- User management (suspend/reactivate/delete)
- Platform-wide analytics
- Audit logs viewer
- CSV export (users & audit logs)
- SLA monitoring (overdue cards)
- Broadcast notifications to all users

### 🌐 Public Access
- View public boards without login (`/explore`)

---

## 🏗️ Architecture

```
Angular Frontend → Nginx (SSL) → API Gateway → Microservices → MySQL
                                      ↓
                              Eureka Service Discovery
```

### Microservices (10 Services)

| Service | Port | Description |
|---------|------|-------------|
| **eureka-server** | 8761 | Service Discovery |
| **gateway-service** | 8080 | API Gateway + CORS + Routing |
| **auth-service** | 8081 | Authentication, JWT, OAuth2 |
| **workspace-service** | 8082 | Workspace & Member management |
| **board-service** | 8083 | Board CRUD + Analytics |
| **list-service** | 8084 | List/Column management |
| **card-service** | 8085 | Card CRUD + Checklist + Attachments |
| **comment-service** | 8086 | Threaded comments |
| **notification-service** | 8088 | Real-time + Email notifications |
| **payment-service** | 8089 | Razorpay integration |

---

## 📦 Technology Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 17, Spring Boot 3.1.5, Spring Cloud |
| **Security** | Spring Security, JWT, OAuth2 |
| **Database** | MySQL 8.4 (per service) |
| **Service Discovery** | Netflix Eureka |
| **API Gateway** | Spring Cloud Gateway |
| **Real-time** | WebSocket, STOMP |
| **Email** | JavaMailSender + Gmail SMTP |
| **Frontend** | Angular 16+, TypeScript |
| **Styling** | Tailwind CSS |
| **Drag-Drop** | Angular CDK |
| **Cloud** | AWS EC2 (c7i-flex.large) |
| **SSL** | Nginx Reverse Proxy |
| **Deployment** | Netlify (Frontend) |

---

## 🗄️ Database Schema

```
flowboard_auth        → users, audit_logs
flowboard_workspace   → workspaces, workspace_members
flowboard_board       → boards, board_members
flowboard_list        → task_lists
flowboard_card        → cards, card_activity, checklist_items, attachments, card_labels
flowboard_comment     → comments
flowboard_notification → notifications
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+
- Angular CLI 16+
- MySQL 8.0+

### Backend Setup
```bash
# Clone repository
git clone https://github.com/Aman-kumar-gola-2006/flowboard-backend.git
cd flowboard-backend

# Create MySQL databases
mysql -u root -p -e "CREATE DATABASE flowboard_auth; CREATE DATABASE flowboard_workspace; CREATE DATABASE flowboard_board; CREATE DATABASE flowboard_list; CREATE DATABASE flowboard_card; CREATE DATABASE flowboard_comment; CREATE DATABASE flowboard_notification;"

# Start services (order matters!)
cd eureka-server && mvn spring-boot:run &
cd ../gateway-service && mvn spring-boot:run &
cd ../auth-service && mvn spring-boot:run &
cd ../workspace-service && mvn spring-boot:run &
cd ../board-service && mvn spring-boot:run &
# ... (all 10 services)
```

### Frontend Setup
```bash
# Clone repository
git clone https://github.com/Aman-kumar-gola-2006/flowboard-frontend.git
cd flowboard-frontend

# Install dependencies
npm install

# Run development server
ng serve --proxy-config proxy.conf.json -o
```

**Browser:** `http://localhost:4200`

---

## ☁️ Cloud Deployment

### AWS EC2
- **Instance:** c7i-flex.large (2 vCPU, 4GB RAM)
- **OS:** Ubuntu 26.04 LTS
- **SSL:** Nginx + Self-signed SSL (Port 443)
- **Public IP:** `16.176.51.5`

### Netlify (Frontend)
- **URL:** `https://flowboard-taskmanager.netlify.app`
- **Build Command:** `npm run build`
- **Publish Directory:** `dist/flowboard`

---

## 🔑 Default Demo Credentials

```
Username: amangola
Password: 123456
Email: aman@test.com
```

---

## 📊 Project Statistics

| Metric | Value |
|--------|-------|
| Total Microservices | 10 |
| Total REST APIs | 50+ |
| Frontend Pages | 12 |
| Databases | 7 |
| Requirements Met | 44/44 (100%) |
| Use Cases Implemented | 26/26 (100%) |
| Lines of Code | 15,000+ |

---

## 📝 Evaluation Parameters Met

| # | Parameter | Status |
|---|-----------|--------|
| 1 | JWT Security + Microservices | ✅ |
| 2 | Design Diagrams | ✅ |
| 3 | Unit Tests (JUnit + Mockito) | ✅ |
| 4 | SonarQube / SonarLint | ⚠️ |
| 5 | Field Validations | ✅ |
| 6 | RabbitMQ/Kafka | ⚠️ (RabbitMQ code present) |
| 7 | Payment Gateway (Razorpay) | ✅ |
| 8 | Swagger Documentation | ✅ |
| 9 | Cloud Deployment | ✅ (AWS EC2 + Netlify) |
| 10 | Unique Functionalities | ✅ (10+ unique features) |
| 11 | UML Diagrams | ✅ |

---

## 📚 Documentation

- **Swagger UI:** `http://localhost:8081/swagger-ui.html` (each service)
- **Eureka Dashboard:** `http://localhost:8761`
- **Postman Collection:** Available in `/docs` folder

---

## 🔧 Environment Variables

Create `application.yml` in each service's `src/main/resources/`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/flowboard_auth
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: your_secret_key
  expiration: 86400000
```

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Aman Kumar Gola**
- GitHub: [@Aman-kumar-gola-2006](https://github.com/Aman-kumar-gola-2006)
- Email: amanagola9841@gmail.com

---

## 🙏 Acknowledgments

- Spring Boot & Spring Cloud Team
- Angular Team
- Tailwind CSS
- Trello (Inspiration)

---
