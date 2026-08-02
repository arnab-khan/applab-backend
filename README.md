# Applab Backend

Backend service for Applab, a full-stack web application built with Angular and Spring Boot.

## Overview

This repository contains the Spring Boot backend for Applab. It provides REST APIs, authentication, session management, profile features, file upload support, and real-time chat communication for the frontend application.

The backend supports secure cookie-based session authentication, public user profiles, unique username validation, profile image upload, and real-time global chat features including live messaging, typing indicators, message editing, message deletion, reactions, quoted replies, and live synchronization.

## Tech Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Redis-backed session management
- STOMP WebSocket
- Maven

## Related Links

- Live project: https://applab.arnabkhan.in/chat/global
- Demo video: https://arnab-khan.github.io/images/projects/app-lab.mp4
- Frontend source code: https://github.com/arnab-khan/applab-frontend
- Backend source code: https://github.com/arnab-khan/applab-backend

## Features

- Cookie-based session authentication
- User registration and login APIs
- Profile management
- Unique username validation
- Profile image upload
- Public user profile support
- RESTful API design
- Redis-backed session storage
- Real-time global chat using STOMP WebSocket
- Live messaging and synchronization
- Typing indicators
- Message editing and deletion
- Message reactions
- Quoted replies

## Deployment

The application is designed to run as the backend service for the Applab frontend. The full project is deployed on a Linux VPS using Nginx and PM2, with automated CI/CD pipelines handled through GitHub Actions.

## Development Setup

### Prerequisites

- Java 21
- MySQL
- Redis

### Database and Redis

Create a MySQL database for the application and make sure Redis is running locally or on a configured server.

Update the application configuration with your local MySQL and Redis connection details before starting the backend.

### Run the Application

```bash
./mvnw clean install
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

## License

This project is not open source. See [LICENSE](LICENSE) for usage restrictions.