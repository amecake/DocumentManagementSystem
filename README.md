# SWEN3-DMS To Dos

## Sprint 1: Project-Setup, REST API, DAL (with Mapping)

- [x] 1. Java/C# Project Setup
- [x] 2. Remote Repository setup - all team members are able to commit/push
- [x] 3. REST Server created - Endopints defined by the team (code-first)
- [x] 4. ORM is integrated to persist the entities on the PostgreSQL database, use the repository pattern
- [x] 5. Show correct function with unit-tests, mock out the “production” database
- [x] 6. Initial docker-compose.yml, used to run the REST-server & database inside containers
- [x] 7. Implement your additional use-case in your project (this must contain additional entities)

## Sprint 2

Fixes from the Sprint 1 code review:

- [x] Login/logout and token handling (`/api/auth`)
- [x] Unknown URLs return 404, bad requests (e.g. upload without file) return 400
- [x] `DocumentService` no longer takes a `MultipartFile`, input validation is in the controller
- [x] 201 with `Location` header, 204 for empty results, `ResponseEntity` in the controller
- [x] Search via `GET /api/documents?q=` instead of `/search`
- [x] Tags: blank names rejected, names normalised, 409 for duplicates, remove endpoint,
      one-directional relation, `equals`/`hashCode` on `TagEntity`
- [x] Length limits (255 / 50) checked in the API → 400

Sprint 2 tasks:

- [x] Webserver (nginx) integrated
- [x] Web UI incl. user-input validation
- [x] Dashboard and detail pages served by the webserver
- [x] Web page talks to the REST server
- [x] docker-compose.yml runs the UI in its own container

## Sprint 3

- [x] RabbitMQ added to docker-compose.yml
- [x] Upload sends an OCR job to the queue
- [x] Worker service (own container) processes the jobs
- [x] Results go back over a second queue and are stored (status + text)
- [x] Scheduled job resends documents that got stuck
- [x] Status shown in the UI

## REST server (`paperless-rest`)

Java 25, Spring Boot 3.5, PostgreSQL.

### Authentication

All `/api/documents` endpoints need a token. Register, log in and send the token as
`Authorization: Bearer <accessToken>`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create a user (body: `username`, `email`, `password`) → 201 |
| POST | `/api/auth/login` | Log in (body: `username`, `password`) → token |
| POST | `/api/auth/logout` | Revoke the current token → 204 |
| GET | `/api/auth/me` | The logged-in user |

### Documents

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents` (multipart: `file`, optional `title`) | Upload a PDF → 201 + `Location` |
| GET | `/api/documents` | List all documents (204 if there are none) |
| GET | `/api/documents?q=` | Search by title |
| GET | `/api/documents/{id}` | Get one document |
| PUT | `/api/documents/{id}` | Update the title |
| DELETE | `/api/documents/{id}` | Delete a document |
| POST | `/api/documents/{id}/tags` (body: `{"name": "..."}`) | Add a tag → 201, 409 if the document already has it |
| GET | `/api/documents/{id}/tags` | List tags (204 if there are none) |
| DELETE | `/api/documents/{id}/tags/{name}` | Remove a tag → 204 |

Tag names are trimmed and lower-cased, so `Finance` and ` finance ` are the same tag.

Each document has a `status` (`QUEUED` → `COMPLETED` / `FAILED`), `processedAt` and `ocrText`,
filled in by the background worker (see below).

## Queues (RabbitMQ + `paperless-worker`)

```
upload -> paperless-rest -> ocr.queue -> paperless-worker
               ^                              |
               +------ ocr.result.queue <-----+
```

- After an upload is saved (status `QUEUED`), `OcrJobDispatcher` sends a job to `ocr.queue`.
  This happens after the commit so the worker never gets an id that isn't in the DB yet.
- `paperless-worker` is its own Spring Boot app/container. It processes the job and sends the
  result to `ocr.result.queue`.
- `OcrResultListener` stores the result -> `COMPLETED` (with `ocrText`) or `FAILED`.
- `OcrRetryScheduler` runs every minute and resends documents that are still `QUEUED` after 2 min
  (`OCR_RETRY_INTERVAL`, `OCR_RETRY_AFTER`).

If the worker is down the job just waits in the queue. If RabbitMQ is down the upload still works
(201), the error is logged and the scheduler sends the job later.

OCR itself is only simulated for now (`SimulatedOcrProcessor`) because we don't store the PDF
content yet.

RabbitMQ UI: http://localhost:15672 (`paperless` / `paperless`)

## Web UI (`paperless-webui`)

Static HTML/CSS/JS, served by nginx.

| Page | Description |
|---|---|
| `/` (`index.html`) | Login and registration |
| `dashboard.html` | List documents with their OCR status, upload new ones, filter by tag |
| `detail.html` | View/edit a document's title, OCR status and text, manage its tags, delete it |

While a document is `QUEUED` the pages reload every 3 s.

The UI talks to the REST server directly from the browser (`http://localhost:8080/api`).
Login stores the JWT in `localStorage` and sends it as `Authorization: Bearer <token>`
on every subsequent request.

Open: http://localhost/

### Example

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
     -d '{"username":"jarod","email":"jarod@example.com","password":"secret123"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
     -d '{"username":"jarod","password":"secret123"}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" -F "file=@HelloWorld.pdf" localhost:8080/api/documents
```

Swagger UI: http://localhost:8080/swagger-ui.html (use **Authorize** to paste the token)

### Run tests

```bash
cd paperless-rest
./mvnw test
```

Coverage report: `paperless-rest/target/site/jacoco/index.html`

Worker:

```bash
cd paperless-worker
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-25 mvn test
```

The messaging classes are tested with Mockito, so no RabbitMQ is needed for the tests.

### Run with Docker

```bash
docker compose up --build
```

Starts the database, RabbitMQ, REST server, worker and web UI together.

- Web UI: http://localhost/
- REST API / Swagger: http://localhost:8080/swagger-ui.html
- RabbitMQ management: http://localhost:15672

Set `JWT_SECRET` for anything other than local development.

### Run locally

Needs a PostgreSQL database `paperless` (user/password `paperless`) on port 5432,
or set `DB_URL`, `DB_USER`, `DB_PASSWORD`. For the queues, start RabbitMQ with
`docker compose up -d rabbitmq` (or set `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`).

```bash
cd paperless-rest
./mvnw spring-boot:run
```
