FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /src

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon -v || true && ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar


FROM debian:bookworm-slim AS tools
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl xz-utils \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /opt/bin

RUN set -eux; \
  arch="$(dpkg --print-architecture)"; \
  case "$arch" in \
    amd64) ytdlp_url="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux" ;; \
    arm64) ytdlp_url="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64" ;; \
    armhf|armv7l|armel) ytdlp_url="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_armv7l" ;; \
    *) echo "Unsupported arch: $arch" >&2; exit 1 ;; \
  esac; \
  curl -fsSL "$ytdlp_url" -o yt-dlp && chmod +x yt-dlp && ./yt-dlp --version

RUN set -eux; \
  arch="$(dpkg --print-architecture)"; \
  case "$arch" in \
    amd64) ffurl="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz" ;; \
    arm64) ffurl="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz" ;; \
    armhf) ffurl="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-armhf-static.tar.xz" ;; \
    *) echo "Unsupported arch: $arch" >&2; exit 1 ;; \
  esac; \
  curl -fsSL "$ffurl" -o /tmp/ffmpeg.tar.xz; \
  mkdir -p /tmp/ff && tar -C /tmp/ff -xJf /tmp/ffmpeg.tar.xz --strip-components=1; \
  mv /tmp/ff/ffmpeg /tmp/ff/ffprobe /opt/bin/; \
  chmod +x /opt/bin/ffmpeg /opt/bin/ffprobe; \
  /opt/bin/ffmpeg -version

RUN mkdir -p /opt/empty


FROM busybox:1.36 AS bb


FROM gcr.io/distroless/java21-debian12 AS runtime

ENV PATH="/usr/local/bin:${PATH}"
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.home=/data"
WORKDIR /app

COPY --from=tools /opt/bin/yt-dlp /usr/local/bin/yt-dlp
COPY --from=tools /opt/bin/ffmpeg /usr/local/bin/ffmpeg
COPY --from=tools /opt/bin/ffprobe /usr/local/bin/ffprobe
COPY --from=tools /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

COPY --from=bb /bin/busybox /usr/local/bin/busybox

COPY --from=tools --chown=65532:65532 /opt/empty/ /data/Downloads/downloader-bot/

COPY --from=builder /src/build/libs/*.jar /app/app.jar

USER nonroot

EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
