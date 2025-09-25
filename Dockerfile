FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /src

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew

RUN ./gradlew --no-daemon -v || true && ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar


FROM debian:bookworm-slim AS ffmpeg
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl xz-utils \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /opt/bin

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


FROM debian:bookworm-slim AS ytdlp
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl \
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

FROM debian:bookworm-slim AS instaloader
ARG PBS_DATE=20250818
ARG PYTHON_VERSION=3.13.7
ENV PY_PREFIX=/opt/py
WORKDIR /tmp

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl xz-utils zstd \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
  arch="$(dpkg --print-architecture)"; \
  case "$arch" in \
    amd64)  triplet="x86_64-unknown-linux-gnu" ;; \
    arm64)  triplet="aarch64-unknown-linux-gnu" ;; \
    armhf)  triplet="armv7-unknown-linux-gnueabihf" ;; \
    *) echo "Unsupported arch for CPython tarball: $arch" >&2; exit 1 ;; \
  esac; \
  file="cpython-${PYTHON_VERSION}+${PBS_DATE}-${triplet}-pgo+lto-full.tar.zst"; \
  url="https://github.com/astral-sh/python-build-standalone/releases/download/${PBS_DATE}/${file}"; \
  echo "Downloading ${url}"; \
  curl -fsSL "$url" -o python.tar.zst; \
  mkdir -p /tmp/py && tar --zstd -xf python.tar.zst -C /tmp/py; \
  mkdir -p "${PY_PREFIX}"; \
  test -d /tmp/py/python/install; \
  cp -a /tmp/py/python/install/. "${PY_PREFIX}/"; \
  ln -sf python3 "${PY_PREFIX}/bin/python"; \
  "${PY_PREFIX}/bin/python3" -V

RUN set -eux; \
  "${PY_PREFIX}/bin/python3" -m ensurepip --upgrade; \
  "${PY_PREFIX}/bin/python3" -m pip install --upgrade pip; \
  "${PY_PREFIX}/bin/python3" -m pip install --no-cache-dir instaloader; \
  "${PY_PREFIX}/bin/instaloader" --version

RUN rm -rf /tmp/*

FROM debian:bookworm-slim AS busybox
RUN apt-get update && apt-get install -y --no-install-recommends \
      busybox-static ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /opt/empty


FROM gcr.io/distroless/java21-debian12 AS runtime

ENV PATH="/usr/local/bin:${PATH}"
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.home=/data"
WORKDIR /app

COPY --from=ytdlp /opt/bin/yt-dlp /usr/local/bin/yt-dlp
COPY --from=ffmpeg /opt/bin/ffmpeg /usr/local/bin/ffmpeg
COPY --from=ffmpeg /opt/bin/ffprobe /usr/local/bin/ffprobe

COPY --from=ffmpeg /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

COPY --from=busybox /bin/busybox /usr/local/bin/busybox
COPY --from=busybox --chown=65532:65532 /opt/empty/ /data/Downloads/downloader-bot/

COPY --from=instaloader /opt/py /opt/py
COPY --from=instaloader /opt/py/bin/instaloader /usr/local/bin/instaloader
ENV PATH="/opt/py/bin:${PATH}"

COPY --from=builder /src/build/libs/*.jar /app/app.jar

USER nonroot

EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
