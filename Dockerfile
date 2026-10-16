# How Render builds and runs the app. Render has no built-in Java option, so we describe it with Docker.
#
# Two stages: the first has the full JDK (compiler) and builds the jar; the second has only the Java runtime
# and runs it. The final image stays small and contains no source code or build tools.

# ---- Stage 1: build ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Download dependencies first, in their own layer. Docker caches it, so rebuilds after a code-only change
# skip the download (like copying package.json and running npm install before copying the code).
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

# ---- Stage 2: run ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/basera-*.jar app.jar

# Run with production settings (application-prod.properties).
ENV SPRING_PROFILES_ACTIVE=prod
# Let Java use up to 75% of the container's memory (Render's free tier has 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
