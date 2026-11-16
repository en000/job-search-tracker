# syntax=docker/dockerfile:1
FROM node:22.22.0-bookworm-slim AS frontend
WORKDIR /build
COPY frontend/package*.json ./
RUN --mount=type=secret,id=build_ca,required=false \
    if [ -f /run/secrets/build_ca ]; then export NODE_EXTRA_CA_CERTS=/run/secrets/build_ca; fi; \
    npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21.0.10_7-jdk-jammy AS backend
WORKDIR /build
COPY backend/ ./
RUN chmod +x mvnw
COPY --from=frontend /build/dist/ src/main/resources/static/
RUN --mount=type=secret,id=build_ca,required=false \
    if [ -f /run/secrets/build_ca ]; then \
      cp /run/secrets/build_ca /usr/local/share/ca-certificates/build-ca.crt && update-ca-certificates \
      && keytool -importcert -cacerts -storepass changeit -noprompt -alias build-ca -file /run/secrets/build_ca || exit 1; \
    fi; \
    ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21.0.10_7-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home tracker
WORKDIR /app
COPY --from=backend /build/target/job-search-tracker.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
