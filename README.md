# OurSecurity backend

REST API for the OurSecurity gated community app, built with Java 21 and Spring Boot 3.5.
It serves the mobile app in [oursecurity-fe](https://github.com/prasanthdsd/oursecurity-fe).

## What it does

| Area | Who | What |
|---|---|---|
| Auth | everyone | Log in with phone and password, get a JWT. |
| Visitors | resident | Pre-approve a guest and get a 6-digit pass code. Approve or deny walk-ins. Cancel. |
| Visitors | guard | Verify a pass code (checks the visitor in), log a walk-in for a flat, check in after approval, check out. |
| Deliveries | guard | Log a package for a flat, mark it collected or returned. |
| Deliveries | resident | Tell the guard to leave it at the gate or send it up. |
| Emergency alerts | everyone | Raise an SOS (MEDICAL, FIRE, SECURITY, OTHER). Every guard and committee member is notified. |
| Emergency alerts | guard, committee | Acknowledge and resolve. |
| Users | committee | Add residents, guards and committee members. Remove (deactivate) them. |
| Notices | committee posts, everyone reads | Community notice board. |
| Reports | committee | Dashboard counts: visitors today, pending approvals, deliveries at gate, open alerts, residents, guards. |
| Notifications | everyone | In-app notifications plus push notifications to phones (through Expo) for approval requests, arrivals, deliveries, alerts and notices. |

## Run it locally

Needs Java 21. No database setup: it uses an in-memory H2 database by default and seeds demo users.

```bash
./mvnw spring-boot:run
```

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs (a copy is checked in at [docs/openapi.json](docs/openapi.json))

### Demo users (seeded on an empty database)

| Role | Phone | Password | Flat |
|---|---|---|---|
| Committee | 9000000001 | admin123 | |
| Guard | 9000000002 | guard123 | |
| Resident | 9000000003 | resident123 | A-101 |
| Resident | 9000000004 | resident123 | B-202 |

```bash
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"phone":"9000000003","password":"resident123"}'
# then send: Authorization: Bearer <token>
```

## API conventions

- Base path `/api`, JSON in and out.
- Auth: `Authorization: Bearer <jwt>` on everything except `POST /api/auth/login`.
- Errors: `{"message": "..."}` with 400 (invalid input), 401 (not logged in), 403 (wrong role), 404, 409 (wrong state, e.g. checking in a visit that is still pending).
- Timestamps are ISO-8601 in UTC, e.g. `2026-11-26T13:05:30Z`.
- Residents only see their own flat's visits and deliveries, and the alerts they raised. Guards and committee see everything.

## Endpoints

| Method | Path | Role |
|---|---|---|
| POST | `/api/auth/login` | public |
| GET | `/api/auth/me` | any |
| POST | `/api/visits/preapprove` | resident |
| POST | `/api/visits/walk-in` | guard |
| POST | `/api/visits/verify` | guard |
| GET | `/api/visits?status=` | any |
| GET | `/api/visits/{id}` | any |
| POST | `/api/visits/{id}/approve` · `/deny` · `/cancel` | resident |
| POST | `/api/visits/{id}/check-in` · `/check-out` | guard |
| POST | `/api/deliveries` | guard |
| GET | `/api/deliveries?status=` | any |
| POST | `/api/deliveries/{id}/instruction` | resident |
| POST | `/api/deliveries/{id}/collect` · `/return` | guard |
| POST | `/api/alerts` | any |
| GET | `/api/alerts?status=` | any |
| POST | `/api/alerts/{id}/acknowledge` · `/resolve` | guard, committee |
| GET / POST | `/api/users?role=` | committee |
| DELETE | `/api/users/{id}` | committee |
| GET | `/api/notices` | any |
| POST | `/api/notices` | committee |
| GET | `/api/reports/summary` | committee |
| GET | `/api/notifications` | any |
| POST | `/api/notifications/{id}/read` | any |
| POST | `/api/devices` | any |
| POST | `/api/devices/unregister` | any |

Visit statuses: `PRE_APPROVED → CHECKED_IN → CHECKED_OUT` for invited guests, and
`PENDING_APPROVAL → APPROVED → CHECKED_IN → CHECKED_OUT` (or `DENIED`) for walk-ins. `CANCELLED` before check-in.

## Push notifications

The app registers its Expo push token with `POST /api/devices {"token":"ExponentPushToken[...]","platform":"ANDROID"}` after login,
and calls `POST /api/devices/unregister {"token":...}` on logout. If another user signs in on the same phone, the token moves to them.

Pushes go through [Expo's push service](https://docs.expo.dev/push-notifications/sending-notifications/), so the server needs no
Firebase or Apple keys. They are sent after the database commit, in the background, and tokens Expo reports as
`DeviceNotRegistered` are deleted. Every push carries `data = {type, id}`:

| `data.type` | Sent to | When | Android channel |
|---|---|---|---|
| `VISIT_REQUEST` | residents of the flat | guard logs a walk-in | `gate` |
| `VISIT_UPDATE` | guards / residents of the flat | resident approves or denies / visitor checks in | `gate` |
| `DELIVERY` | residents of the flat / guards | guard logs a delivery / resident sets an instruction | `default` |
| `ALERT` | all guards and committee / the person who raised it | alert raised / acknowledged or resolved | `alerts` |
| `NOTICE` | everyone except the poster | committee posts a notice | `default` |

## Configuration

Set with environment variables:

| Variable | Default | Notes |
|---|---|---|
| `PORT` | 8080 | |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | in-memory H2 | e.g. `jdbc:postgresql://host:5432/oursecurity` |
| `JWT_SECRET` | dev-only value | 32+ characters. Must be set in production. |
| `JWT_EXPIRY` | 7d | |
| `CORS_ALLOWED_ORIGINS` | `*` | comma-separated |
| `APP_TIME_ZONE` | Asia/Kolkata | used for "visitors today" |
| `PASS_VALIDITY` | 24h | how long a pre-approval pass works after its expected time |
| `SEED_DEMO_DATA` | true | off under the `prod` profile |
| `PUSH_PROVIDER` | expo | `expo` sends real pushes, `log` only logs them |
| `EXPO_ACCESS_TOKEN` | empty | only if "enhanced push security" is on in the Expo project |

Production: `SPRING_PROFILES_ACTIVE=prod` with PostgreSQL. It requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `CORS_ALLOWED_ORIGINS`, and turns off demo data. A `Dockerfile` is included.

## Tests

```bash
./mvnw test
```

`ApiFlowTest` runs the main flows end to end through the HTTP API: login, pre-approval with pass code, walk-in approval, deliveries, emergency alerts, user management, notices and role checks.
`PushFlowTest` covers device registration and which pushes each event sends, and `ExpoPushGatewayTest` covers the Expo request format, batching and dead-token handling.

## Not done yet

- Push receipts. Expo's second step (checking receipts about 15 minutes later) is not polled yet, so some dead tokens are only removed on the next send.
- Schema migrations. Hibernate creates the tables (`ddl-auto=update`); switch to Flyway before real data.
- One community per deployment. Multi-community support would add a community id to users and records.
- Password reset, OTP login, refresh tokens, visitor photos, and rate limiting on login.
