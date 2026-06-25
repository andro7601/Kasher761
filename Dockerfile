# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY game-engine/pom.xml game-engine/
COPY web-api/pom.xml     web-api/

RUN mvn dependency:go-offline -pl web-api -am -q

COPY game-engine/src game-engine/src
COPY web-api/src     web-api/src

RUN mvn clean package -pl web-api -am -DskipTests -q

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/web-api/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
