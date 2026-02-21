# ── Stage 1: Descarga de dependencias (cacheado mientras pom.xml no cambie) ──
FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B -q 2>/dev/null || true

# Instalar Maven en el contenedor Alpine
RUN apk add --no-cache maven

# Pre-cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# ── Stage 2: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 3: Extraer capas del JAR (Spring Boot Layered JARs) ─────────────────
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 4: Runtime minimal ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no-root para seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}", \
  "org.springframework.boot.loader.launch.JarLauncher"]
