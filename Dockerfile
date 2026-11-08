# Multi-stage build for the Split Bill API.
#   docker build -t split-bill-api .
#   docker run -p 4110:4110 split-bill-api
#
# Changes vs. the provided template:
#   - Builder uses the official Maven image instead of ./mvnw (no wrapper scripts to maintain).
#   - Tests run during the image build by default; a red build never produces an image.
#     Use --build-arg SKIP_TESTS=true for a faster local build.
#   - Runtime runs as a non-root user and exposes a HEALTHCHECK.

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

ARG SKIP_TESTS=false
COPY src/ src/
RUN mvn -B -q package -DskipTests=${SKIP_TESTS}

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder /app/target/split-bill-api.jar app.jar

USER app
EXPOSE 4110

ENV GITHUB_USERNAME=achmadrizkin \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:4110/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
