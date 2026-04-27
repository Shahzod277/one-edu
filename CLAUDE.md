# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build          # Build the project
./gradlew bootRun        # Run (HTTPS on port 8084)
./gradlew test           # Run tests (no tests written yet)
```

Windows: use `gradlew.bat` instead of `./gradlew`.

Requires **Java 21**. Uses **Spring Boot 4.0.1** with Gradle.

## What This System Does

One-Edu — ta'lim tizimlarini One-ID orqali autentifikatsiya qilish platformasi. Tashkilotlar (universitetlar) o'z client system'larini ro'yxatdan o'tkazadi, har biriga RSA key pair va API key beriladi. Foydalanuvchi One-ID orqali login qilganda, uning PINFL va passport raqami shifrlangan holda client tizimga redirect qilinadi.

## Architecture

**Authentication flow:** Browser → `/api/auth/{apiKey}` → 302 to One-ID SSO → callback with code → exchange for token → get user info (PINFL) → RSA encrypt → 302 redirect to client system's `redirectUrl?data=encrypted`.

The `state` parameter in OAuth flow carries the `apiKey` (or `my-hemis`, `my-tutor|universityCode`) to route the callback to the correct service.

**Three OAuth callback handlers based on `state`:**
- `my-hemis` → `MyHemisService`
- `my-tutor|{code}` → `EmployeeHemisService`
- any other apiKey → `AuthService` (generic client system flow)

**Key domain relationships:**
- `Organization` 1→N `ClientSystem` — each org has multiple client systems
- `ClientSystem` 1→N `Audit` — every auth attempt is logged
- `User` N→N `Role` — admin panel users with JWT auth

**Service layer** handles HEMIS integration via `HemisAuthConfigService` which pushes RSA keys to HEMIS API. Client systems track push state via `isPushed` and `isUpdatedHemis` flags.

## Package Layout

- `api_integration/one_id_api/` — One-ID OAuth client (token exchange, user info)
- `security/` — JWT auth filter, Spring Security config
- `sevice/` — business logic (note: package name is intentionally `sevice`, not `service`)
- `domain/` — JPA entities, all extend `AbstractEntity` (soft delete via `isActive`, optimistic locking via `@Version`, JPA auditing)
- `model/` — DTOs, request objects, and native query projection interfaces

## Database

PostgreSQL. Hibernate `ddl-auto: update` — schema auto-managed, no migration tool.

All entities extending `AbstractEntity` get: `id`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `isActive`, `version`.

Soft delete pattern: set `isActive = false`, query with `findAllByIsActiveTrue()` / `findByIdAndIsActiveTrue()`.

## API Endpoints

**Auth:** `/api/auth/**` — One-ID OAuth redirect va callback, admin login (`POST /api/auth/public/signIn`)

**Organization CRUD:** `/api/organizations` — create, getById, list, paginated search, update, soft delete. Duplicate code tekshirish bor.

**Client Systems:** `/api/client-systems` — CRUD + HEMIS'ga key push (`POST /push-hemis/{id}`), filtrlash (active, isPushed, orgId, search).

**Dashboard Statistics:** `/api/audit-stats/dashboard/*` — 9 ta endpoint:
- `summary` — bugungi + all-time raqamlar
- `monthly-trend?months=6` — oylik grafik uchun
- `monthly-comparison` — joriy vs oldingi oy + o'sish foizi
- `top-organizations?limit=10` — eng faol orglar
- `top-error-organizations?limit=10` — eng ko'p xato bergan orglar
- `unique-users-trend?months=6` — unikal foydalanuvchilar dinamikasi
- `peak-hours` — soatlar kesimida yuk (0-23)
- `error-breakdown?limit=20` — xato turlari bo'yicha guruhlash
- `client-uptime` — har bir client system oxirgi faoliyati

Batafsil response formatlari `DASHBOARD_API.md` da.

## Security

Public endpoints: `/api/auth/**`, `/api/audit-stats/**` (eski 4 ta stat endpoint), Swagger docs.
Dashboard endpoint'lari va qolgan barcha API'lar JWT Bearer token talab qiladi.

Secrets are in environment variables (see `.env.example`): `DB_PASSWORD`, `JWT_SECRET`, `ONEID_ADMIN_CLIENT_SECRET`, `ONEID_USER_CLIENT_SECRET`. One-ID OAuth credentials kodda emas, `application.yml` da `${ENV_VAR}` orqali oqiladi.

## Conventions

- Response wrapper: `ResponseDto` with `code`, `message`, `success`, `data` fields
- Error messages are in Uzbek (e.g. "Topilmadi", "Sizga ruxsat yo'q")
- `ResponseMessage` enum holds standard messages
- `ClientSystem` entity does NOT extend `AbstractEntity` — it has its own `id` and `active` field
- Dashboard stats use native SQL queries with Spring Data projection interfaces
- Organization/Audit entities use soft delete (`isActive = false`), ClientSystem uses `active = false`
