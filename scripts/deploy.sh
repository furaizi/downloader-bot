#!/usr/bin/env bash

# 1. Create all necessary directories
# 2. Create a stub .env
# 3. Download DevOps and other config files from a remote repository
# 4. Setup necessary permissions for files
# 5. Ensure enough free disk space
# 6. Pull image and run application

set -euo pipefail

APP_DIR="${APP_DIR:?}"
REPOSITORY="${REPOSITORY:?}"
REPO_REF="${REPO_REF:?}"

prepare_dirs() {
  mkdir -p "$APP_DIR"
  cd "$APP_DIR"
}

create_env_stub() {
  if [ ! -f .env ]; then
    umask 077
    : > .env
  fi
}

fetch_configs() {
  local asset_url="https://github.com/${REPOSITORY}/releases/download/${REPO_REF}/config.tar.gz"
  local temp_path="/tmp/config.tar.gz"

  curl -fsSL -o "$temp_path" "$asset_url"
  tar -xzf "$temp_path" --no-same-owner
  rm -f "$temp_path"
}

setup_permissions() {
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
    sudo journalctl --vacuum-time=7d || true
    sudo apt-get clean || true
  fi
}

run_application() {
  docker compose pull
  docker compose --profile monitoring up -d \
    --remove-orphans \
    --wait \
    --wait-timeout 120
  docker image prune -f || true
}

main() {
  prepare_dirs
  create_env_stub
  fetch_configs
  setup_permissions
  cleanup_disk
  run_application
}

main "$@"


