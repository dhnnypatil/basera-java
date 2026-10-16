# Basera (Java) 🧭

> **Basera** (बसेरा) - *a home, a place to settle for a while.*

An Airbnb-style stay booking platform built with **Java 25, Spring Boot 4, MongoDB and Thymeleaf**.
It's a port of the original Node.js / Express / EJS version of Basera, with the same pages, rules and REST API.

People sign up as one of two roles:

| Role | Can do |
|---|---|
| **Customer** | Search & filter stays, book them, change dates, cancel, write reviews |
| **Host (Admin)** | List places, edit & delete them, see every booking on their places, cancel bookings, see earnings |

Everything is available both as a **website** and as a **JSON REST API** (see [docs/API.md](docs/API.md)).

---

## Quick start

You need **JDK 25** (`winget install EclipseAdoptium.Temurin.25.JDK`) and **Docker Desktop** (running).
Maven is not needed - the project brings its own (`mvnw`).

```bash
./mvnw spring-boot:run          # Windows: .\mvnw spring-boot:run
```

Open **http://localhost:8080**.

Spring Boot starts the MongoDB container from `compose.yaml` by itself and fills an empty database with demo data.
Unlike the Node version's in-memory database, the data survives restarts. `docker compose down` wipes it,
and the next start seeds it again.

| Demo account | Username | Password |
|---|---|---|
| Customer | `democustomer` (or `rahul`) | `password123` |
| Host | `demohost` (or `priyahost`) | `password123` |

Other commands:

| Command | Does | Node equivalent |
|---|---|---|
| `./mvnw spring-boot:run` | start the app; restarts itself when code changes | `npm run dev` |
| `./mvnw test` | run the tests (needs Docker: they start a throw-away MongoDB) | `npm test` |
| `./mvnw package` | build `target/basera-0.0.1-SNAPSHOT.jar` | - |
| `java -jar target/basera-0.0.1-SNAPSHOT.jar` | run the built jar | `npm start` |

---

## Configuration

Set these as environment variables (same names as the Node app). All are optional locally.

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8080` | Port to listen on |
| `MONGO_URL` | Docker MongoDB from `compose.yaml` | MongoDB connection string. **Required in production.** |
| `SESSION_SECRET` | an insecure development value | Signs the "remember me" login cookie. **Required in production.** |
| `CLOUD_NAME`, `CLOUD_API_KEY`, `CLOUD_API_SECRET` | empty | Store uploaded photos on Cloudinary. Empty = save them in the `uploads/` folder. |
| `BASERA_SEED` | `if-empty` | Demo data: `if-empty`, `reset` (wipe and re-seed on every start - the old `npm run seed`) or `never` |
| `SPRING_PROFILES_ACTIVE` | - | `prod` turns on the production settings (`src/main/resources/application-prod.properties`). The Docker image sets it. |

Any setting in `application.properties` can be overridden the same way, e.g. `SERVER_PORT=9090`.

---

## Deploying to Render (Docker + MongoDB Atlas + Cloudinary)

Render has no built-in Java runtime, so the app is deployed as a Docker image (`Dockerfile`).

1. **MongoDB Atlas** - use a **separate database** for the Java version, so the live Node site keeps its data:
   the same cluster is fine, just end the connection string with `/basera-java` instead of `/basera`.
   (Passwords from the Node app can't be reused anyway: it hashed them with PBKDF2, this version uses BCrypt.)
2. **Cloudinary** - copy your cloud name, API key and API secret. *Needed in production:* Render's disk is
   wiped on every restart, so photos saved to `uploads/` would disappear.
3. **Render** - New → Web Service → connect the `basera-java` GitHub repo → **Language: Docker**.
   No build or start command is needed; the `Dockerfile` has both. Environment variables:

   | Variable | Value |
   |---|---|
   | `MONGO_URL` | your Atlas connection string ending in `/basera-java` |
   | `SESSION_SECRET` | a long random string, e.g. the output of `openssl rand -hex 32` |
   | `CLOUD_NAME`, `CLOUD_API_KEY`, `CLOUD_API_SECRET` | from Cloudinary |

4. Deploy. On the first start the app sees an empty database and adds the demo data itself - there's no
   separate seed step.

Without `MONGO_URL` or `SESSION_SECRET` the app refuses to start in production, with a clear message in the
Render logs. Cookies are HTTPS-only in production.

> Render's free tier sleeps after 15 minutes without traffic; the first visit afterwards takes ~30-60 seconds
> (Java starts a little slower than Node). Logged-in users stay logged in thanks to the remember-me cookie.

To check the production build on your own machine first:

```bash
docker build -t basera-java-check .
docker network create bj-net
docker run -d --name bj-mongo --network bj-net mongo:8.0
docker run --rm --network bj-net -p 8081:8080 -e MONGO_URL=mongodb://bj-mongo:27017/basera -e SESSION_SECRET=test basera-java-check
# open http://localhost:8081, then clean up:
docker rm -f bj-mongo && docker network rm bj-net && docker rmi basera-java-check
```

---

## Project structure (and the Node file each part replaces)

```
src/main/java/com/basera/
├── BaseraApplication.java      entry point                         server.js
├── config/
│   ├── SecurityConfig.java     login, logout, who may open what     config/passport.js + middleware/auth.js
│   ├── MongoConfig.java        createdAt/updatedAt                  { timestamps: true }
│   └── WebConfig.java          serve /uploads                       express.static
├── model/                      the documents in MongoDB             models/*.js
├── repository/                 database access                      Mongoose model methods
├── dto/                        form/JSON input + validation rules   validators/schemas.js (Joi)
├── service/                    business rules                       services/*.js, utils/geocode.js, config/upload.js
├── security/                   current user, login handlers         passport + parts of controllers/users.js
├── web/                        website controllers                  routes/*.js + controllers/*.js
├── api/                        JSON REST API controllers            routes/api/*.js + controllers/api/*.js
├── exception/AppException.java an error with a status code          utils/ExpressError.js
└── seed/DataSeeder.java        demo data                            seeds/
src/main/resources/
├── templates/                  Thymeleaf pages                      views/ (EJS)
├── static/                     CSS, JS                              public/
├── application.properties      settings                             .env + app.js
└── seed/listings.json          sample listings                      seeds/listings.js
src/test/java/com/basera/       tests (JUnit + a Testcontainers MongoDB)
```

## Differences from the Node version

- **Logins survive restarts** through a signed "remember me" cookie (7 days) instead of sessions stored in MongoDB.
- **CSRF protection** on every website form (the Node app had none). The API doesn't use CSRF tokens; it relies on
  `SameSite=Lax` cookies and JSON-only bodies.
- Forms with errors (signup, listing) are **shown again with what you typed**, instead of redirecting back empty.
- Uploading a file that isn't PNG/JPG/WebP shows an error (multer silently ignored it).
- Passwords are limited to **72 characters** (BCrypt's limit) instead of 100.
- API dates are plain dates (`"2026-12-10"`), and `owner`/`author` come back as `{ _id, username }`.
- Unknown URLs need a login first (everything not explicitly public is protected by default).
