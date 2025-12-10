# ------------ Stage 1: Build ------------
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# ------------ Stage 2: Run ------------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/wooriBank-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

