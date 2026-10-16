# Migration progress

Porting `../basera` (Node/Express) → this Spring Boot app. Update this file at the end of every phase.

Legend: ✅ done · 🔄 in progress · ⬜ not started

## Phases

### ✅ Phase 1 — Skeleton, config, Docker (2026-10-16)
- Generated from start.spring.io (Boot 4.1.1, Java 25): webmvc, thymeleaf, security, data-mongodb, validation, devtools, docker-compose.
- `application.properties` (port from `$PORT`, auto-index creation, `basera.seed`), `application-prod.properties`
  (`spring.mongodb.uri=${MONGO_URL}`, forwarded headers, secure cookie).
- `compose.yaml`: local MongoDB 8.0, auto-started by Spring Boot in dev.
- `Dockerfile`: multi-stage (temurin 25 JDK build → 25 JRE run), `SPRING_PROFILES_ACTIVE=prod`, `MaxRAMPercentage=75`.
- Verified: Docker build compiles; the app runs in prod mode against a Mongo container.

### ✅ Phase 2 — Models, repositories, seeder (2026-10-16)
- `model/`: User, Listing, Review, Booking, Image (record), enums Role, BookingStatus, Category (label + icon).
  Collections are named like Mongoose's (`users`, `listings`, `reviews`, `bookings`). `@DocumentReference` = `ref`.
  Virtuals became getters (`getAverageRating`, `getNights`, `isUpcoming`).
- `repository/`: one MongoRepository each. UserRepository has findByUsername / existsByUsername / existsByEmail.
- `config/MongoConfig` (`@EnableMongoAuditing` = timestamps).
- `config/SecurityConfig`: **TEMPORARY permit-all** + BCrypt `PasswordEncoder` bean.
- `seed/DataSeeder` + `resources/seed/listings.json` (exported from `../basera/seeds/listings.js`, categories turned into enum names).
  Modes: `basera.seed=if-empty` (default) | `reset` | `never`.
- `web/PagesController` + `templates/pages/home.html`: **TEMPORARY** page that lists the listings.
- Verified: seeded 4 users / 29 listings / 3 bookings; the stored documents match the Node shape (ObjectId refs,
  midnight-UTC dates, GeoJSON `[lng, lat]`); unique indexes on username and email.
- `./mvnw test` verified in phase 3. JDK 25 installed locally on 2026-10-16.

### ✅ Phase 3 — Authentication & roles (2026-10-16)
- `security/`: `CurrentUser` (UserDetails = `req.user`; id, username, role; authorities `ROLE_HOST` / `ROLE_CUSTOMER`;
  erases the hash after login), `BaseraUserDetailsService` (lookup by username), `RoleCheckingAuthenticationProvider`
  (password check, then role tab check → `WrongRoleException`), `LoginDetails` (the role tab from the form),
  `AuthRedirects` (flash + redirect for login success/failure, logout, not-logged-in, wrong role, bad CSRF token),
  `ManualLogin` (`req.login` for auto-login after signup).
- `config/SecurityConfig`: real URL rules copied from `routes/*.js` (public / host-only / customer-only / logged-in;
  **anything unlisted needs a login**), form login at `/login`, `POST /logout`, CSRF on, redirect back after login
  (Spring's saved request: GET only, URL gets a `?continue` suffix).
- Sessions: **remember-me cookie** (user's decision). `TokenBasedRememberMeServices`, always on, 7 days, signed with
  `basera.remember-me-key` = `SESSION_SECRET` (dev fallback; prod refuses to start without it). Sessions stay in memory.
- Signup: `dto/SignupForm` (Bean Validation with Joi's messages), `service/AuthService` (lowercase+trim email, 409s for
  duplicate email/username, unique-index race handled), `web/UserController`, `web/FormErrors` (joins messages like
  `middleware/validate.js`; an empty field shows only its "not allowed to be empty" message). On error the form is
  re-rendered with the message (inputs kept) instead of redirecting back.
- `exception/AppException` = ExpressError. `web/Flash` = connect-flash for code outside controllers.
  `web/GlobalModelAttributes` = `res.locals` (`currUser`, `currentPath`). `model/Role`: `fromParam("host")`, `getParam()`, `getLabel()`.
- Templates (pulled forward from phase 7): `layouts/boilerplate.html` (fragment layout), `includes/{navbar,flash,footer}.html`,
  `users/{login,signup}.html`; `static/css`, `static/js` copied from the Node app.
- **TEMPORARY**: `PagesController.placeholder` serves `/listings`, `/bookings`, `/dashboard` with the temp home page.
- Verified (Docker build + container + curl): every login/logout/role/signup message and redirect matches the Node app;
  remember-me logs the user back in after a container restart; tampered cookie rejected; cookies HttpOnly + SameSite=Lax
  (+ Secure in prod); prod without `SESSION_SECRET` exits; no generated-password warning; `./mvnw test` passes (in a JDK 25 container).

### ✅ Phase 4 — Listings (2026-10-16)
- `service/ListingService`: search (MongoTemplate + Criteria; q regex-escaped over title/location/country, category by label
  or enum name, min/max price, guests, availability via distinct booked listing ids, sort newest/price/rating-in-memory),
  featured, byOwner, getById (reviews newest first), `requireOwned` (isListingOwner), create/update (re-geocode only when
  the address changes, keep the photo unless a new file/URL is given), delete with **cascade** (reviews + bookings).
- `dto/ListingForm` (listingSchema; JSON `image.url` = form field `imageInput.url`, file field `image`), `dto/ListingFilters`
  (cleanFilters). `model/Category.fromParam`.
- `service/GeocodeService` (Nominatim, RestClient, 5 s timeouts, User-Agent; `basera.geocoding.enabled` switch for tests).
- `service/ImageStorage`: Cloudinary **signed upload via its REST API** (no SDK dependency) when `CLOUD_NAME` is set,
  else `uploads/` on disk served at `/uploads/**` (`config/WebConfig`). 5 MB limit, png/jpg/webp (other types -> 400).
- `web/ListingController`, `web/CategoryBar`, templates `listings/{index,new,edit,show}`, `includes/{listing-card,listing-form,category-bar}`.
  PUT/DELETE forms use `th:method` + `spring.mvc.hiddenmethod.filter.enabled=true`.

### ✅ Phase 5 — Reviews & bookings (2026-10-16)
- `service/BookingService`: all 5 rules, `calculatePrice`, forGuest/forHost/upcomingBookedRanges, `requireAccess`
  (canAccessBooking) / `requireGuest` (isBookingGuest), update, cancel (soft delete).
- `BookingRepository.existsOverlapping` (`@Query`, strict `$lt/$gt`, ignores the booking being edited).
- `service/ReviewService` (owner can't review, author-only delete, list kept in `listing.reviews`).
- `dto/{BookingForm,ReviewForm}`, `web/{BookingController,ReviewController}`, templates `bookings/{index,edit}`.
  `Booking.getDisplayStatus()` = the views' displayStatus ("completed").

### ✅ Phase 6 — Host dashboard + REST API (2026-10-16)
- `web/DashboardController` + `dashboard/index.html` (stats: listings, upcoming bookings, earnings = nights x price).
- `api/`: `AuthApiController` (signup/login/logout/me), `ListingApiController` (JSON or multipart), `BookingApiController`,
  `ApiJson` (response records, `_id`, no password hashes), `ApiRequests` (`{listing: ...}` / `{booking: ...}` wrappers),
  `ApiExceptionHandler` (`{ "error": ... }` for 400/401/403/404/409/415/500).
- `SecurityConfig` now has two chains: `apiSecurity` (`/api/**`, JSON 401/403 via `security/ApiAuthErrors`, **no CSRF** -
  relies on SameSite=Lax cookies + JSON-only bodies, no saved requests) and `webSecurity`. `AuthenticationManager` is a bean.
- `docs/API.md` copied from the Node app with the Java differences in section 9.

### ✅ Phase 7 — Templates & static files (2026-10-16)
- Every EJS page is ported (18 templates). Real home page (featured = 8 top-rated, category bar).
- `web/WebExceptionHandler` (= app.js errorHandler): AppException with `redirectTo` -> flash + redirect (middleware/auth.js
  deny targets); 4xx on POST/PUT/DELETE -> flash + redirect back; otherwise `pages/error.html`
  (`errorStatus`/`errorMessage`, because `error` is the flash attribute). Unknown URLs -> 404 page.
- `web/Formats` bean `fmt` (`${@fmt.price(...)}` with Indian grouping ₹1,50,000; `date`, `todayIso`, `dateInput`, `plural`, `rating`).
  `GlobalModelAttributes` also exposes `categories` and `serviceFeeRate`.

### ✅ Phase 8 — Tests, docs, dependency check (2026-10-16)
- Tests (31, all pass): `service/BookingRulesTest` (every booking rule, overlap edge cases, search availability, cascade
  delete - against a real MongoDB), `web/SecurityRulesTest` (MockMvc: URL rules, login role tab, signup, CSRF, API JSON
  errors), `web/FormatsTest`, `dto/ListingFiltersTest`, `BaseraApplicationTests`. MongoDB via **Testcontainers**
  (`TestMongo`; Docker needed for `./mvnw test`).
- `README.md` (run, config, Render deploy, structure, differences from Node).
- Dependency check (OSV.dev, all 74 runtime libraries): Boot 4.1.1's Tomcat 11.0.24 (3 critical) and Jackson 3.1.5
  (several high) had advisories -> overridden in `pom.xml` to **Tomcat 11.0.27, Jackson 3.1.7** -> 0 known vulnerabilities.
  Remove the overrides once a newer Spring Boot includes the fixes.
- Verified: whole site + API end to end with curl (scripts not kept), production Docker image (prod profile, seeds an
  empty DB, https redirects behind a proxy).
- Not done (needs the user's accounts): GitHub repo, Render service, Atlas database, Cloudinary keys.

## Decisions log
- Spring Boot + MongoDB (user's choice); new repo `basera-java`, sibling folder to `../basera`.
- Java 25 (current LTS) instead of the 21 in the original plan.
- Enums stored by name (`HOST`, `BEACH`), not the Node strings. Old Node data isn't reused (password hashes are incompatible: PBKDF2 vs BCrypt).
- Thymeleaf fragments instead of thymeleaf-layout-dialect (one less dependency).
- No Lombok: explicit getters/setters, so a Java beginner can see what's going on.
- The seeder runs on startup when the DB is empty, instead of a separate seed command.
- Logins survive restarts through a signed remember-me cookie (7 days, `SESSION_SECRET`); sessions are in memory (phase 3).
- CSRF protection is on for pages (the Node app had none). Forms must use `th:action`.
- Signup password max is 72 (BCrypt limit; Spring Security refuses longer), not Joi's 100.
- URL rules are central in SecurityConfig and default to "login required"; ownership checks go in services.
- Signup and listing form errors re-render the form with the message (inputs kept); booking/review form errors
  flash + redirect back like Node.
- Cloudinary through its REST upload API instead of the Java SDK (one request, no extra dependency).
- The JSON API has no CSRF tokens (like Node); SameSite=Lax cookies + JSON-only bodies protect it.
- API responses use explicit records (`api/ApiJson`); dates are `"2026-12-10"`; owner/author are `{_id, username}`.
- Tests use a Testcontainers MongoDB (the Node app had mongodb-memory-server).

## Waiting on the user
- [ ] Run `.\mvnw spring-boot:run` locally and click through the site (the migration is code-complete).
- [ ] Create the empty GitHub repo `dhnnypatil/basera-java` (no README), then `git init -b main`, commit and push
      (Claude can do the git part when asked).
- [ ] Render deploy (README "Deploying to Render"): Docker web service, `MONGO_URL` ending in `/basera-java`,
      `SESSION_SECRET`, `CLOUD_*`.
