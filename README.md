# SWEN3-DMS To Dos

## Sprint 1: Project-Setup, REST API, DAL (with Mapping)

- [x] 1. Java/C# Project Setup
- [x] 2. Remote Repository setup - all team members are able to commit/push
- [x] 3. REST Server created - Endopints defined by the team (code-first)
- [x] 4. ORM is integrated to persist the entities on the PostgreSQL database, use the repository pattern
- [x] 5. Show correct function with unit-tests, mock out the “production” database
- [x] 6. Initial docker-compose.yml, used to run the REST-server & database inside containers
- [x] 7. Implement your additional use-case in your project (this must contain additional entities)

## REST server (`paperless-rest`)

Java 25, Spring Boot 3.5, PostgreSQL.

| Method | Path | Description |
|---|---|---|
| POST | `/api/documents` (multipart: `file`, optional `title`) | Upload a PDF |
| GET | `/api/documents` | List all documents |
| GET | `/api/documents/{id}` | Get one document |
| GET | `/api/documents/search?q=` | Search by title |
| PUT | `/api/documents/{id}` | Update the title |
| DELETE | `/api/documents/{id}` | Delete a document |
| POST   | /api/documents/{id}/tags | Add a tag to a document (body: `{"name": "..."}`) |
| GET    | /api/documents/{id}/tags | List tags on a document |

Swagger UI: http://localhost:8080/swagger-ui.html

### Run tests

```bash
cd paperless-rest
./mvnw test
```

Coverage report: `paperless-rest/target/site/jacoco/index.html`

### Run locally

Needs a PostgreSQL database `paperless` (user/password `paperless`) on port 5432,
or set `DB_URL`, `DB_USER`, `DB_PASSWORD`.

```bash
cd paperless-rest
./mvnw spring-boot:run
```
