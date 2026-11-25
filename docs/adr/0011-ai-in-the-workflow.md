# 0011. AI assistance with deterministic gates and no merge rights

Status: accepted

## Context
AI helps with boilerplate, review and triage, but it can also write plausible wrong code, be steered by text inside a
PR (prompt injection), or "fix" CI by weakening it.

## Decision
* **Rules in the repo**: `CLAUDE.md` holds the dependency rule, exact verify commands and a "never" list (no lowering
  the coverage gate, no disabling tests, no editing generated files).
* **Deterministic gates stay the authority**: detekt, ktlint, Lint, Konsist, Kover. The AI reviewer is told to skip style.
* **Narrow tools**: read-only subagents for architecture and accessibility review; a test-writer that must show a test
  failing against broken code. A PreToolUse hook blocks edits to generated/protected paths.
* **PR workflow** (`ai-review.yml`): plain `pull_request` (no `pull_request_target`), skipped for forks, bots and drafts;
  read-only tool allow-list; `@claude` limited to owner/member/collaborator; capped turns. No signing keys or deploy
  secrets are reachable. It cannot merge or approve; branch protection and a human do that.
* **Product AI feature (Phase 12.6)**: not implemented. A key in the APK is extractable, so it would need a backend proxy,
  which is out of scope.

## Consequences
* Cost: one API key secret (`ANTHROPIC_API_KEY`) and per-PR spend, bounded by path filter and `--max-turns`.
* Noise is possible; the prompt is meant to be tuned from `docs/notes.md` measurements, which are **not collected yet**.
* The workflow has never run; it is unverified until pushed.
