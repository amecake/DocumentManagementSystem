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

- [x] RabbitMQ runs in its own container (docker-compose.yml)
- [x] Queues integrated into the REST server
- [x] On upload the REST server sends a message to RabbitMQ
- [x] The message is processed by an "empty" OCR worker (own container, only logs it)
- [x] Failure handling: if RabbitMQ is down the upload still works, the error is logged
- [x] Logging when a message is sent / received / fails

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

## Queues (RabbitMQ + `paperless-worker`)

```
POST /api/documents -> paperless-rest -> exchange "document.events" -> queue "paperless.ocr" -> paperless-worker
                                         (routing key "document.uploaded")
```

- `paperless-rest` (`messaging/`): `RabbitConfig` declares the exchange, `DocumentUploadedPublisher`
  sends a `DocumentUploadedEvent` after the document is saved (`DocumentService.upload`).
  If RabbitMQ is not reachable the error is logged and the upload still returns 201.
- `paperless-worker`: `RabbitConfig` declares the queue and binds it to the exchange,
  `DocumentUploadedListener` receives the event and writes a log entry. The actual OCR comes in sprint 4.

Check it: upload a PDF, then `docker compose logs paperless-worker` shows
`OCR worker received document id=..., title='...', file='...'`.

RabbitMQ UI: http://localhost:15672 (`paperless` / `paperless`)

## Web UI (`paperless-webui`)

Static HTML/CSS/JS, served by nginx.

| Page | Description |
|---|---|
| `/` (`index.html`) | Login and registration |
| `dashboard.html` | List documents, upload new ones, filter by tag |
| `detail.html` | View/edit a document's title, manage its tags, delete it |


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

The messaging classes are tested without a running RabbitMQ.

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
