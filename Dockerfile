# ─────────────────────────────────────────────
# Stage 1: Build the JAR with Maven
# ─────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

# Copy pom.xml first to cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Install Maven
RUN apk add --no-cache maven

# Re-download dependencies properly
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ─────────────────────────────────────────────
# Stage 2: Lightweight runtime image
# ─────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create unprivileged security user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from Stage 1
COPY --from=builder /build/target/*.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
