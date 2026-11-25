#!/usr/bin/env bash
# Creates the repository labels used by labeler.yml, release notes and automation. Idempotent.
# Usage: scripts/create-labels.sh [owner/repo]   (needs an authenticated `gh`)
set -euo pipefail
REPO="${1:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"

declare -A LABELS=(
  [domain]="0E8A16"        [network]="1D76DB"      [rest]="5319E7"      [graphql]="E535AB"
  [local]="C2E0C6"         [features]="FBCA04"     [app]="0052CC"       [ci]="BFD4F2"
  [docs]="0075CA"          [dependencies]="000000" [schema-drift]="D93F0B"
  [skip-changelog]="EDEDED" [ai]="7057FF"  [run-e2e]="0E8A16"
)

for name in "${!LABELS[@]}"; do
  gh label create "$name" --color "${LABELS[$name]}" --repo "$REPO" --force
done
