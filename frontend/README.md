# Secret Santa Organizer · Frontend

SvelteKit single-page app for the Secret Santa Organizer. The full documentation, including how to run the backend and the database locally, is in the [root README](../README.md).

## Local development

```bash
npm install
cp .env.example .env
npm run dev
```

`PUBLIC_API_URL` in `.env` must point to the Spring Boot API, usually `http://localhost:8080`. Without it, the app calls the deployed backend.

The app runs at `http://localhost:5173`.

## Checks

```bash
npm run check
npm run build
```
