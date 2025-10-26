# Chatter Backend

A real-time communication backend built with Spring Boot, supporting instant messaging, audio/video calls, and screen sharing capabilities.

## 🚀 Features

- **Real-time Messaging**: WebSocket-based instant messaging with message persistence and read receipts
- **Stories**: Temporary content sharing with 24-hour expiration
- **Reactions**: Message reactions and emoji responses
- **Audio/Video Calls**: WebRTC integration with Mediasoup for peer-to-peer communication
- **Screen Sharing**: Screen sharing capabilities for collaboration
- **Voice Messages**: Record and send audio messages
- **File Attachments**: Upload and share files in conversations
- **Groups**: Create and manage group conversations with member permissions
- **Typing Indicators**: Real-time typing status updates
- **Presence Tracking**: Online/offline status for users
- **Search & Filters**: Search messages within conversations and filter chat history
- **User Blocking**: Block and unblock users to control interactions
- **Message Queue**: RabbitMQ (STOMP) integration for reliable message delivery
- **Caching Layer**: Redis caching for improved performance
- **Database Migrations**: Flyway integration for version-controlled schema management

## 🔒 Security

- JWT-based authentication
- Password encryption using BCrypt
- CORS configuration for secure cross-origin requests
- Rate limiting with token bucket algorithm

## 🛠️ Tech Stack

- **Framework**: Spring Boot
- **Language**: Java
- **Database**: MySQL
- **Message Broker**: RabbitMQ (STOMP protocol)
- **Cache**: Redis
- **Real-time Communication**: WebSockets
- **Containerization**: Docker

## 🔧 Installation

### Local Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/chatter-backend.git
cd chatter-backend
```

2. **Configure environment variables**

3. **Install dependencies**
```bash
./gradlew build
```

4. **Run database migrations**
```bash
./gradlew flywayMigrate
```

5. **Start the application**
```bash
./gradlew bootRun
```

### Docker Setup

1. **Build and run with Docker Compose**
```bash
docker-compose up -d
```

This will start:
- Spring Boot application
- MySQL database
- Redis cache
- RabbitMQ message broker

## 🧪 Testing

Run the test suite:
```bash
./gradlew test
```

⭐ If you find this project helpful, please consider giving it a star!