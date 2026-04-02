# School Management System Backend

Spring Boot backend for a school management system covering authentication, student management, teacher management, attendance, results, fees, announcements, notifications, and supporting school operations.

## Main Features

- JWT-based authentication and authorization
- Role-based access control for admin, teacher, student, and parent
- Student management
- Teacher management
- Attendance tracking
- Term and session results
- Fee management and payment tracking
- Email notifications
- SMS notifications
- Event and announcement management

## Tech Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven

## Environment Configuration

This project uses environment variables for configuration.

Do not commit real secrets, `.env` files, keystores, or production credentials.

Create a local environment file or configure your IDE/runtime with these variables.

### Required variables

```env
SERVER_PORT=8443
SERVER_SSL_ENABLED=false

DB_URL=jdbc:postgresql://localhost:5432/school_db
DB_USERNAME=postgres
DB_PASSWORD=your_password

APP_BASE_URL=https://localhost:8443
APP_FRONTEND_URL=https://localhost:3000

JWT_SECRET=replace_with_a_long_random_secret
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000