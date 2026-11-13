# Secret Santa Organizer

Full-stack application for organizing Secret Santa gift exchanges. A game is created with a budget and a date, participants join with a share code, and the backend performs a fair random draw and emails each participant their assigned person.

There are no user accounts: the game code is the only access. The project is split into a SvelteKit frontend and a Spring Boot API, with PostgreSQL as the database.

## Live demo

**https://secret-santa-organizer-delta.vercel.app/**

The frontend is hosted on Vercel, the API on Render and the database on Supabase. No sign-up is needed: create a game, share the code and join from another device. The free backend sleeps after 15 minutes idle, so the first request may take up to a minute; the status indicator in the header shows when it is awake.

## Stack

| Layer | Technology |
| --- | --- |
| Frontend | SvelteKit 2, Svelte 5, Vite, native CSS |
| Backend | Spring Boot 4, Java 25, Maven |
| Database | PostgreSQL 16 (Supabase in the live demo) |
| Email | Brevo HTTP API |
| Containers | Docker and Docker Compose |

## Project structure

```text
.
├── frontend/            SvelteKit application
├── server/              Spring Boot API
├── Dockerfile           Backend image
├── docker-compose.yml   PostgreSQL + backend
└── README.md
```

## How it works

### Frontend

The UI is a single page with two tabs:

1. **Unirme**: sends `POST /api/games/{code}/participants` with the participant data and wishlist. It is the default tab, because most visitors arrive with a code.
2. **Crear intercambio**: sends `POST /api/games` with the name, budget and date, then shows the generated share code.

The Santa panel is a modal opened from the header. It loads the participants of a game with `GET /api/games/{code}/participants`, shows masked email addresses, and triggers the draw with `POST /api/games/{code}/assign`.

The API base URL is read from `PUBLIC_API_URL` (see `frontend/src/lib/config.js`). If it is not defined, the app points to the deployed Render backend, so a local frontend must set it explicitly. Styling is plain CSS; no Tailwind or UI framework is used.

A small status widget in the header polls `/actuator/health` on page load and shows three states: backend asleep or unreachable, API up but database down, and everything healthy.

### Backend

The backend follows a conventional layered structure:

```text
controller -> service -> repository -> PostgreSQL
```

- `GameController` exposes the REST endpoints.
- `GameService` contains the business rules: game creation, participant registration, participant deletion, listing and the draw.
- `GameRepository` and `ParticipantRepository` are Spring Data JPA repositories.
- `EmailService` sends the assignment email through the Brevo HTTP API.
- `GlobalExceptionHandler` maps domain exceptions to HTTP status codes.

### The draw

The draw uses Sattolo's algorithm, which produces a random cyclic permutation. This guarantees that:

- no participant is assigned to themselves;
- every participant appears exactly once as a receiver;
- the result is produced on the first attempt, without retry loops.

Once the draw is done, the game is marked as assigned and participants can no longer be deleted, so the existing pairs stay consistent.

### Email safety

- Full email addresses are never returned by the API; `ParticipantDTO` masks them (for example `an***@test.com`).
- Logs use masked addresses plus the `messageId` that Brevo returns for each accepted message, which can be searched in the Brevo dashboard.
- User-provided names and wishlists are HTML-escaped before being inserted into the email.
- The Brevo payload is built with Jackson, so quotes or line breaks cannot break the JSON.
- Emails are sent in parallel through HTTPS requests; the API key is never logged.
- Rate limits and a maximum of 100 participants per game reduce abuse of the email flow.

If `MAIL_FROM` or `BREVO_API_KEY` is missing, the application still starts and the draw still works, but no email is sent. The problem is logged at startup and the draw response reports how many messages the mail provider accepted, so the UI never claims delivery when nothing was sent.

| Startup state | Log | Draw response |
| --- | --- | --- |
| `MAIL_FROM` and `BREVO_API_KEY` set | `Email delivery enabled. sender=...` | Real count accepted by Brevo |
| `MAIL_FROM` empty, key set | `Email delivery misconfigured: MAIL_FROM is not set` | `emailsDispatched: 0` |
| `BREVO_API_KEY` is an SMTP key | `Email delivery misconfigured: ... SMTP key ...` | `emailsDispatched: 0` |
| No `BREVO_API_KEY` | `Email delivery disabled` and simulated emails | `emailsDispatched: 0` |

Without credentials, local runs simply skip delivery: the draw succeeds and the console prints the pair that would have been sent.

## API

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/games` | List games with budget, date, participant count and status |
| `POST` | `/api/games` | Create a game and return its share code |
| `POST` | `/api/games/{gameId}/participants` | Add a participant to a game |
| `GET` | `/api/games/{gameCode}/participants` | List participants with masked emails |
| `POST` | `/api/games/{gameId}/assign` | Run the draw and return how many emails Brevo accepted (`emailsDispatched`) out of `participants` |
| `DELETE` | `/api/games/{gameId}/participants/{participantId}` | Remove a participant before the draw |

Game codes are 8 characters, uppercase. Requests and responses use JSON.

Quick check once the backend is running:

```bash
curl -X POST http://localhost:8080/api/games \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","maxPrice":10,"exchangeDate":"2026-12-24"}'
```

## Requirements

- JDK 25
- Node.js 20.19+ or 22.12+
- PostgreSQL 16, or Docker for the database
- Maven is not required: the Maven wrapper is included

Docker is optional. It is the fastest way to get PostgreSQL, but an existing local server works too.

## Run locally

### 1. Database

Start PostgreSQL with Docker:

```bash
docker compose up -d postgres
```

This creates the database `secretsanta` with user `postgres` and password `postgres`, matching the defaults in `server/src/main/resources/application.properties`.

If you already have a local PostgreSQL instance, create a database named `secretsanta` and adjust the credentials with the `SPRING_DATASOURCE_*` environment variables.

### 2. Backend

```bash
cd server
./mvnw spring-boot:run
```

On Windows:

```powershell
cd server
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`. Tables are created or updated by Hibernate on startup, so no manual migration is needed.

Optional environment variables for email delivery:

```text
MAIL_FROM=verified-sender@example.com
MAIL_FROM_NAME=Secret Santa Organizer
BREVO_API_KEY=your-brevo-api-key
```

Set them in your shell or in the run configuration of your IDE. On PowerShell:

```powershell
$env:MAIL_FROM = "verified-sender@example.com"
$env:BREVO_API_KEY = "xkeysib-..."
.\mvnw.cmd spring-boot:run
```

### 3. Frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

On Windows, copy the file with `Copy-Item .env.example .env`.

`.env` contains:

```text
PUBLIC_API_URL=http://localhost:8080
```

The app is available at `http://localhost:5173`.

If `PUBLIC_API_URL` is omitted, the frontend calls the deployed Render API instead of your local backend. Keep it set while developing.

### 4. Tests and checks

```bash
cd server
./mvnw test
```

Backend tests use an in-memory H2 database and do not require PostgreSQL. They cover the draw, participants, email formatting, the Brevo integration (with HTTP mocked), rate limits, masking and the health endpoint.

```bash
cd frontend
npm run check
npm run build
```

`npm run check` runs `svelte-check` and reports type and accessibility issues.

### 5. Full backend stack with Docker

```bash
docker compose up --build
```

This starts PostgreSQL and the Spring Boot API on `http://localhost:8080`. The frontend is not included in the Compose file; run it separately with `npm run dev`.

### Local troubleshooting

| Symptom | Likely cause |
| --- | --- |
| `Connection refused` on `localhost:5432` | PostgreSQL is not running; start it with `docker compose up -d postgres` |
| `Port 8080 is already in use` | Another process owns the port; stop it or run with `--server.port=8081` and update `PUBLIC_API_URL` |
| The UI shows no participants | The frontend points to Render; check `PUBLIC_API_URL` in `frontend/.env` |
| CORS error in the browser | The frontend origin is not allowed in `CorsWebConfig`; localhost is allowed by default |
| Draw returns `emailsDispatched: 0` | `MAIL_FROM` or `BREVO_API_KEY` is missing or invalid; the startup log explains which one |
| Draw returns HTTP 409 | The game was already drawn; create a new game to test again |

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/secretsanta` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `DB_POOL_SIZE` | `5` | Hikari maximum pool size |
| `DB_POOL_MIN_IDLE` | `2` | Hikari minimum idle connections |
| `MAIL_FROM` | empty | Verified sender address in Brevo |
| `MAIL_FROM_NAME` | `Secret Santa Organizer` | Sender display name |
| `BREVO_API_KEY` | empty | Brevo API v3 key (`xkeysib-...`) |
| `RATE_LIMIT_ENABLED` | `true` | Enable or disable rate limiting |
| `RATE_LIMIT_CREATE_GAMES` | `5` | Games per hour and IP |
| `RATE_LIMIT_JOIN` | `10` | Joins per minute and IP |
| `RATE_LIMIT_ASSIGN` | `3` | Draws per minute and IP |
| `PUBLIC_API_URL` | deployed backend | Frontend API base URL |

Secrets such as `MAIL_FROM` and `BREVO_API_KEY` are read from environment variables and must never be committed. The repository only contains empty defaults.

`BREVO_API_KEY` must be an API v3 key, which starts with `xkeysib-`. The SMTP key (`xsmtpsib-`) is a different credential and is rejected by the HTTP API. The startup log reports the SMTP key mistake without printing the key.

## Health endpoints

- `GET /actuator/health`: full status, including the database.
- `GET /actuator/health/liveness`: process status only, without database access.

`/actuator/health` returns `UP` only when the application and the database are healthy. The frontend uses it to show the connection status.

## Design decisions and limitations

- There are no user accounts or passwords. A game code grants access to its game, which keeps the flow frictionless but means anyone with the code can join, run the draw or delete participants before the draw.
- Emails are not verified with a double opt-in. Rate limits and the participant cap reduce abuse, but they do not prove ownership of an address.
- Rate limits are stored in memory, so they reset when the backend restarts and are not shared between multiple instances.
- `GET /api/games` lists every game. This is acceptable for the current scope but should be restricted if the application becomes multi-tenant.
- A database unique constraint on `(game_id, email)` is not yet defined; duplicate checks are performed in the service layer.

## License

Apache License 2.0. See `LICENSE`.
