# Project Documentation (ByPass Transfers)

## 1) Overview
This project is a Spring Boot (Java 17) web application that provides:
- Authentication: login, registration, and role-based access
- Password management: forgot-password and password reset
- Email features: password reset email, email verification, and admin notifications
- Core business features: transaction recording and reconciliation (UI + REST endpoints)
- Database: PostgreSQL with Flyway migrations

## 2) Folder / Module Map (conceptual)
Even though this is a single Spring Boot application, the code is organized by layer:
- `controller/`: HTTP endpoints (page navigation + REST endpoints)
- `service/`: business logic (token creation, email sending, reconciliation, etc.)
- `repository/`: persistence (Spring Data JPA)
- `model/`: entities and DTO-like classes
- `templates/`: Thymeleaf HTML views
- `templates/email/`: Thymeleaf templates used to render SMTP email bodies

## 3) Authentication & Password Reset

### 3.1 Login & Registration
- Public pages include `/login`, `/register`, `/forgot-password`.
- Authenticated pages require login and relevant roles (configured in `SecurityConfig`).

### 3.2 Forgot Password (request)
- Endpoint: `POST /forgot-password`
- Behavior:
  1. Looks up a user by email or username.
  2. Creates a password reset token with an expiry.
  3. Sends the reset link by email if the user has an email address.
  4. If no email is available, the page can show the link (for admin/debug scenarios).

### 3.3 Password Reset (set new password)
- GET form page: `GET /reset?token=...`
- POST to submit new password: `POST /reset`
- Security:
  - The reset form includes CSRF token support (required by Spring Security).

## 4) Email System (SMTP)

### 4.1 What to Configure
The app reads SMTP settings via Spring properties placeholders:
- `spring.mail.host=${MAIL_HOST:...}`
- `spring.mail.port=${MAIL_PORT:...}`
- `spring.mail.username=${MAIL_USERNAME:...}`
- `spring.mail.password=${MAIL_PASSWORD:...}`

### 4.2 Gmail Note
If you use Gmail, prefer:
- SMTP/App password (not your normal Google password)
- `MAIL_USERNAME` matching the authorized sender

### 4.3 Email Templates
Email templates live under:
- `src/main/resources/templates/email/`

For example:
- `password-reset.html`
- `verify-email.html` (if used by email verification flow)

### 4.4 SMTP Verification Endpoint
When the app is running:
- `GET /api/test/send-test-email`
  - Sends a “test” email using the configured SMTP sender
  - Optional override: `?to=recipient@example.com`

## 5) Running the App

### 5.1 Docker (recommended for “everything together”)
1. Install Docker Desktop.
2. Create `.env` from `.env.example`.
3. Start:
   - `docker compose up --build -d`
4. Open:
   - `http://localhost:8080`

### 5.2 Maven / IDE (no Docker)
Use when you want to run everything directly on your machine:
1. Start PostgreSQL locally (the app expects `localhost:5432` by default).
2. Provide DB credentials and mail credentials to the running process.
3. Start:
   - `mvn spring-boot:run`

## 6) Database & Migrations
- Flyway is enabled (`spring.flyway.enabled=true`).
- Migration scripts are located under `classpath:db/migration`.
- Hibernate auto-update is enabled in the configuration (`ddl-auto=update`).

## 7) Troubleshooting

### 7.1 “SMTP accepted” but you didn’t receive the email
Common causes:
- Message was sent to a different recipient than you expect
- Gmail spam/quarantine
- Provider policy / app-password/auth issues

### 7.2 Password reset page says “Access Denied (403)”
Most common cause:
- Missing/invalid CSRF token on the reset POST.
This project’s reset template is designed to include the CSRF hidden field.

### 7.3 App can’t start (DB connection errors)
- Ensure PostgreSQL is running
- Ensure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` match your local setup

## 8) Security Notes
- Admin routes and REST endpoints are protected by Spring Security roles.
- `.env` contains secrets; do not commit it.

