# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon > /dev/null
COPY src src
# -x jar skips the *-plain.jar so the runtime stage copies exactly one artifact.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -x test -x jar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system --gid 1001 app && useradd --system --uid 1001 --gid app --no-create-home app
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER 1001
EXPOSE 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
