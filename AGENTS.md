# AGENTS

## Fast Facts
- Single-module Maven project (Java 17, JavaFX 17); no monorepo tooling.
- Main app entrypoint is `com.educompus.app.PreviewApp` (wired in `pom.xml` via `javafx-maven-plugin`).
- UI assets are split across `src/main/resources/View/**` (FXML) and root `styles/educompus.css` (not under `src/main/resources`).

## Commands You Should Actually Use
- Run app: `mvn javafx:run`
- Run app with wrapper: `./mvnw javafx:run` (bash) or `mvnw.cmd javafx:run` (Windows cmd/PowerShell)
- Open a specific screen: `mvn javafx:run -Dfxml=View/front/FrontDashboard.fxml`
- Disable splash for faster UI iteration: `mvn javafx:run -Dsplash=false -Dfxml=View/back/BackShell.fxml`
- Run one test class: `mvn -Dtest=ProjectSubmissionRepositoryTest test`
- Run one test method: `mvn -Dtest=ProjectSubmissionRepositoryTest#listMineReturnsEmptyWhenNoIdentityProvided test`

## Test/DB Gotchas
- DB access defaults to local MySQL via `EducompusDB` (`jdbc:mysql://127.0.0.1:3306/educompus`, user `root`, empty password) unless JVM props override `educompus.jdbcUrl`, `educompus.dbUser`, `educompus.dbPass`.
- `pom.xml` sets Surefire env vars `TEST_USE_LOCAL_MYSQL=1`, `TEST_DB_USER=root`, `TEST_DB_PASS=`; full `mvn test` assumes local MySQL is available.
- Only some tests are DB-free (e.g. `ProjectSubmissionRepositoryTest`); many service tests call `TestMySqlHelper.init()` and hit live MySQL.
- `TEST_FORCE_SCHEMA_APPLY=1` in `TestMySqlHelper` currently tries `/sql/projects_schema.sql`, but that file is not present in `src/main/resources/sql`; forcing schema apply will fail unless that resource is added.

## Codebase Map (High-Signal)
- `src/main/java/com/educompus/app`: app bootstrap and global state (`PreviewApp`, `AppState`).
- `src/main/java/com/educompus/nav/Navigator`: central FXML loading + scene switching + CSS apply logic.
- `src/main/java/com/educompus/controller/front` and `.../back`: JavaFX controllers for FO/BO screens.
- `src/main/java/com/educompus/repository`: JDBC/file persistence (`EducompusDB`, repositories).
- `src/main/java/com/educompus/service`: business layer used by controllers/repositories.
- `src/main/resources/sql`: available SQL bundles are `user_schema.sql`, `courses_schema.sql`, `progress_schema.sql`.

## Repo-Specific Pitfalls
- Path fallbacks still reference legacy folder name `eduCompus-javafx` in several classes; avoid deleting/changing these fallbacks unless you update all related loaders.
- Runtime writes happen under `var/` (e.g. `var/exam_attempts.properties`, `var/certificates/*.pdf`); these are generated artifacts, avoid committing accidental churn.
