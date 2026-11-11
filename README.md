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
- Emails are sent asynchronously through an HTTPS request; the API key is never logged.
- Rate limits and a maximum of 100 participants per game reduce abuse of the email flow.

If `MAIL_FROM` or `BREVO_API_KEY` is missing, the application still starts and the draw still works, but emails are skipped and a warning is logged.

## API

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/games` | List games with budget, date, participant count and status |
| `POST` | `/api/games` | Create a game and return its share code |
| `POST` | `/api/games/{gameId}/participants` | Add a participant to a game |
| `GET` | `/api/games/{gameCode}/participants` | List participants with masked emails |
| `POST` | `/api/games/{gameId}/assign` | Run the draw and send the emails |
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
| `MAIL_FROM` | empty | Verified sender address in Brevo |
| `MAIL_FROM_NAME` | `Secret Santa Organizer` | Sender display name |
| `BREVO_API_KEY` | empty | Brevo API key |
| `RATE_LIMIT_ENABLED` | `true` | Enable or disable rate limiting |
| `RATE_LIMIT_CREATE_GAMES` | `5` | Games per hour and IP |
| `RATE_LIMIT_JOIN` | `10` | Joins per minute and IP |
| `RATE_LIMIT_ASSIGN` | `3` | Draws per minute and IP |
| `PUBLIC_API_URL` | Render URL | Frontend API base URL |

Secrets such as `MAIL_FROM` and `BREVO_API_KEY` must be configured as environment variables in Render, not committed to the repository.

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
