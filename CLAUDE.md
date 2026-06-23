# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Build & Run Commands

### Backend (repo root)

```bash
# Build
mvn clean package

# Run application
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Compile only
mvn compile
```

### Frontend (`frontend/`)

```bash
npm install      # install dependencies
npm run dev      # Vite dev server on http://localhost:5173
npm run build    # production build
npm run lint     # ESLint
```

## Architecture Overview

Full-stack, multi-tenant NPHIES (Saudi Arabia national health insurance) integration hub for healthcare claim management:

- **Backend**: Spring Boot 3.2.5 (Java 21) REST API under `/api/**`
- **Frontend**: React 18 + Vite + Tailwind SPA in `frontend/` (login, dashboard, beneficiaries, coverages, practitioners, organizations, eligibility, claim wizard)

The backend acts as a gateway between internal tenants (hospitals/providers) and the NPHIES FHIR API. Each tenant has isolated credentials, token caches, and resilience instances.

### Key Packages (`com.amins.nphies`)

| Package | Responsibility |
|---|---|
| `auth/` | Per-tenant NPHIES OAuth2 token fetch, caching, and refresh via `NphiesTokenStore` (Caffeine cache) |
| `config/` | JPA entity `TenantNphiesConfig` (one row per tenant), WebClient beans, Resilience4j config, Spring Security config |
| `gateway/` | `NphiesGatewayClient` — all outbound FHIR calls (submit Bundle, poll response), wrapped with RateLimiter → CircuitBreaker → Retry |
| `fhir/` | HAPI FHIR bundle builders (claim, eligibility) and response mappers — all FHIR JSON translation lives here |
| `security/` | AES-256-GCM encryption (`AesEncryptionService`) and external secret resolution (`VaultCredentialService` — GCP Secret Manager or HashiCorp Vault) |
| `user/` | App-level users: JWT login/register (`AuthController`, `JwtService`, `JwtAuthFilter`) |
| `beneficiary/`, `coverage/`, `encounter/`, `organization/`, `practitioner/`, `claim/` | Tenant-scoped domain modules (controller → service → JPA repository → entity + DTOs) |
| `service/` | `EligibilityService` — orchestrates a NPHIES coverage eligibility check |
| `model/` | `TenantContext` (ThreadLocal tenant ID holder) |
| `exception/` | `NphiesException` hierarchy: retryable (5xx/network) vs non-retryable (4xx) |

### Multi-Tenancy Pattern

- **Per-schema isolation**: each tenant lives in its own MySQL schema (`nphies_{tenantId}`). There are no `tenant_id` columns in domain tables. `TenantSchemaResolver` maps the active `TenantContext` to a schema name; `SchemaTenantConnectionProvider` runs `USE schema` on every Hibernate connection checkout.
- **Authenticated requests**: `JwtAuthFilter` reads the `tid` claim from the JWT (embedded at login from the validated tenant) and sets `TenantContext`, clearing it in a `finally` block. Controllers call `TenantContext.require()` as an explicit guard.
- **Pre-auth endpoints** (login, register, send-otp, forgot-password, reset-password): the client supplies a `tenantId` in the request body. `AuthController` validates this against `TenantSchemaProvisioner.exists()` before setting `TenantContext` — requests for non-existent tenants are rejected before any DB access.
- Each tenant gets isolated: Caffeine token cache slot, Resilience4j circuit breaker/retry/rate-limiter instance, and encrypted credential row.
- `NphiesTokenStore` uses `ReentrantLock` per tenant to prevent thundering herd on token refresh.
- **Do not** accept tenant ID from client in any context other than pre-auth flows — all post-auth routing uses the JWT `tid` claim exclusively.

### Two Auth Systems

1. **App users → this service**: JWT (jjwt) issued by `AuthController` login; stateless Spring Security; `/api/auth/**` is public (except `/api/auth/invite`, ADMIN-only), everything else authenticated.
2. **This service → NPHIES**: per-tenant OAuth2 client credentials from `TenantNphiesConfig`.

### User Registration (invite-based, OTP)

- There is **no open registration**. A tenant ADMIN calls `POST /api/auth/invite` (`InvitationService`) with an email + role; this creates a `user_invitations` row carrying the **admin's own tenant**, a BCrypt-hashed 6-digit OTP, 48h expiry, and a 5-attempt cap.
- **OTP delivery**: when mail is enabled (`MAIL_ENABLED=true` + SMTP env vars), `InviteEmailService` sends a branded HTML email (`src/main/resources/templates/invite-email.html`) and the OTP is **not** included in the API response. When mail is disabled (dev default) or sending fails, the OTP is returned once in the response for out-of-band delivery — the frontend Invite User page shows whichever applies. Invite creation is never blocked by SMTP failures.
- `POST /api/auth/register` requires `{name, email, password, otp}`; tenant and role always come from the invitation, never the client. A new invite for the same email supersedes the pending one.
- **Self-service OTP delivery**: the public `POST /api/auth/register/send-otp` rotates the pending invitation's OTP and emails it to the invitee (silent no-op for uninvited emails). The register page is two-step: email → emailed code + details.
- **Bootstrap**: `BootstrapAdminInitializer` runs at startup; when the users table is empty it seeds an ADMIN (`app.bootstrap.admin-email`/`admin-password`, defaults `admin@tracare.local` / `Admin@12345`) plus a placeholder `tenant_nphies_config` row (`app.bootstrap.tenant-id`, default `HOSPITAL-001`). Change the password after first login; replace placeholder NPHIES credentials before real submissions.

### Password Reset (OTP)

- `POST /api/auth/forgot-password` always returns success (no account enumeration). For known users it stores a BCrypt-hashed 6-digit OTP in `password_reset_tokens` (15 min expiry, 5 attempts, single-use) and emails it via `EmailService` (`templates/reset-password-email.html`).
- `POST /api/auth/reset-password` takes `{email, otp, newPassword}`; every failure mode returns the same generic message. The OTP is **never** returned in any API response; with mail disabled (dev) it is logged at WARN instead.
- **Transactions and email**: DB writes commit via `TransactionTemplate` BEFORE any email is sent (`InvitationService`, `PasswordResetService`) — SMTP latency must never hold a DB transaction open. Keep this pattern for any new email-sending flow.

### Credential Security

Client secrets are stored AES-256-GCM encrypted in the DB column `client_secret_encrypted`. Alternatively, `external_secret_ref` can point to a GCP Secret Manager reference or HashiCorp Vault path — `VaultCredentialService` resolves whichever is present.

Master encryption key is read from env var `NPHIES_MASTER_KEY_BASE64`. Generate one via `AesEncryptionService.generateNewMasterKey()`.

### Resilience Stack (per tenant)

- **Circuit Breaker**: 50% failure threshold, 10-call sliding window, 30s wait
- **Retry**: 3 attempts, exponential backoff (1s → 2s → 4s)
- **Rate Limiter**: 10 calls/second

### Database

- **MySQL 8.0.19+** with **Flyway** migrations (`src/main/resources/db/migration/`, requires the `flyway-mysql` module)
- `ddl-auto: none` — with Hibernate SCHEMA multi-tenancy there is no single default schema at startup (`nphies_system` only has `tenant_registry`, not domain tables), so `validate` would fail. Flyway owns all schemas; Hibernate never touches DDL.
- Timestamps are `DATETIME(6)` stored in UTC (`connectionTimeZone=UTC` on the JDBC URL + `hibernate.jdbc.time_zone: UTC`); `updated_at` columns use `ON UPDATE CURRENT_TIMESTAMP(6)` instead of triggers
- List-valued columns (`claim_items.care_team_sequences`, `diagnosis_sequences`, `modifier_codes`) are `JSON` columns mapped with `@JdbcTypeCode(SqlTypes.JSON)` (MySQL has no array type)
- Tables: `tenant_nphies_config`, `users`, `beneficiaries`, `practitioners`, `organizations`, `coverages`, `encounters`, `claims` (+ care team/diagnoses/items/responses)

### NPHIES FHIR Endpoints

| Environment | Base URL |
|---|---|
| UAT | `https://HSB.nphies.sa/r4` |
| Production | `https://api.nphies.sa/r4` |

Key operations: `POST /r4/Bundle` (submit), `GET /r4/Bundle/{id}` (poll response).

## Environment Variables

| Variable | Purpose |
|---|---|
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | App JWT signing key (Base64); dev fallback exists, required in prod |
| `NPHIES_MASTER_KEY_BASE64` | AES-256 master key (Base64); dev fallback exists, required in prod |
| `MAIL_ENABLED` | `true` to email invitation OTPs (default `false` — OTP returned in API response) |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | SMTP server for invitation emails |
| `MAIL_FROM` | From address for invitation emails (default `no-reply@tracare.sa`) |
| `GCP_SECRET_MANAGER_ENABLED` | `true` to create the GCP Secret Manager client (default `false`; requires ADC credentials) |
| `GCP_PROJECT_ID` | Required if using GCP Secret Manager refs |
| `VAULT_ENABLED` / `VAULT_HOST` / `VAULT_PORT` / `VAULT_TOKEN` | Required if using HashiCorp Vault refs |
