# Basera (Java) — notes for Claude

Claude Code loads this file automatically when a session starts in this folder.
**Read `PROGRESS.md` first.** It has the phase checklist, what's done, decisions made and the next steps.

## What this project is
A port of **Basera**, an Airbnb-style stay booking site with Host and Customer roles. The original Node/Express +
MongoDB + EJS version is in the sibling folder **`../basera`**. It's deployed on Render and is the **reference
implementation**: when porting a feature, read the matching Node files first and keep the behaviour the same
(routes, validation messages, business rules, page layout).

The project has to be in Java (a requirement given to the user). The user chose **Spring Boot + MongoDB**.

Stack: Spring Boot 4.1.1 · Java 25 · Spring Data MongoDB · Spring Security · Thymeleaf · Maven wrapper · Docker (Render deploy).

## Working with this user
- They know the Node version (they built it) but **know little to nothing about Java**. They asked for good
  explanations. For every step, explain what we're doing and **map each Java concept to the Node file or idea it
  replaces** (a table of "Java file → Node file it replaces" works well).
- Code comments are beginner-oriented and refer back to the Node equivalent, like the comments in `../basera`.
  Keep that style.
- Work phase by phase (see PROGRESS.md). At the end of each phase: verify it, update PROGRESS.md, then explain.
- Commit/push only when the user asks.

## Commands
- Run locally: `./mvnw spring-boot:run` (Windows: `.\mvnw spring-boot:run`). This auto-starts MongoDB from `compose.yaml`
  (Docker Desktop must be running), seeds demo data if the DB is empty and serves http://localhost:8080.
- Tests: `./mvnw test` (Docker must be running: Testcontainers starts a MongoDB for them)
- Build the jar: `./mvnw package`
- Verify the production build like Render does: `docker build -t basera-java-check .`, then run it against a Mongo
  container with `-e MONGO_URL=...`. The image sets `SPRING_PROFILES_ACTIVE=prod`. Remove the containers, network and
  image afterwards.
- Demo logins (password `password123`): host `demohost`, `priyahost`; customer `democustomer`, `rahul`.

## Gotchas found so far
- Boot 4 renamed the Mongo connection property: it's **`spring.mongodb.uri`** (not `spring.data.mongodb.uri`).
  `spring.data.mongodb.auto-index-creation` keeps its old name.
- Boot 4 uses **Jackson 3**: package `tools.jackson.databind` (e.g. `tools.jackson.databind.json.JsonMapper`).
  Annotations are still `com.fasterxml.jackson.annotation`.
- The web starter is `spring-boot-starter-webmvc`. Test starters are per module (`spring-boot-starter-webmvc-test`, …).
- **Spring Session MongoDB doesn't support Boot 4**, so sessions are in memory (lost on restart). The remember-me
  cookie logs users back in, but flash messages and saved "return to" URLs from before a restart are lost.
- Spring Security 7: if you set `http.authenticationManager(...)` yourself, providers added by configurers (like
  remember-me's) are ignored. That's why SecurityConfig puts `RememberMeAuthenticationProvider` in its ProviderManager by hand.
  Don't declare an `AuthenticationProvider` @Bean either (warning + `UserDetailsService` bean gets ignored).
- Forms that POST must use `th:action="@{...}"` so Thymeleaf adds the CSRF token; plain `action=` gets "Your session expired".
- Testing prod mode over plain HTTP: the session cookie is `Secure`, so curl won't send it back. Test flows with
  `-e SPRING_PROFILES_ACTIVE=default -e SPRING_MONGODB_URI=mongodb://<mongo>:27017/basera`.
- XML comments can't contain `--`. That broke `pom.xml` once.
- Don't add `src/test/resources/application.properties`: it *replaces* the main one instead of layering on top.
  Use `@SpringBootTest(properties = ...)` or a profile-specific file.
- The JVM default timezone is forced to UTC in `BaseraApplication.main`, so `LocalDate` is stored as midnight UTC (same as Node).
- JDK 25 (Temurin 25.0.4) is installed (2026-10-16) and the user's JAVA_HOME points to it, so `.\mvnw` works
  locally. Older JDKs (Oracle 20, 17) are still installed. A terminal opened before that date may still have the
  old JAVA_HOME; open a new one.
- The shell is Windows (PowerShell + Git Bash). In PowerShell, quote `git show "origin/main:file"`.
- Testing against a running app without touching the user's dev data: run it with `SPRING_DOCKER_COMPOSE_ENABLED=false`,
  `SPRING_MONGODB_URI=mongodb://localhost:<compose port>/basera-test`, `BASERA_SEED=reset`, another `PORT` and an
  `UPLOAD_DIR` in the scratchpad; drop `basera-test` afterwards. With `BASERA_SEED=reset` every DevTools restart re-seeds.
- curl tests: pages contain two CSRF tokens (navbar logout form + the page's form). Take one with `head -1`, or the
  server sees an invalid token ("Your session expired").
- Thymeleaf: read a record's values with `${listing.image.url()}` (records have no getters). Call beans with
  `${@fmt.price(x)}`. `th:method="put"` adds the hidden `_method` field.
- MockMvc's `flash()` only sees controller flash attributes; messages from security handlers (web/Flash) live in the
  session. `SecurityRulesTest.flashMessage` checks the session.
- @DocumentReference fields are queried with the entity (`where("owner").is(user)`); `existsOverlapping` takes ObjectIds.
- `pom.xml` overrides `tomcat.version` and `jackson-bom.version` for security fixes. Re-check on OSV.dev when upgrading Boot.
