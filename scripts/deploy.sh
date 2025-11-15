#!/usr/bin/env bash

# 1. Set up the application directory
# 2. Download DevOps and configuration files from the remote repository
# 3. Set up required file permissions
# 4. Ensure sufficient free disk space
# 5. Pull the Docker image and start the application

set -euo pipefail

APP_DIR="${APP_DIR:?}"
REPOSITORY="${REPOSITORY:?}"
REPO_REF="${REPO_REF:-}"

set_up_app_dir() {
  mkdir -p "$APP_DIR"
  cd "$APP_DIR"
}

fetch_configs() {
  local temp_path="/tmp/config.tar.gz"
  local asset_url

  if [ -n "$REPO_REF" ]; then
    asset_url="https://github.com/${REPOSITORY}/releases/download/${REPO_REF}/config.tar.gz"
  else
    asset_url="https://github.com/${REPOSITORY}/releases/latest/download/config.tar.gz"
  fi

  curl -fsSL "$asset_url" -o "$temp_path"
  tar -xzf "$temp_path" --no-same-owner
  rm -f "$temp_path"
}

set_up_permissions() {
  find . -type f -exec chmod 644 {} \;
  find . -type d -exec chmod 755 {} \;
}

cleanup_disk() {
  local min_free_mb="${MIN_FREE_MB:-2048}"
  local free_mb=$(df -Pm / | awk 'NR==2{print $4}')
  echo "Free space on /: ${free_mb} MB (need >= ${min_free_mb} MB)"

  if [ "$free_mb" -lt "$min_free_mb" ]; then
    docker container prune -f || true
    docker image prune -f || true
    docker builder prune -af || true
    journalctl --vacuum-time=7d || true
    apt-get clean || true
  fi
}

start_application() {
  docker compose pull
  docker compose --profile monitoring up -d \
    --remove-orphans \
    --wait \
    --wait-timeout 120
  docker image prune -f || true
}

main() {
  set_up_app_dir
  fetch_configs
  set_up_permissions
  cleanup_disk
  start_application
}

main "$@"


