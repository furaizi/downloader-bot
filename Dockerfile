FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /src

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./

RUN chmod +x ./gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar


FROM eclipse-temurin:21-jre-noble AS runtime

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV PATH="/opt/venv/bin:$PATH"
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.home=/app/data --add-modules=jdk.management"

WORKDIR /app

# UID 65532 for backward compatibility
RUN groupadd -g 65532 botuser && \
    useradd -r -u 65532 -g botuser -m -d /app/data botuser

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    atomicparsley \
    git \
    ffmpeg \
    python3 \
    python3-pip \
    python3-venv \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /opt/venv
RUN pip install --no-cache-dir --upgrade pip setuptools wheel && \
    pip install --no-cache-dir --upgrade --pre \
      "yt-dlp[default,curl-cffi]" \
      yt-dlp-ejs && \
    pip install --no-cache-dir --force-reinstall \
        https://github.com/mikf/gallery-dl/archive/master.tar.gz \
        https://github.com/instaloader/instaloader/archive/master.tar.gz \
        requests[socks]

RUN deno --version && \
    yt-dlp --version && \
    gallery-dl --version && \
    instaloader --version && \
    ffmpeg -version

COPY --from=builder --chown=botuser:botuser /src/build/libs/*.jar app.jar

USER botuser
EXPOSE 8080
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]