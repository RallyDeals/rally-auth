FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

ARG PACKAGES_USERNAME
ARG PACKAGES_TOKEN

ENV PACKAGES_USERNAME=$GITHUB_ACTOR
ENV PACKAGES_TOKEN=$GITHUB_TOKEN

RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

COPY pom.xml .
COPY src src

# Run global 'mvn' instead of wrapper './mvnw' to bypass script execution issues
RUN mvn -s /root/.m2/settings.xml -q clean package -DskipTests

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