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

- [x] Login/logout and token handling (`/api/users`, `/api/sessions`)
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

## Sprint 4: Worker Services (OCR, MinIO, ELK)

- [x] MinIO runs in its own container (docker-compose.yml)
- [x] REST server stores the uploaded PDF in MinIO (`FileStorage` / `MinioFileStorage`)
- [x] The stored PDF can be downloaded again (`GET /api/documents/{id}/content`)
- [ ] OCR worker fetches the PDF from MinIO and runs Ghostscript + Tesseract
- [ ] Elasticsearch runs in its own container (docker-compose.yml)
- [ ] Indexing worker stores the OCR text in Elasticsearch
- [ ] Full-text search over Elasticsearch (searching "Hello" finds HelloWorld.pdf)

## REST server (`paperless-rest`)

Java 25, Spring Boot 3.5, PostgreSQL, MinIO.

### Authentication

All `/api/documents` endpoints need a token. Register, log in and send the token as
`Authorization: Bearer <accessToken>`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/users` | Create a user (body: `username`, `email`, `password`) → 201 |
| POST | `/api/sessions` | Log in (body: `username`, `password`) → token |
| DELETE | `/api/sessions/current` | Log out: revoke the current token → 204 |
| GET | `/api/users/me` | The logged-in user |

### Documents

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents` (multipart: `file`, optional `title`) | Upload a PDF, it is stored in MinIO → 201 + `Location`, 415 if the file is not a PDF, 503 if MinIO is not reachable |
| GET | `/api/documents` | List all documents incl. their tags and `ocrStatus` (204 if there are none) |
| GET | `/api/documents?q=` | Search by title |
| GET | `/api/documents/{id}` | Get one document |
| GET | `/api/documents/{id}/content` | Download the stored PDF → 200, 404 if the document has no stored file, 503 if MinIO is not reachable |
| PUT | `/api/documents/{id}` | Update the title |
| DELETE | `/api/documents/{id}` | Delete a document and its file in MinIO |
| POST | `/api/documents/{id}/tags` (body: `{"name": "..."}`) | Add a tag → 201, 409 if the document already has it |
| GET | `/api/documents/{id}/tags` | List tags (204 if there are none) |
| DELETE | `/api/documents/{id}/tags/{name}` | Remove a tag → 204 |

Tag names are trimmed and lower-cased, so `Finance` and ` finance ` are the same tag.
Tag names must not contain `/` or `\`.

## File storage (MinIO)

The PDF itself is not stored in PostgreSQL. The database only keeps the metadata and the object key
(`storage_key`), the file lives in the MinIO bucket `documents`.

```
POST /api/documents -> FileStorage.store (MinIO, object key = random UUID) -> DocumentRepository.save -> queue
```

- `storage/FileStorage` is the interface used by the service layer, `MinioFileStorage` is the MinIO
  implementation (`MinioConfig` creates the `MinioClient`). The bucket is created on first use.
- Order on upload: file first, then database row. If saving the row fails, the file is removed again.
  If MinIO is not reachable nothing is saved and the API answers 503.
- Order on delete: the database row is deleted first, then the file. If MinIO fails here the error is
  only logged (the database is the source of truth), the file stays orphaned in the bucket.
- Documents uploaded before sprint 4 have no stored file (`storage_key` is empty), their download returns 404.

| Environment variable | Default | Description |
|---|---|---|
| `MINIO_ENDPOINT` | `http://localhost:9000` | S3 API of MinIO |
| `MINIO_USER` | `paperless` | Access key |
| `MINIO_PASSWORD` | `paperless123` | Secret key (MinIO needs at least 8 characters) |
| `MINIO_BUCKET` | `documents` | Bucket for the PDFs |

MinIO console: http://localhost:9001 (`paperless` / `paperless123`)

## Queues (RabbitMQ + `paperless-worker`)

```
POST /api/documents -> paperless-rest -> default exchange -> queue "paperless.ocr" -> paperless-worker
```

There is only one kind of message, so no routing key logic is needed: the REST server sends the
`DocumentUploadedEvent` through the default exchange straight to the queue `paperless.ocr`
(the queue name is the routing key). A topic exchange would only make sense once several event types
(e.g. `document.uploaded`, `document.ocr-done`) go through the same exchange.

- `paperless-rest` (`messaging/`): `DocumentUploadedPublisher` (implements `DocumentEventPublisher`)
  sends a `DocumentUploadedEvent` after the document is saved (`DocumentServiceImpl.upload`).
  The event contains `documentId`, `title`, `filename` and `storageKey` (the object key in MinIO),
  so that the worker can fetch the PDF.
  If RabbitMQ is not reachable the error is logged and the upload still returns 201 (deliberate decision:
  the document is stored, but this one message is lost; there is no retry yet).
- `ocrStatus` of a document: `PENDING` when the message was sent, `FAILED` when RabbitMQ could not be
  reached (so lost messages are visible in the API and on the detail page). The worker will set `DONE`
  in sprint 4. Documents from before this column existed have no status.
- `paperless-worker`: `RabbitConfig` declares the queue, `DocumentUploadedListener` receives the event
  and writes a log entry. The actual OCR comes in sprint 4.

Check it: upload a PDF, then `docker compose logs paperless-worker` shows
`OCR worker received document id=..., title='...', file='...'`.

RabbitMQ UI: http://localhost:15672 (`paperless` / `paperless`)

## Web UI (`paperless-webui`)

Static HTML/CSS/JS, served by nginx.

| Page | Description |
|---|---|
| `/` (`index.html`) | Login and registration |
| `dashboard.html` | List documents, upload new ones, filter by tag |
| `detail.html` | View/edit a document's title and OCR status, manage its tags, delete it |


The UI talks to the REST server directly from the browser (`http://localhost:8080/api`).
Login stores the JWT in `localStorage` and sends it as `Authorization: Bearer <token>`
on every subsequent request.

Open: http://localhost/

### Example

```bash
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' \
     -d '{"username":"jarod","email":"jarod@example.com","password":"secret123"}'
TOKEN=$(curl -s -X POST localhost:8080/api/sessions -H 'Content-Type: application/json' \
     -d '{"username":"jarod","password":"secret123"}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" -F "file=@HelloWorld.pdf" localhost:8080/api/documents
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/documents/1/content -o back.pdf
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

The messaging classes are tested without a running RabbitMQ. The file storage is tested with a
mocked `FileStorage`, so the unit tests need no running MinIO.

### Run with Docker

```bash
docker compose up --build
```

Starts the database, RabbitMQ, MinIO, REST server, worker and web UI together.

- Web UI: http://localhost/
- REST API / Swagger: http://localhost:8080/swagger-ui.html
- RabbitMQ management: http://localhost:15672
- MinIO console: http://localhost:9001

Set `JWT_SECRET` for anything other than local development.

### Run locally

Needs a PostgreSQL database `paperless` (user/password `paperless`) on port 5432,
or set `DB_URL`, `DB_USER`, `DB_PASSWORD`. For the queues and the file storage, start RabbitMQ and MinIO with
`docker compose up -d rabbitmq minio` (or set `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` and
`MINIO_ENDPOINT`, `MINIO_USER`, `MINIO_PASSWORD`, `MINIO_BUCKET`).

```bash
cd paperless-rest
./mvnw spring-boot:run
```
