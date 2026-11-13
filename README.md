# Secret Santa Organizer

Full-stack application for organizing Secret Santa gift exchanges. A game is created with a budget and a date, participants join with a share code, and the backend performs a fair random draw and emails each participant their assigned person.

The project is split into a SvelteKit frontend and a Spring Boot API, with PostgreSQL as the database. The frontend is deployed on Vercel and the backend on Render.

## Stack

| Layer | Technology |
| --- | --- |
| Frontend | SvelteKit 2, Svelte 5, Vite, native CSS |
| Backend | Spring Boot 4, Java 25, Maven |
| Database | PostgreSQL 16 |
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

1. **Crear intercambio**: sends `POST /api/games` with the name, budget and date, then shows the generated share code.
2. **Unirme**: sends `POST /api/games/{code}/participants` with the participant data and wishlist.

The Santa panel is a modal opened from the header. It loads the participants of a game with `GET /api/games/{code}/participants`, shows masked email addresses, and triggers the draw with `POST /api/games/{code}/assign`.

The API base URL is read from `PUBLIC_API_URL` (see `frontend/src/lib/config.js`). If it is not defined, the app points to the deployed Render backend. Styling is plain CSS; no Tailwind or UI framework is used.

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

The draw uses Sattolo's algorithm, which produces a random cyclic permutation. This guarantees that:

- no participant is assigned to themselves;
- every participant appears exactly once as a receiver;
- the result is produced on the first attempt, without retry loops.

Once the draw is done, the game is marked as assigned and participants can no longer be deleted, so the existing pairs stay consistent.

### Email safety

- Full email addresses are never returned by the API; `ParticipantDTO` masks them (for example `an***@test.com`).
- Logs use masked addresses.
- User-provided names and wishlists are HTML-escaped before being inserted into the email.
- The Brevo payload is built with Jackson, so quotes or line breaks cannot break the JSON.
- Emails are sent in parallel through HTTPS requests; the API key is never logged.
- Rate limits and a maximum of 100 participants per game reduce abuse of the email flow.

If `MAIL_FROM` or `BREVO_API_KEY` is missing, the application still starts and the draw still works, but no email is sent. The problem is logged at startup and the draw response reports how many messages the mail provider accepted, so the UI never claims delivery when nothing was sent.

| Startup state | Log | Draw response |
| --- | --- | --- |
| `MAIL_FROM` and `BREVO_API_KEY` set | `Email delivery enabled with sender: ...` | Real count accepted by Brevo |
| `MAIL_FROM` empty, key set | `Email delivery misconfigured: MAIL_FROM is not set` | `emailsDispatched: 0` |
| `BREVO_API_KEY` is an SMTP key | `Email delivery misconfigured: ... SMTP key ...` | `emailsDispatched: 0` |
| No `BREVO_API_KEY` | `Email delivery disabled` and simulated emails | `emailsDispatched: 0` |

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

## Requirements

- JDK 25
- Node.js 20.19+ or 22.12+
- PostgreSQL 16, or Docker for the database
- Maven is not required: the Maven wrapper is included

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

The API starts on `http://localhost:8080`. Tables are created or updated by Hibernate on startup.

Optional environment variables for email:

```text
MAIL_FROM=verified-sender@example.com
MAIL_FROM_NAME=Secret Santa Organizer
BREVO_API_KEY=your-brevo-api-key
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

If `PUBLIC_API_URL` is omitted, the frontend uses the deployed Render API.

### 4. Tests and checks

```bash
cd server
./mvnw test
```

Backend tests use an in-memory H2 database and do not require PostgreSQL.

```bash
cd frontend
npm run check
npm run build
```

### 5. Full backend stack with Docker

```bash
docker compose up --build
```

This starts PostgreSQL and the Spring Boot API on `http://localhost:8080`. The frontend is not included in the Compose file; run it separately or deploy it to Vercel.

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
| `PUBLIC_API_URL` | Render URL | Frontend API base URL |

Secrets such as `MAIL_FROM` and `BREVO_API_KEY` must be configured as environment variables in Render, not committed to the repository.

`BREVO_API_KEY` must be an API v3 key, which starts with `xkeysib-`. The SMTP key (`xsmtpsib-`) is a different credential and is rejected by the HTTP API. The startup log reports the SMTP key mistake without printing the key.

## Deployment

### Render

Create a web service from the repository using the root `Dockerfile`. Configure at least:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
MAIL_FROM
BREVO_API_KEY
```

`MAIL_FROM` must be a sender verified in Brevo.

The free PostgreSQL instance on Render expires 30 days after creation. For a long-lived deployment, use Supabase (see below) or a paid plan.

### Supabase database

Supabase can replace Render PostgreSQL. Use the **Session pooler** (port 5432): the shared pooler is IPv4-compatible and supports prepared statements, which Hibernate uses.

1. Create a Supabase project and open **Connect** in the dashboard.
2. Copy the **Session pooler** connection string:
   ```text
   postgresql://postgres.<project-ref>:<password>@aws-0-<region>.pooler.supabase.com:5432/postgres
   ```
3. Set these variables on Render:
   ```text
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   SPRING_DATASOURCE_USERNAME=postgres.<project-ref>
   SPRING_DATASOURCE_PASSWORD=<password>
   ```
4. Remove the old `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD` variables.

`sslmode=require` is mandatory. Percent-encode reserved characters in the password (`@`, `:`, `/`, `?`, `#`). The application caps the Hikari pool at 5 connections, well below the pooler limit.

To migrate existing data:

```bash
pg_dump -Fc --no-owner --no-acl "<render-external-url>" -f secretsanta.dump
pg_restore --no-owner --no-acl -d "<supabase-session-pooler-url>" secretsanta.dump
```

Free Supabase projects pause after 7 days without database activity. The liveness probe does not touch the database, so it does not prevent this. A daily request to `/actuator/health` is enough to keep the project active; otherwise, resume it from the dashboard. Data is kept for up to one year after pausing.

### Health check and keep-alive

The API exposes two endpoints:

- `GET /actuator/health`: full status, including the database.
- `GET /actuator/health/liveness`: process status only, without database access.

On platforms that suspend idle services, an external cron can keep the API warm during the hours you need it. With cron-job.org, the expression `*/10 8-23 * * *` (timezone `Europe/Madrid`) sends a request every 10 minutes from 08:00 to 23:59. That is about 480 instance hours per month, well below Render's 750-hour free allowance.

Point the cron to `/actuator/health/liveness` so every request does not query the database.

### Vercel

Deploy the `frontend` directory. Set `PUBLIC_API_URL` to the public Render URL and use the default build command:

```bash
npm run build
```

CORS is configured in `server/src/main/java/com/kdmm/secretsanta/config/CorsWebConfig.java`. It currently allows `https://*.vercel.app` and localhost. Update it if you use a custom domain.

## Design decisions and limitations

- There are no user accounts or passwords. A game code grants access to its game, which keeps the flow frictionless but means anyone with the code can join, run the draw or delete participants before the draw.
- Emails are not verified with a double opt-in. Rate limits and the participant cap reduce abuse, but they do not prove ownership of an address.
- Rate limits are stored in memory, so they reset when the backend restarts and are not shared between multiple instances.
- `GET /api/games` lists every game. This is acceptable for the current scope but should be restricted if the application becomes multi-tenant.
- A database unique constraint on `(game_id, email)` is not yet defined; duplicate checks are performed in the service layer.
