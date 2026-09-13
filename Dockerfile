# syntax=docker/dockerfile:1
#
# Story 037: a hand-written multi-stage image. Podman is the reference engine; the same
# Dockerfile also works with the Docker CLI. Building this image is never part of
# `./mvnw verify`, which only needs a JDK.

# ---- Build stage: compile and package the application jar ----
FROM eclipse-temurin:25.0.4_7-jdk-noble AS build

# unzip is required by the Maven Wrapper to extract the pinned Maven distribution; without
# it, mvnw silently falls back to the .tar.gz distribution, whose checksum does not match
# the .zip checksum pinned in .mvn/wrapper/maven-wrapper.properties.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -B -ntp package -DskipTests

# ---- Extract stage: split the layered jar into dependencies/loader/application layers ----
FROM build AS extract
WORKDIR /workspace
RUN java -Djarmode=tools -jar target/rungs-*.jar extract --layers --launcher --destination extracted

# ---- Runtime stage: a small JRE image, non-root, read-only-root capable ----
FROM eclipse-temurin:25.0.4_7-jre-noble AS runtime

# curl is the only tool present that HEALTHCHECK can call; the base image ships neither
# curl nor wget.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Dedicated non-root user and group (REQ-003); no login shell or home directory needed.
RUN groupadd --system rungs \
    && useradd --system --gid rungs --no-create-home --shell /usr/sbin/nologin rungs

WORKDIR /app

# Layer order matches Spring Boot's guidance: least-to-most likely to change, so an
# application-only change reuses the dependency and loader layers on rebuild.
COPY --from=extract --chown=rungs:rungs /workspace/extracted/dependencies/ ./
COPY --from=extract --chown=rungs:rungs /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=rungs:rungs /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=rungs:rungs /workspace/extracted/application/ ./

USER rungs
EXPOSE 8080

# Calls the readiness probe (story 017); the app is healthy once ready to serve requests.
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD curl --fail http://localhost:8080/actuator/health/readiness || exit 1

# No hard-coded heap flags (REQ-008): the JVM's container-aware defaults size the heap
# from the container's memory limit.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
