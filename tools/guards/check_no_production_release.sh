#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

fail() {
  echo "no-production-release guard failed: $1" >&2
  exit 1
}

search_regex() {
  local pattern="$1"
  shift
  if command -v rg >/dev/null 2>&1; then
    rg -n "$pattern" "$@"
  else
    grep -R -n -E -- "$pattern" "$@"
  fi
}

runtime_paths=(
  app/src/main
  functions
  README.md
  firebase.json
  firestore.rules
)

if search_regex "firebase[[:space:]]+deploy|firebase[[:space:]]+hosting|firebase[[:space:]]+functions:config:set" "${runtime_paths[@]}" >/tmp/pedilo_no_prod_match.txt; then
  cat /tmp/pedilo_no_prod_match.txt >&2
  fail "deploy command found in repo runtime/test paths"
fi

if search_regex "bundleRelease|assembleRelease|signingConfig[[:space:]]+release|play[[:space:]]*console|Google Play listo|producci[oó]n lista" "${runtime_paths[@]}" >/tmp/pedilo_no_prod_match.txt; then
  cat /tmp/pedilo_no_prod_match.txt >&2
  fail "release or production-ready claim found"
fi

if search_regex "api[_-]?key[[:space:]]*[:=]|bearer[[:space:]]+[A-Za-z0-9._-]{12,}|authorization[[:space:]]*[:=]|twilio|meta graph|openai|anthropic|gemini|firebaseMessaging[.]send|-----BEGIN (PRIVATE|RSA|EC) KEY-----" app/src/main functions >/tmp/pedilo_no_prod_match.txt; then
  cat /tmp/pedilo_no_prod_match.txt >&2
  fail "secret, token or external provider marker found"
fi

if search_regex "WhatsApp enviado|push enviado|notificaci[oó]n enviada|IA externa activa|pago confirmado|pasarela activa" "${runtime_paths[@]}" >/tmp/pedilo_no_prod_match.txt; then
  cat /tmp/pedilo_no_prod_match.txt >&2
  fail "fake production capability copy found"
fi

if search_regex "seed_public_catalog|node[[:space:]]+tools/seed|firebase[[:space:]]+database:set" app/src/main functions README.md firebase.json >/tmp/pedilo_no_prod_match.txt; then
  cat /tmp/pedilo_no_prod_match.txt >&2
  fail "seed command wired into runtime or validation docs"
fi

echo "no-production-release guard passed"
