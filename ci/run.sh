#!/usr/bin/env bash
#
# GBL-013 US-4 (wave 1) — CI contract for codebase-gradle.
#
# The pipeline YAML is an adapter: it checks out, selects the runtime image
# (`cccp-ci-base`, pinned by digest) and delegates to this script. The `unit`
# job mounts the host Docker socket for testcontainers (pgvector).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/codebase-plugin"

job="${1:-}"

case "$job" in
  fast)
    ./gradlew testFast --no-daemon --console=plain
    ;;
  test)
    ./gradlew test ktlintCheck --no-daemon --console=plain
    ;;
  *)
    printf 'unsupported job for codebase-gradle: %s\n' "${job:-<none>}" >&2
    printf 'supported: fast | test\n' >&2
    exit 2
    ;;
esac
