# NPHIES Gateway

A multi-tenant integration service that connects hospital EMR systems to **NPHIES** — the Saudi national health insurance exchange that all Saudi healthcare providers must route claims through.

Hospitals submit insurance claims and eligibility checks through this service. It translates requests into FHIR R4 bundles, authenticates against NPHIES using per-tenant OAuth2 credentials, and surfaces the asynchronous response back to the hospital system. Multiple hospital tenants share one deployment with full data and credential isolation.

## Architecture highlights

### Schema-per-tenant multi-tenancy
Each tenant gets a dedicated MySQL schema provisioned on registration via Flyway. Hibernate's `MultiTenantConnectionProvider` routes every query to the correct schema using a `ThreadLocal` tenant ID derived from the authenticated JWT — never from a client-supplied header. `TenantSchemaProvisioner` runs migrations against each new schema at registration time.

### Per-tenant Resilience4j stack
Each tenant gets dedicated circuit breaker, retry, and rate limiter instances so one misbehaving payer doesn't degrade others:
- **Circuit breaker**: 50% failure threshold, 10-call sliding window, 30 s open wait
- **Retry**: 3 attempts, exponential backoff (1 s → 2 s → 4 s), retryable on 5xx/network only
- **Rate limiter**: 10 calls/s

### FHIR R4 translation (HAPI FHIR)
`ClaimBundleBuilder` and `CoverageEligibilityRequestBundleBuilder` assemble typed FHIR R4 Bundles from domain objects. `ClaimResponseMapper` and `CoverageEligibilityResponseMapper` parse NPHIES Bundle responses back into domain DTOs. All FHIR JSON logic is confined to the `fhir/` package.

### Credential encryption + external secret resolution
NPHIES OAuth2 client secrets are stored AES-256-GCM encrypted in the database (key from `NPHIES_MASTER_KEY_BASE64`). `VaultCredentialService` can resolve credentials from an encrypted DB column, GCP Secret Manager, or HashiCorp Vault — switchable per tenant row.

### Token cache (Caffeine + per-tenant ReentrantLock)
`NphiesTokenStore` caches OAuth2 access tokens per tenant in Caffeine. A per-tenant `ReentrantLock` prevents thundering-herd refresh storms when a token expires under concurrent load.

### Two independent auth systems
1. **App users → this service**: stateless JWT (jjwt); invite-only registration with BCrypt-hashed OTP (15 min expiry, 5-attempt cap); password reset via OTP email
2. **This service → NPHIES**: OAuth2 client credentials per tenant, fetched and cached by `NphiesTokenService`

## Tech stack

| Layer | Choice |
|---|---|
| Runtime | Java 21, Spring Boot 3.2.5 |
| FHIR | HAPI FHIR 7.x (R4) |
| Database | MySQL 8.0+ · Flyway migrations · schema-per-tenant |
| Resilience | Resilience4j — circuit breaker, retry, rate limiter |
| Security | JWT (jjwt) · AES-256-GCM · GCP Secret Manager / HashiCorp Vault |
| HTTP client | Spring WebClient (reactor-netty) |
| Caching | Caffeine |
| Frontend | React 18 · Vite · Tailwind CSS |
| Container | Docker · docker-compose |
| CI/CD | GitHub Actions → GHCR → SSH deploy |

## Running locally

```bash
cp .env.example .env          # fill in DB_PASSWORD and NPHIES_MASTER_KEY_BASE64
docker-compose up -d db
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev                   # http://localhost:5173
```

## Domain context

NPHIES (National Platform for Health Information Exchange for Saudi) is the Saudi Ministry of Health's FHIR R4 clearinghouse. Every insurance claim from a Saudi healthcare provider must be submitted through it. This service is the integration layer: it handles per-payer credential lifecycle, FHIR bundle assembly, submission (`POST /r4/Bundle`), and response polling — so hospital applications talk to a single internal REST API instead of implementing any of that themselves.

| Environment | Base URL |
|---|---|
| UAT | `https://HSB.nphies.sa/r4` |
| Production | `https://api.nphies.sa/r4` |
