# ByPass Transfers

ByPass Transfers is a Spring Boot web application for recording and reconciling financial transactions. It uses Thymeleaf for the UI, Spring Security for authentication/authorization, PostgreSQL for persistence, Flyway for DB migrations, and SMTP for transactional email (password reset, notifications, etc.).

## Tech Stack
- Java 17
- Spring Boot (Web, Security, Data JPA, Mail, Thymeleaf)
- PostgreSQL
- Flyway (migrations)
- Thymeleaf templates (HTML views + email templates)
- SMTP (Gmail/App Password supported)

## Prerequisites
- Java 17 + Maven installed
- PostgreSQL available (for non-Docker runs)
- (Optional) Docker Desktop (for Docker runs)

## Environment Variables
Sensitive values should be provided via a local `.env` file (Docker) or OS/IDE environment variables (Maven/IDE).

Copy the example:
```powershell
Copy-Item .env.example .env
```

Never commit `.env` to version control.

### Mail (SMTP)
The app expects:
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

`MAIL_PASSWORD` should be your SMTP/app-password for providers like Gmail (not your normal account password).

## Run the App

### Option A: Run with Docker (DB + app)
1. Start Docker Desktop.
2. Ensure `.env` exists in the project root and contains `DB_*` and `MAIL_*` values.
3. Run:
```powershell
docker compose up --build -d
```
4. Open:
`http://localhost:8080`

### Option B: Run with Maven / IDE (No Docker)
1. Start PostgreSQL locally so the app can reach:
   - `localhost:5432`
   - database `bypass_records` (as per `application.properties`)
2. Start the application:
```powershell
mvn spring-boot:run
```
3. Ensure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` are available to the running process (set them in your IDE run configuration or your PowerShell environment).

## Verify Email (SMTP)
When the app is running, test SMTP with:
```text
GET /api/test/send-test-email
```

By default, it sends to your configured sender account (`MAIL_USERNAME`). You can override it:
```text
GET /api/test/send-test-email?to=someone@example.com
```

## Password Reset Flow
1. Page: `POST /forgot-password` (from the “Forgot Password” form)
2. Email link: `GET /reset?token=...`
3. Submit new password: `POST /reset`

## Testing
Run unit/integration tests:
```powershell
mvn test
```

## Troubleshooting
- **SMTP “accepted” but no email received**
  - Confirm the recipient address is the one you expect (spam folder too).
- **Reset page fails with Access Denied (403)**
  - The reset form must include a CSRF token. This project’s `reset-password.html` includes CSRF automatically.
- **App starts but DB errors occur**
  - Ensure PostgreSQL is running locally and credentials match `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

