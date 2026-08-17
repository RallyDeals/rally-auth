FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

# Mount the secret settings.xml file securely during build time and run maven
RUN --mount=type=secret,id=mvn_settings \
    mvn -s /run/secrets/mvn_settings -q clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 10001 rally \
    && mkdir -p /app && chown -R rally:rally /app

USER rally
COPY --from=build /workspace/target/auth-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8082/actuator/health || exit 1"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]