FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts jooq-config.xml ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew installDist --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/build/install/imdbselector-backend ./
EXPOSE 8080
ENTRYPOINT ["./bin/imdbselector-backend"]
