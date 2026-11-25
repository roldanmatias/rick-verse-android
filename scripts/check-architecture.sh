#!/usr/bin/env bash
# Fast architecture guard that needs no Gradle: greps imports so it can run as a 10-second CI job.
# The authoritative rules live in :architecture-tests (Konsist); this catches the worst violations early.
set -euo pipefail
cd "$(dirname "$0")/.."
fail=0

check() { # description, pattern, path-glob...
  local description="$1" pattern="$2"; shift 2
  local hits
  hits=$(grep -rEn --include='*.kt' "$pattern" "$@" 2>/dev/null | grep -v '/build/' || true)
  if [[ -n "$hits" ]]; then
    echo "FAIL: $description"; echo "$hits"; echo; fail=1
  fi
}

check "domain must stay pure Kotlin" '^import (android\.|androidx\.|retrofit2|okhttp3|com\.apollographql)' core/domain/src/main
check "presentation must not import Compose or android.*" '^import (androidx\.compose|android\.)' \
  $(find feature -type d -name presentation -path '*src/main*')
check "only :data:graphql may import Apollo" '^import com\.apollographql' \
  app/src core data/rest feature architecture-tests
check "features must not import data modules" '^import com\.roldanmatias\.rickverse\.data\.' feature

if [[ $fail -eq 0 ]]; then echo "Architecture OK"; fi
exit $fail
