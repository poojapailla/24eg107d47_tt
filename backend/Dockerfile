# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the pom first so dependencies can be cached separately from the source code.
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src

# Build the truststore that contains the Aiven CA certificate, so the container can
# verify the server certificate (sslMode=VERIFY_IDENTITY) without extra setup.
RUN mkdir -p /build/certs \
 && keytool -importcert -noprompt -alias aiven-ca \
      -file src/main/resources/tls/aiven-ca.pem \
      -keystore /build/certs/aiven-truststore.p12 \
      -storetype PKCS12 -storepass changeit

RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# TLS material for the verified-tls profile.
COPY --from=build /build/certs /app/certs

COPY --from=build /build/target/lab-exam-backend-*.jar /app/app.jar

# The image ships the Aiven CA truststore, so verified TLS is on by default.
# The verified-tls profile points at /app/certs/aiven-truststore.p12.
# Override SPRING_PROFILES_ACTIVE in the platform dashboard if you need a different profile.
ENV SPRING_PROFILES_ACTIVE=verified-tls

# Render injects PORT; Spring Boot reads it through server.port=${PORT:8080}.
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
