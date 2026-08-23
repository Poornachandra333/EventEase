# Lightweight runtime image using OpenJDK 21 JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create unprivileged security user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy compiled executable JAR from host target directory
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
