FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /src

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon -v || true && \
    ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar


FROM debian:bookworm-slim AS tools
ARG PBS_DATE=20250818
ARG PYTHON_VERSION=3.13.7
ENV PY_PREFIX=/opt/py
WORKDIR /tmp

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl xz-utils zstd busybox-static \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
  arch="$(dpkg --print-architecture)"; \
  case "$arch" in \
    amd64)  ffurl="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz" ;; \
    arm64)  ffurl="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz" ;; \
    armhf)  ffurl="https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-armhf-static.tar.xz" ;; \
    *) echo "Unsupported arch: $arch" >&2; exit 1 ;; \
  esac; \
  curl -fsSL "$ffurl" -o /tmp/ffmpeg.tar.xz; \
  mkdir -p /opt/bin /tmp/ff && tar -C /tmp/ff -xJf /tmp/ffmpeg.tar.xz --strip-components=1; \
  mv /tmp/ff/ffmpeg /tmp/ff/ffprobe /opt/bin/; \
  chmod +x /opt/bin/ffmpeg /opt/bin/ffprobe; \
  /opt/bin/ffmpeg -version >/dev/null

RUN set -eux; \
  arch="$(dpkg --print-architecture)"; \
  case "$arch" in \
    amd64)  ytdlp="yt-dlp_linux" ;; \
    arm64)  ytdlp="yt-dlp_linux_aarch64" ;; \
    armhf|armv7l|armel) ytdlp="yt-dlp_linux_armv7l" ;; \
    *) echo "Unsupported arch: $arch" >&2; exit 1 ;; \
  esac; \
  curl -fsSL "https://github.com/yt-dlp/yt-dlp/releases/latest/download/${ytdlp}" -o /opt/bin/yt-dlp; \
  chmod +x /opt/bin/yt-dlp; \
  /opt/bin/yt-dlp --version >/dev/null

RUN set -eux; \
  curl -fsSL "https://github.com/mikf/gallery-dl/releases/latest/download/gallery-dl.bin" \
    -o /opt/bin/gallery-dl; \
  chmod +x /opt/bin/gallery-dl; \
  /opt/bin/gallery-dl --version >/dev/null

RUN set -eux; \
  arch="$(dpkg --print-architecture)"; \
  case "$arch" in \
    amd64)  triplet="x86_64-unknown-linux-gnu" ;; \
    arm64)  triplet="aarch64-unknown-linux-gnu" ;; \
    armhf)  triplet="armv7-unknown-linux-gnueabihf" ;; \
    *) echo "Unsupported arch: $arch" >&2; exit 1 ;; \
  esac; \
  file="cpython-${PYTHON_VERSION}+${PBS_DATE}-${triplet}-pgo+lto-full.tar.zst"; \
  url="https://github.com/astral-sh/python-build-standalone/releases/download/${PBS_DATE}/${file}"; \
  curl -fsSL "$url" -o python.tar.zst; \
  mkdir -p /tmp/py && tar --zstd -xf python.tar.zst -C /tmp/py; \
  mkdir -p "${PY_PREFIX}"; \
  cp -a /tmp/py/python/install/. "${PY_PREFIX}/"; \
  ln -sf python3 "${PY_PREFIX}/bin/python"; \
  "${PY_PREFIX}/bin/python3" -V

RUN set -eux; \
  "${PY_PREFIX}/bin/python3" -m ensurepip --upgrade; \
  "${PY_PREFIX}/bin/python3" -m pip install --upgrade pip; \
  "${PY_PREFIX}/bin/python3" -m pip install --no-cache-dir instaloader; \
  "${PY_PREFIX}/bin/instaloader" --version >/dev/null

RUN mkdir -p /opt/empty && rm -rf /tmp/*


FROM gcr.io/distroless/java21-debian12 AS runtime
ENV PATH="/usr/local/bin:/opt/py/bin:${PATH}" \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.home=/data"
WORKDIR /app

COPY --from=tools /opt/bin/ffmpeg   /usr/local/bin/ffmpeg
COPY --from=tools /opt/bin/ffprobe  /usr/local/bin/ffprobe
COPY --from=tools /opt/bin/yt-dlp   /usr/local/bin/yt-dlp
COPY --from=tools /opt/bin/gallery-dl /usr/local/bin/gallery-dl
COPY --from=tools /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=tools /bin/busybox      /usr/local/bin/busybox

COPY --from=tools --chown=65532:65532 /opt/empty/ /data/Downloads/downloader-bot/

COPY --from=tools /opt/py /opt/py

COPY --from=builder /src/build/libs/*.jar /app/app.jar

USER nonroot
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
