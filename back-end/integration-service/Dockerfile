# Build
FROM maven:3.9.15-amazoncorretto-25 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -B -q -DskipTests package

# Runtime
FROM eclipse-temurin:25.0.3_9-jre-alpine-3.23

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
