#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

fail() {
  echo "architecture guard failed: $1" >&2
  exit 1
}

search_fixed() {
  local pattern="$1"
  shift
  if command -v rg >/dev/null 2>&1; then
    rg -n --fixed-strings "$pattern" "$@"
  else
    grep -R -n -F -- "$pattern" "$@"
  fi
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

search_regex_quiet() {
  local pattern="$1"
  shift
  if command -v rg >/dev/null 2>&1; then
    rg -n "$pattern" "$@" >/dev/null
  else
    grep -R -n -E -q -- "$pattern" "$@"
  fi
}

architecture_paths=(
  app/src/main
  app/build.gradle.kts
  firestore.rules
  firestore.indexes.json
  firebase.json
  README.md
)

for pattern in \
  "UserRole"".Customer" \
  "customer""Id" \
  "owner" \
  "isOwner" \
  "signIn""Anonymously" \
  "id""Token" \
  "TODO" \
  "FIXME" \
  "mock" \
  "Mock" \
  ".collection(\"orders\").add" \
  ".collection(\"orders\").document" \
  "Firebase""Firestore.getInstance().collection(\"orders\")"
do
  if search_fixed "$pattern" "${architecture_paths[@]}" >/tmp/pedilo_guard_match.txt; then
    cat /tmp/pedilo_guard_match.txt >&2
    fail "forbidden pattern found: $pattern"
  fi
done

for removed_path in \
  app/src/main/java/com/pedilo/app/data \
  app/src/main/java/com/pedilo/app/domain
do
  if [ -e "$removed_path" ]; then
    fail "removed legacy path exists: $removed_path"
  fi
done

for removed_symbol in \
  "Pedilo""Screen" \
  "Pedilo""ViewModel" \
  "Firebase""Pedilo""Repository" \
  "Pedilo""Repository" \
  "com.pedilo.app.""data" \
  "com.pedilo.app.""domain"
do
  if search_fixed "$removed_symbol" app/src/main tests >/tmp/pedilo_guard_match.txt; then
    cat /tmp/pedilo_guard_match.txt >&2
    fail "removed legacy symbol found"
  fi
done

if search_regex "collection[(][\"']orders[\"'][)].*[.](set|update|delete|add)|document[(].*orders.*[)].*[.](set|update|delete)" app/src/main/java/com/pedilo/app/ui >/tmp/pedilo_guard_match.txt; then
  cat /tmp/pedilo_guard_match.txt >&2
  fail "UI must not write directly to orders"
fi

pure_core_paths=(
  app/src/main/java/com/pedilo/app/core/model
  app/src/main/java/com/pedilo/app/core/port
  app/src/main/java/com/pedilo/app/core/result
  app/src/main/java/com/pedilo/app/core/usecase
)

if [ -d app/src/main/java/com/pedilo/app/core ]; then
  if search_regex "firebase|Firebase|Firestore|Functions|com[.]google[.]firebase" "${pure_core_paths[@]}" >/tmp/pedilo_guard_match.txt; then
    cat /tmp/pedilo_guard_match.txt >&2
    fail "pure core must not import Firebase"
  fi

  if search_regex "androidx[.]compose|android[.]app|android[.]content" "${pure_core_paths[@]}" app/src/main/java/com/pedilo/app/core/firebase >/tmp/pedilo_guard_match.txt; then
    cat /tmp/pedilo_guard_match.txt >&2
    fail "core must not import Android or Compose"
  fi
fi

if ! search_regex_quiet '"source"[[:space:]]*:[[:space:]]*"functions"' firebase.json; then
  fail "functions deploy config must only point to the local functions source"
fi

if [ -d functions ]; then
  if ! search_regex_quiet "exports[.]createLocalOrder" functions/index.js; then
    fail "functions must expose the createLocalOrder callable"
  fi
  if ! search_regex_quiet "exports[.]createPlusOrder" functions/index.js; then
    fail "functions must expose the createPlusOrder callable"
  fi
  if ! search_regex_quiet "exports[.]getPublicOrderTracking" functions/index.js; then
    fail "functions must expose the getPublicOrderTracking callable"
  fi
  if ! node <<'NODE' >/tmp/pedilo_guard_match.txt 2>&1
const fs = require("node:fs");
const source = fs.readFileSync("functions/index.js", "utf8");
const forbidden = /collection[(]["'](roles|payments|order_tracking)["'][)]|whatsapp|WhatsApp|driverId/;
const slices = [
  ["createLocalOrder", "exports.createLocalOrder", "exports.createPlusOrder"],
  ["createPlusOrder", "exports.createPlusOrder", "exports.getPublicOrderTracking"],
  ["getPublicOrderTracking", "exports.getPublicOrderTracking", "exports.submitPublicClaim"],
];
for (const [name, startNeedle, endNeedle] of slices) {
  const start = source.indexOf(startNeedle);
  const end = source.indexOf(endNeedle);
  if (start === -1 || end === -1 || end <= start) {
    console.error(`missing public function slice: ${name}`);
    process.exit(1);
  }
  const match = source.slice(start, end).match(forbidden);
  if (match) {
    console.error(`${name}: forbidden public function pattern ${match[0]}`);
    process.exit(1);
  }
}
NODE
  then
    cat /tmp/pedilo_guard_match.txt >&2
    fail "public order functions must not touch roles, payments, WhatsApp or tracking collections"
  fi
fi

if search_regex "isOwner|owner|customer" firestore.rules >/tmp/pedilo_guard_match.txt; then
  cat /tmp/pedilo_guard_match.txt >&2
  fail "firestore rules contain forbidden public ownership logic"
fi

if ! search_regex_quiet "allow create, update, delete: if false" firestore.rules; then
  fail "orders must reject direct client writes"
fi

echo "architecture guard passed"
