# Install and Run Port Royal Backend

This guide covers local development on Windows or Linux, the Docker Compose setup, IntelliJ IDEA,
and the production deployment that uses Nginx Proxy Manager and the external email queue worker.
If you only run the backend in Docker Compose, the Docker build provides its own Java/Maven builder;
install Java and Maven on the host when you want to build or run the project directly in IntelliJ
IDEA or from a local CLI.

> **Important:** the local `dev` profile uses HTTP on localhost, a temporary OAuth signing key, and
> Hibernate schema updates. Never expose it to the public Internet or use it in production.

## Contents

- [Requirements](#requirements)
- [Install the tools](#install-the-tools)
- [Get the source code](#get-the-source-code)
- [Start locally with Docker Compose](#start-locally-with-docker-compose)
- [Check the API and sign in](#check-the-api-and-sign-in)
- [Run and debug with IntelliJ IDEA](#run-and-debug-with-intellij-idea)
- [Deploy with production Docker Compose](#deploy-with-production-docker-compose)
- [Stop and restart](#stop-and-restart)
- [Troubleshooting](#troubleshooting)

## Requirements

- Java JDK 21
- Maven 3.9 or later
- Git
- Docker Engine with Docker Compose v2
  - Windows: Docker Desktop with WSL 2 enabled
  - Linux: Docker Engine and the Compose plugin
- PostgreSQL 16 or later for production. Local Compose starts PostgreSQL 17.

The API listens on port `8080`; PostgreSQL uses `5432`. The CLI temporarily listens on
`127.0.0.1:8765` for its OIDC login callback.

## Install the tools

### Windows 10/11

Install:

1. [Git for Windows](https://git-scm.com/download/win).
2. [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21). Install the **JDK**,
   not just the JRE.
3. [Apache Maven 3.9+](https://maven.apache.org/download.cgi). If using the ZIP distribution,
   extract it (for example, to `C:\tools\apache-maven-3.9.x`) and add its `bin` directory to `PATH`.
4. [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/). Complete its
   first-run setup and enable WSL 2 if prompted. Docker Desktop includes Docker Compose v2.

Open a **new PowerShell window** and check the tools:

```powershell
java -version
mvn -version
git --version
docker --version
docker compose version
```

Java must be version 21 and Maven must report Java 21. If a command is not found, reopen the
terminal and check your `PATH`.

### Linux (Ubuntu/Debian)

Install Git, Java, Maven, Docker and Docker Compose:

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk maven docker.io docker-compose-v2
sudo systemctl enable --now docker
```

If `openjdk-21-jdk` or `docker-compose-v2` is unavailable for your distribution, install Java 21
from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) and Docker Engine plus the
Compose plugin from the [official Docker instructions](https://docs.docker.com/engine/install/).

Check the tools:

```bash
java -version
mvn -version
git --version
sudo docker --version
sudo docker compose version
```

Java must be version 21 and Maven must be 3.9 or later. This guide uses `sudo docker`; you can
configure non-root Docker access separately by following your distribution's Docker documentation.

## Get the source code

If you already have this repository, open a terminal in its root directory (the directory containing
the root `pom.xml`) and continue to the next section.

Otherwise, create a working directory and clone the repository URL to which you have access:

**PowerShell**

```powershell
New-Item -ItemType Directory -Force C:\dev | Out-Null
Set-Location C:\dev
git clone <repository-url> portroyal_be_full
Set-Location .\portroyal_be_full
```

**Linux**

```bash
mkdir -p ~/dev
cd ~/dev
git clone <repository-url> portroyal_be_full
cd portroyal_be_full
```

Replace `<repository-url>` with the Git URL provided for your project.

## Start locally with Docker Compose

The root [`compose.yaml`](compose.yaml) starts PostgreSQL and the backend. It does not start the
external email worker: locally, the API queues emails in `emails_queue`, but no email is sent unless
you also run that worker. The local administrator created below is already email-confirmed, so you
can sign in without receiving an email.

### 1. Create the local environment file

From the repository root, copy the sample file:

**PowerShell**

```powershell
Copy-Item .env.example .env
notepad .env
```

**Linux**

```bash
cp .env.example .env
nano .env
```

In `.env`, replace `POSTGRES_PASSWORD` with a password used only by this local database and replace
`PORTROYAL_INITIAL_ADMIN_PASSWORD` with a unique local passphrase of at least 15 characters. Do not
reuse either value for any public or production service. `.env` is ignored by Git; never commit it.

### 2. Build and start the services

From the repository root:

```bash
docker compose up --build -d
```

On Linux, prefix it with `sudo` if your user does not have Docker access.

Compose builds the backend image from the root `Dockerfile`, starts PostgreSQL, waits for its
health-check, then starts the API. The `dev` profile creates a temporary signing key, and
`application-dev.properties` lets Hibernate create/update the local game tables. Flyway creates the
versioned OAuth and security tables. No manual SQL import is needed for this local setup.

Check the containers and backend log:

```bash
docker compose ps
docker compose logs --tail=100 be_app
```

On Linux, add `sudo` if needed. Wait for a `Started ...` message. Follow live logs with
`docker compose logs -f be_app`; press `Ctrl+C` to stop following the log, not the container.

### 3. What the local Compose setup creates

- PostgreSQL database `portroyal`, stored in the named volume `portroyal_dev_data`.
- Local `admin` account with `ADMIN` and `USER` roles, using the passphrase from `.env`.
- Technical `shaslabot` identity without a password.
- OIDC browser and CLI clients.
- No Telegram bot OAuth client unless `PORTROYAL_BOT_CLIENT_SECRET` is set.

Re-running `docker compose up` reuses the same database and data volume.

## Check the API and sign in

Open:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenID Connect discovery: <http://localhost:8080/.well-known/openid-configuration>
- OpenAPI JSON: <http://localhost:8080/api/v1/api-docs>

Swagger and discovery are public. Protected API endpoints require an OAuth bearer token; they do
not accept HTTP Basic.

### Sign in with the CLI

Open a second terminal in `portroyal_be`:

**PowerShell**

```powershell
Set-Location C:\dev\portroyal_be_full\portroyal_be
mvn exec:java
```

**Linux**

```bash
cd ~/dev/portroyal_be_full/portroyal_be
mvn exec:java
```

In the CLI menu:

1. Select `16` (**Sign in with OpenID Connect**).
2. Complete sign-in in the system browser with username `admin` and your local passphrase.
3. Select `2` (**Show current user**) to verify the login.
4. Select `17` (**Sign out**) to revoke the refresh token and clear CLI memory.

The CLI uses Authorization Code + PKCE. It never saves access or refresh tokens to disk; sign in
again after restarting the CLI.

## Run and debug with IntelliJ IDEA

Use Compose for PostgreSQL and let IntelliJ run the Java API on your computer. This is useful for
breakpoints, stepping through code, and viewing application output in the IDE.

### 1. Import the Maven project

1. Start the local Compose stack once as described above. This creates the database and local admin.
2. Stop only its API container, leaving PostgreSQL running:

   ```bash
   docker compose stop be_app
   ```

   On Linux, add `sudo` if needed.
3. In IntelliJ IDEA, choose **File → Open**, select the repository root `pom.xml`, and open it as a
   project.
4. In **File → Project Structure**, set the Project SDK and language level to Java 21. In Maven
   settings, select Maven 3.9+ running on JDK 21.
5. Wait for Maven import to finish. If needed, run `mvn clean install` once in the IDE terminal.

### 2. Create the backend run configuration

Choose **Run → Edit Configurations → + → Spring Boot** and set:

| Setting | Value |
|---|---|
| Name | `PortRoyal Backend (dev)` |
| Main class | `com.matteopaciolla.prbe.Main` |
| Use classpath of module | `portroyal_be` |
| Working directory | `<repository-root>/portroyal_be` |
| Active profile | `dev` |

Add these environment variables to the run configuration (replace the password with the value in
`.env`):

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portroyal
SPRING_DATASOURCE_USERNAME=portroyal
SPRING_DATASOURCE_PASSWORD=<POSTGRES_PASSWORD from .env>
PORTROYAL_AUTH_ISSUER=http://localhost:8080
PORTROYAL_API_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

Do **not** put `PORTROYAL_INITIAL_ADMIN_PASSWORD` in a shared or committed IntelliJ run
configuration. Compose already created the local administrator in PostgreSQL.

Click **Run** or **Debug**. The application connects to PostgreSQL on `localhost:5432` and listens
on `localhost:8080`. Set breakpoints under `portroyal_be/src/main/java`.

### 3. Run the CLI from IntelliJ (optional)

Choose **Run → Edit Configurations → + → Application**:

| Setting | Value |
|---|---|
| Name | `PortRoyal CLI` |
| Main class | `com.matteopaciolla.prbe.cli.PortRoyalCli` |
| Use classpath of module | `portroyal_be` |

Run it in the IDE terminal if you need interactive console input, then select menu option `16` to
sign in through the browser.

## Deploy with production Docker Compose

The root [`docker-compose.yml`](docker-compose.yml) is the production stack based on the Compose
file you used previously. It:

- connects to the existing external PostgreSQL data volume (it refuses to invent a replacement);
- attaches `be_app` to the existing `npm_main_net` network so Nginx Proxy Manager can reach
  `be_app:8080`;
- does not publish PostgreSQL or the API ports directly to the host;
- builds `email_batch` from the external `email_sender_bot` checkout and passes the Mailjet,
  PostgreSQL-polling and health-check settings you provided.

The `email_sender_bot` source is not in this repository. By default, Compose expects it at
`../email_sender_bot`; set `EMAIL_WORKER_BUILD_CONTEXT` if it lives elsewhere. Do not run two email
pollers against the same queue simultaneously, or queued messages may be sent more than once.

### Production preparation checklist

1. Back up PostgreSQL and stop the old Compose stack, especially its email worker, before starting
   the new one.
2. Confirm Nginx Proxy Manager is running and owns the external network:

   ```bash
   docker network inspect npm_main_net
   ```

3. Find the **exact existing database volume name**:

   ```bash
   docker volume ls
   ```

   Set `POSTGRES_VOLUME_NAME` to that name in `.env.production`. The Compose volume is marked
   `external: true` so a typo fails instead of silently creating an empty production database.
4. Copy `.env.production.example` to `.env.production`, fill every placeholder, and restrict access
   to it. On Linux:

   ```bash
   cp .env.production.example .env.production
   chmod 600 .env.production
   ```

   Do not commit the populated file or paste it into logs/tickets. The template requires HTTPS
   issuer and redirect URLs, exact CORS origins, bot and Mailjet secrets, database credentials,
   callback encryption key, and signing keystore settings.
5. Set `POSTGRES_INIT_SCHEMA_FILE` to the domain schema SQL file. The supplied template points to
   `./portroyal_be/schema.sql`. PostgreSQL executes this file only when initializing an empty data
   volume; it does **not** rerun it against an existing database. Keep the old data volume and use
   the schema file matching your deployed backend version.
6. Verify the email worker path and its Dockerfile exist at `EMAIL_WORKER_BUILD_CONTEXT`.
7. Point your Nginx Proxy Manager host to upstream `be_app`, port `8080`, using the existing
   `npm_main_net`. Nginx Proxy Manager handles public HTTPS; `PORTROYAL_AUTH_ISSUER` and the
   registered client redirect URIs must use the public HTTPS host.
8. If migrating from the old default `admin/password` account, the backend disables that known
   credential. Provision a new administrator with `PORTROYAL_BOOTSTRAP_ADMIN_ENABLED=true` and a
   unique `PORTROYAL_INITIAL_ADMIN_PASSWORD`, then disable bootstrap and remove that password from
   the deployment environment.

Validate the resolved settings before deployment:

```bash
docker compose --env-file .env.production -f docker-compose.yml config
```

Then start and inspect the services:

```bash
docker compose --env-file .env.production -f docker-compose.yml up --build -d
docker compose --env-file .env.production -f docker-compose.yml ps
docker compose --env-file .env.production -f docker-compose.yml logs -f be_app email_batch
```

On Linux, prefix Docker commands with `sudo` if required. PostgreSQL is only on the private
`db_internal` network; the email worker has outbound network access but is not attached to Nginx
Proxy Manager's network.

## Stop and restart

- Stop local services without deleting the database:

  ```bash
  docker compose down
  ```

  **Do not add `-v`** unless you explicitly intend to delete local database data.
- Start them again with `docker compose up -d`.
- For production, use the same `--env-file .env.production -f docker-compose.yml` options every
  time.
- `docker compose logs -f ...` only follows logs. Press `Ctrl+C` to stop following them without
  stopping the containers.

## Troubleshooting

| Symptom | What to check |
|---|---|
| `java` or `mvn` is not found | Reopen the terminal after installing; verify JDK 21, Maven 3.9+ and `PATH`. |
| `docker compose` is not found | Install Docker Compose v2 (Windows Docker Desktop includes it). |
| Local connection refused on `localhost:5432` | Check Docker is running and `docker compose ps` reports PostgreSQL healthy. |
| `password authentication failed` | Datasource values must match `POSTGRES_USER`, `POSTGRES_PASSWORD` and `POSTGRES_DB` in `.env`. |
| `PORTROYAL_AUTH_KEYSTORE must be configured` | The app did not start with profile `dev`. Do not enable ephemeral signing keys in production. |
| Admin bootstrap password error | Use 15–128 characters; disable bootstrap if an administrator already exists. |
| CLI browser login does not return | Check port 8765 is free and the backend is reachable at `http://localhost:8080`. |
| Production says `npm_main_net` is missing | Ensure Nginx Proxy Manager has created the external network; do not remove `external: true`. |
| Production says the database volume is missing | Stop before starting; check `POSTGRES_VOLUME_NAME` against `docker volume ls`. |
| `email_batch` cannot build | Verify `EMAIL_WORKER_BUILD_CONTEXT` points to the external `email_sender_bot` checkout with its Dockerfile. |
