#!/usr/bin/env bash
# Re-captures the real REST responses used as test fixtures. Review the diff before committing:
# a changed fixture means the backend changed (the nightly contract workflow also checks this).
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=data/rest/src/test/resources/fixtures
BASE=https://rickandmortyapi.com/api
mkdir -p "$OUT"

curl -fsS "$BASE/character?page=1"  > "$OUT/characters_page1.json"
curl -fsS "$BASE/character?page=42" > "$OUT/characters_page_last.json"
curl -fsS "$BASE/character/1"       > "$OUT/character_1.json"
curl -fsS "$BASE/character/2"       > "$OUT/character_2.json"
curl -fsS "$BASE/character/1,2"     > "$OUT/characters_1_2.json"
curl -fsS "$BASE/episode?page=1"    > "$OUT/episodes_page1.json"
curl -fsS "$BASE/episode/1"         > "$OUT/episode_1.json"
curl -fsS "$BASE/episode/1,2,3"     > "$OUT/episodes_1_2_3.json"
# An empty search is a 404 (the body is what matters); -f would abort, so no -f here.
curl -sS "$BASE/character?name=zzzzzz" > "$OUT/characters_empty_search_404.json"
echo "Fixtures refreshed in $OUT"
