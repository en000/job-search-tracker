# Job Search Tracker

A local job search tracker for one person. Track applications, interviews, assignments, and follow-ups in a single PostgreSQL workspace. The app opens directly to Today.

## Quick start

Run these commands from the repository root with Docker and Compose v2 running. Docker Desktop includes both on Windows and macOS.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

On macOS or Linux, use `[ -f .env ] || cp .env.example .env`. If you created the file from the example, give `POSTGRES_PASSWORD` a local value before the first startup. Keep this file with your local setup.

```sh
docker compose up --build -d --wait
```

Open [Today](http://localhost:8080). The app and API share that address. Docker publishes the app on the host’s loopback interface, and PostgreSQL stays on the Compose network. The readiness endpoint is `/api/health`.

## Use the tracker

1. Select **Add role** and start it in **Saved** or **Applied**. Open it from **Roles** to update its stage, record activity, or manage follow-ups, interviews, and assignments.
2. Add interviews from the role’s **Interviews** section. The optional round number is the interview’s position in that role’s hiring process. Saving an interview can suggest a stage change. Choose **Move stage** to apply it or **Keep current stage** to skip it.
3. Give each assignment a due date. Open assignments appear in **Overdue**, **Due today**, or **Upcoming**. **To do** holds planned interviews, follow-ups without a due date, and any older open assignments that still need a date.
4. After 14 days at the same stage and substage, **Today** suggests **No response** for **Applied** and **In progress** roles. Review the suggestion, dismiss it for the current stage and substage, or choose **Remind me in 2 weeks**. Completing the role requires confirmation.
5. Use **Settings** to change the time zone for interview times and Today’s date. Assignment and follow-up due dates stay on the calendar date you chose.

| Role stage | Options |
| --- | --- |
| **Saved** | No substages |
| **Applied** | No substages |
| **In progress** | Interview scheduled, First interview passed, Awaiting interview feedback, Assignment pending, Assignment submitted, Final interview, Offer received |
| **Completed** | Choose Accepted, Rejected, Withdrawn, or No response. Reopening restores the previous active stage and substage. |

## Manage the local app

```sh
docker compose ps
docker compose logs -f app
```

Press Ctrl+C to stop following logs. Use `docker compose down` to stop both services and run the startup command again to resume your workspace. `docker compose down -v` removes the named PostgreSQL volume and permanently deletes the databases in it. The next startup seeds a fresh workspace.

If Docker builds report certificate errors on a network that inspects HTTPS, save the trusted local root CA as a PEM file and pass it as a build secret. The certificate is used by the dependency download stages.

```powershell
$env:TRACKER_BUILD_CA_FILE = "C:/path/to/trusted-root.pem"
docker compose -f compose.yaml -f compose.build-ca.yaml up --build -d
```

On macOS or Linux, use `export TRACKER_BUILD_CA_FILE=/path/to/trusted-root.pem` before the same Compose command.

## Backup and restore

Use **Settings → Download JSON** for a portable snapshot of the workspace. Use a PostgreSQL dump when you want to restore a working database.

Run these commands from the repository root with the database service running. The dump includes the schema, Flyway history, and data. Keep the resulting `tracker.dump` somewhere outside Docker's volume.

```sh
docker compose exec -T db sh -ec 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/tracker.dump'
docker compose cp db:/tmp/tracker.dump ./tracker.dump
```

To restore, copy the dump into the database container and load it into a fresh database on the same server. Use an unused database name for each restore. The original database stays available while you check the restored copy.

```sh
docker compose cp ./tracker.dump db:/tmp/tracker-restore.dump
docker compose exec -T db sh -ec 'pg_restore --list /tmp/tracker-restore.dump >/dev/null'
docker compose exec -T db sh -ec 'createdb -U "$POSTGRES_USER" jobtracker_restored'
docker compose exec -T db sh -ec 'pg_restore -U "$POSTGRES_USER" -d jobtracker_restored --single-transaction --exit-on-error /tmp/tracker-restore.dump'
docker compose exec -T db psql -U jobtracker -d jobtracker_restored -c 'select count(*) from roles'
```

Use your configured `POSTGRES_USER` in the last command if it differs from `jobtracker`.

Set `POSTGRES_DB=jobtracker_restored` in `.env` and run `docker compose up -d --wait`. Open the tracker and check the expected records. To switch back, restore the previous database name in `.env` and run the same command.

## Develop with hot reload

Use Java 21 and the Node version in [frontend/.nvmrc](frontend/.nvmrc). Stop the packaged app if it is using port 8080. The optional development override publishes PostgreSQL on loopback for the local Spring process.

```sh
docker compose down
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait db
```

From `backend`, start Spring Boot on loopback. The development profile reads the root `.env`. Use simple alphanumeric local credentials so both Compose and Spring can read the same values.

```sh
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

On Windows, replace `./mvnw` with `.\mvnw.cmd`. In a second terminal, run the following from `frontend`.

```sh
npm ci
npm run dev
```

Open [the Vite dev server](http://localhost:5173). Vite proxies `/api` to Spring.

## Run the checks

From `backend`, run the architecture, domain, and integration checks. Testcontainers starts a disposable PostgreSQL database. Keep the Docker engine running.

```sh
./mvnw verify
```

On Windows, run `.\mvnw.cmd verify`.

From `frontend`, run the type check, production build, and React Testing Library tests.

```sh
npm ci
npm run build
npm test
```

Run Playwright against a packaged Compose instance. Use a separate Compose project so the browser flow writes only to a disposable workspace. Start Compose from the repository root. Stop any existing instance that uses port 8080 first.

```sh
docker compose -p jobtracker-e2e up --build -d --wait
cd frontend
npm ci
npx playwright install chromium
npm run test:e2e
```

From the root, `docker compose -p jobtracker-e2e down` stops the test instance. Add `-v` when you want to discard that test workspace. Set `PLAYWRIGHT_BASE_URL` if you run the browser tests against a different local address configured for the API.

## Project structure

- `backend/` contains the Spring Boot app, Flyway migrations, Maven Wrapper, and Java tests.
- `frontend/` contains the React app, npm lockfile, and browser and component tests.
- `Dockerfile` builds the React assets into the Spring Boot JAR and runs it under a non-root user.
