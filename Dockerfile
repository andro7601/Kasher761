FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY game-engine/pom.xml game-engine/pom.xml
COPY web-api/pom.xml web-api/pom.xml

RUN ./mvnw -B -pl web-api -am dependency:go-offline

COPY game-engine/src game-engine/src
COPY web-api/src web-api/src

RUN ./mvnw -B -pl web-api -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /workspace/web-api/target/web-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
