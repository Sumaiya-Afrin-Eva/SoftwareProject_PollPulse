# PollPulse

PollPulse is a campus election system for managing election accounts and access.
It provides registration, profiles, and administrative user management.

## Run with Docker

1. Copy `.env.example` to `.env`.
2. Edit the values in `.env`, including the database and seed passwords.
3. Run `docker compose up --build`.
4. Open http://localhost:8080.
5. Stop with `docker compose down`; add `-v` only if you want to delete the database data.

## Run the app locally with the database in Docker

1. Copy `.env.example` to `.env` and replace its placeholder values.
2. Start PostgreSQL with `docker compose up -d db`.
3. Export the application variables in your terminal, using the corresponding `.env` values:

   ```sh
   export DB_URL='jdbc:postgresql://localhost:5432/<POSTGRES_DB>'
   export DB_USERNAME='<POSTGRES_USER>'
   export DB_PASSWORD='<POSTGRES_PASSWORD>'
   export SPRING_PROFILES_ACTIVE='prod'
   export SEED_DEMO_USERS='true'
   export SEED_PASSWORD='<SEED_PASSWORD>'
   ```

4. Run `./mvnw spring-boot:run`.

## Run from VS Code

Start the database with `docker compose up -d db`. In a Java launch
configuration for `com.pollpulse.PollpulseApplication`, set the `env` values
for `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE`,
`SEED_DEMO_USERS`, and `SEED_PASSWORD`, using the corresponding values from
`.env`. Replace all angle-bracketed placeholders before launching. The
`.vscode` directory is gitignored, so keep local launch settings there and do
not commit credentials.
