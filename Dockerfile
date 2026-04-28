# Multi-stage build for IAM Service with optimized caching
FROM eclipse-temurin:25-jdk-alpine AS builder

# Install Maven for build optimization
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy Maven configuration for dependency caching
COPY pom.xml .

# Download dependencies in separate layer for better caching
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests -B && \
    mkdir -p target/dependency && \
    cd target && \
    java -Djarmode=tools -jar app.jar extract --layers --destination dependency

# Production runtime stage with security hardening
FROM eclipse-temurin:25-jre-alpine

# Security: Install only essential packages
RUN apk add --no-cache curl tzdata && \
    rm -rf /var/cache/apk/* && \
    addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser -s /bin/false -h /app

WORKDIR /app
RUN mkdir -p /app/logs /app/tmp && \
    chown -R appuser:appuser /app

# Copy application layers for optimal caching
COPY --from=builder --chown=appuser:appuser /app/target/dependency/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/dependency/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /app/target/dependency/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/dependency/application/ ./

USER appuser

EXPOSE 8080
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health/readiness || exit 1

# JVM optimization for IAM service workloads
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true \
    -Dlogging.config=classpath:logback-spring.xml"

ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/app/tmp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_TOOL_OPTIONS org.springframework.boot.loader.launch.JarLauncher"]
