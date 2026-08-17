# interview-platform

A small HTTP service holding shipping port reference data. Kotlin and Ktor, backed by PostgreSQL.

## Endpoints

| Method | Path                | Purpose                                                      |
| ------ | ------------------- | ------------------------------------------------------------ |
| `GET`  | `/health`           | Liveness. Does not touch the database. Returns `200` whenever the process is running. |
| `GET`  | `/ready`            | Readiness. `200` when the database is reachable and the schema is present, `503` otherwise, with a `detail` field explaining why. |
| `GET`  | `/api/ports`        | List all ports.                                              |
| `GET`  | `/api/ports/{code}` | Fetch one port by code, case insensitive. `404` if unknown.   |
| `POST` | `/api/ports`        | Create a port. `201` on success, `409` on duplicate, `400` on invalid input. |

```bash
curl localhost:8080/api/ports

curl -X POST localhost:8080/api/ports \
  -H 'Content-Type: application/json' \
  -d '{"code":"GBSOU","name":"Southampton","country":"United Kingdom"}'
```

## Configuration

All configuration comes from environment variables.

| Variable            | Required | Default | Example                                 |
| ------------------- | -------- | ------- | --------------------------------------- |
| `DATABASE_URL`      | yes      | —       | `jdbc:postgresql://postgres:5432/ports` |
| `DATABASE_USER`     | yes      | —       | `ports`                                 |
| `DATABASE_PASSWORD` | yes      | —       | —                                       |
| `PORT`              | no       | `8080`  | `8080`                                  |

If a required variable is missing or blank, the process prints which ones and exits non-zero.

## Database

The service expects a table called `ports`. The schema, with a few seed rows, is in [`db/schema.sql`](db/schema.sql).

The service does not create or migrate the schema. It expects the table to exist already.

It does start when the database is unreachable, reporting the problem on `/ready` rather than refusing to boot, so `/health` stays answerable while the database is down.

## Building and running

Requires JDK 21 or newer. Gradle comes via the wrapper.

```bash
./gradlew build          # compile and run the tests
./gradlew installDist    # produces a runnable distribution
./gradlew run            # run directly, needs the variables above
```

`installDist` writes a start script and its jars to `build/install/interview-platform/`.

Running against a local PostgreSQL:

```bash
docker run -d --name ports-db \
  -e POSTGRES_DB=ports -e POSTGRES_USER=ports -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 postgres:17

psql postgresql://ports:secret@localhost:5432/ports -f db/schema.sql

DATABASE_URL=jdbc:postgresql://localhost:5432/ports \
DATABASE_USER=ports \
DATABASE_PASSWORD=secret \
  ./gradlew run
```

## Tests

```bash
./gradlew test
```

Covers configuration parsing and the HTTP routes, using a fake repository. No Docker or database required.
