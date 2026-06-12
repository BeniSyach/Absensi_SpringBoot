FROM eclipse-temurin:17-jre-alpine AS base

WORKDIR /app

# Buat user non-root untuk keamanan
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
# Download dependencies dulu (cache layer)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Runtime stage
FROM base
COPY --from=builder /build/target/absensi-app-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC -XX:G1HeapRegionSize=16m \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]