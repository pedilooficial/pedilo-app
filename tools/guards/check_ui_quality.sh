#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

fail() {
  echo "ui quality guard failed: $1" >&2
  exit 1
}

ui_paths=(app/src/main/java/com/pedilo/app)

if find app/src/main/res -type f -path '*/drawable*/*' -name 'plan_*.png' | rg . >/tmp/pedilo_ui_guard_match.txt; then
  cat /tmp/pedilo_ui_guard_match.txt >&2
  fail "runtime plan screenshots found"
fi

for pattern in \
  "R.drawable.""plan_" \
  "Plan""Screen" \
  "Plan""Phone""Screen" \
  "Tap""Zone" \
  "ContentScale.FillHeight" \
  "ContentScale.FillBounds"
do
  if rg -n --fixed-strings "$pattern" "${ui_paths[@]}" >/tmp/pedilo_ui_guard_match.txt; then
    cat /tmp/pedilo_ui_guard_match.txt >&2
    fail "screenshot UI pattern found: $pattern"
  fi
done

if rg -n "offset[(][^)]*[.]dp[^)]*[)][[:space:]]*[.]size[(][^)]*[.]dp[^)]*[)][[:space:]]*[.]clickable|offset[(][^)]*[.]dp[^)]*[)][[:space:][:graph:]]{0,160}clickable" "${ui_paths[@]}" >/tmp/pedilo_ui_guard_match.txt; then
  cat /tmp/pedilo_ui_guard_match.txt >&2
  fail "transparent hotspot pattern found"
fi

if find app/src/main/res -type f \( -path '*/drawable*/*' -o -path '*/mipmap*/*' \) | rg '/(mockup|mockups|screenshot|screenshots|visual-certification|public-user-approved-mockups)/|/(0[0-9][._-].*)[.]png$' >/tmp/pedilo_ui_guard_match.txt; then
  cat /tmp/pedilo_ui_guard_match.txt >&2
  fail "design screenshot copied into runtime resources"
fi

if rg -n "TODO|FIXME|Pedido listo para enviar|Cliente|Customer|customer|signInAnonymously" app/src/main/java/com/pedilo/app/ui >/tmp/pedilo_ui_guard_match.txt; then
  cat /tmp/pedilo_ui_guard_match.txt >&2
  fail "legacy or unresolved UI text found"
fi

if rg -n "Color[(]0x" app/src/main/java/com/pedilo/app --glob '!app/src/main/java/com/pedilo/app/ui/theme/Color.kt' --glob '!app/src/main/java/com/pedilo/app/ui/publicuser/PublicTheme.kt' >/tmp/pedilo_ui_guard_match.txt; then
  cat /tmp/pedilo_ui_guard_match.txt >&2
  fail "hardcoded color outside theme"
fi

if rg -n "OutlinedTextField" app/src/main/java/com/pedilo/app/ui --glob '!app/src/main/java/com/pedilo/app/ui/components/PediloTextField.kt' >/tmp/pedilo_ui_guard_match.txt; then
  cat /tmp/pedilo_ui_guard_match.txt >&2
  fail "raw text field outside shared component"
fi

if [ -d app/src/main/java/com/pedilo/app/ui/components ]; then
  if find app/src/main/java/com/pedilo/app/ui/components -type f -printf '%f\n' | sort | uniq -d | rg . >/tmp/pedilo_ui_guard_match.txt; then
    cat /tmp/pedilo_ui_guard_match.txt >&2
    fail "duplicated component file names"
  fi
fi

if [ -d app/src/main/java/com/pedilo/app/ui/theme ]; then
  if find app/src/main/java/com/pedilo/app/ui/theme -type f -printf '%f\n' | sort | uniq -d | rg . >/tmp/pedilo_ui_guard_match.txt; then
    cat /tmp/pedilo_ui_guard_match.txt >&2
    fail "duplicated theme file names"
  fi
fi

echo "ui quality guard passed"
