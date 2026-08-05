# ─────────────────────────────────────────────────────────────
# Nexus — Multi-stage Docker build
# Stage 1: Build the fat JAR with Maven
# Stage 2: Slim JRE runtime image
# ─────────────────────────────────────────────────────────────

# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and POM files first (better layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY nexus-app/pom.xml nexus-app/pom.xml
COPY nexus-notifications/pom.xml nexus-notifications/pom.xml

# Download dependencies (cached unless POMs change)
RUN chmod +x mvnw && ./mvnw dependency:go-offline --no-transfer-progress -B

# Copy source code
COPY nexus-app/src nexus-app/src
COPY nexus-notifications/src nexus-notifications/src

# Build the fat JAR (skip tests — CI runs them separately)
RUN ./mvnw package -DskipTests --no-transfer-progress -B

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre

# Security: run as non-root
RUN groupadd --system nexus && useradd --system --gid nexus nexus

WORKDIR /app

# Copy only the built JAR from the build stage
COPY --from=build /app/nexus-app/target/nexus-app-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER nexus

# Expose the default Spring Boot port
EXPOSE 8080

# JVM tuning for containers (respect cgroup memory limits)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
