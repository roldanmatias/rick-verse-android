# GitHub setup checklist (cannot be done from code)

Workflows are committed and YAML-validated, but **none has run yet**: they need the repository pushed first.
Third-party actions are pinned to full commit SHAs (resolved from the GitHub API); Dependabot keeps them current.

## Once, in the repository settings
- [ ] Settings -> Actions -> General: workflow permissions = **read-only**; allow Actions to create PRs = off
- [ ] Settings -> Pages: source = **GitHub Actions** (needed by `docs.yml`)
- [ ] Enable Issues, Dependabot alerts and security updates, and code scanning (CodeQL default setup off: `codeql.yml` is used)
- [ ] `scripts/create-labels.sh` (domain, network, rest, graphql, local, features, app, ci, docs, dependencies,
      schema-drift, skip-changelog, ai, run-e2e)
- [ ] Environment **`release`** (required reviewer) with secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
      `KEY_PASSWORD`. Create a keystore with `keytool`, then `base64 -i release.jks | pbcopy`. Never commit it.
- [ ] Optional: environment `play-internal` + `PLAY_SERVICE_ACCOUNT_JSON`; the upload step in `publish.yml` is
      deliberately a failing placeholder until you choose and SHA-pin a Play upload action

## Branch ruleset on `main`
Restrict deletions, block force pushes, require linear history, require a PR, require status checks:
`build-and-test`, `lint`, `architecture`, `PR title`.
**Do not require** `codegen and mapping tests` (`graphql.yml`) or `e2e` (`instrumented.yml`): the first is path-filtered and
the second is label-gated, so they do not always report and a required check that never reports blocks the PR forever.

## Workflow map
| Workflow | When | Notes |
|---|---|---|
| ci | PR, main | wrapper validation, build, tests, Kover gate, Lint, dependency review/submission |
| lint | PR, main | detekt (SARIF to code scanning), Spotless, Lint, `check-architecture.sh`, Konsist |
| instrumented | label `run-e2e`, main, nightly | emulator (KVM), AVD cache, one retry; nightly API 26/33/36 |
| pr-hygiene | PR | semantic title + module labels; `pull_request_target`, never checks out PR code |
| graphql | PR touching GraphQL | codegen + mapping tests (not a required check) |
| contract | nightly | live contract tests + schema drift -> one `schema-drift` issue |
| codeql | PR, main, weekly | `java-kotlin` (manual build) and `actions` |
| docs | main | Dokka aggregated -> Pages via OIDC |
| release | tag `v*.*.*` | tests, signed APK+AAB from environment secrets, release notes by label |
| benchmark | manual | request counts to the job summary |

## Known caveats (unverified until they run)
- CodeQL's Kotlin support can lag the Kotlin compiler (2.4.x here): if `java-kotlin` fails to extract, that is a CodeQL
  support issue, not a code issue.
- CI uses JDK 25 to run Gradle (see `gradle-daemon-jvm.properties`); bytecode targets 17.
- Dependabot coupling: Kotlin, KSP, Hilt and the Compose compiler move together (grouped in `dependabot.yml`).
- `release.yml` needs the keystore secrets; without them it fails fast with a clear message.

## Proof still to produce
- [ ] Throwaway PR adding `import androidx.compose.ui.Modifier` to a ViewModel: screenshot the red annotation, close it
- [ ] First green nightly `contract.yml` run; first Pages deploy; `v0.1.0` tag and release
- [ ] README badges (Phase 13)

## AI review (Phase 12)
- [ ] Secret `ANTHROPIC_API_KEY` (repository secret; not available to fork PRs by design)
- [ ] Install the Claude GitHub app on the repo; confirm the pinned `claude-code-action` still supports the inputs used
- [ ] Do not make `ai-review` a required check (it is advisory)
