## What and why
<!-- One or two sentences. Link the issue or the guide phase. -->

## Changes
-

## How to verify
- [ ] `./gradlew test detekt spotlessCheck lint koverVerify`
- [ ] Manual check (steps / screenshots / GIF for UI changes):

## Checklist
- [ ] PR title follows Conventional Commits (`feat(rest): ...`)
- [ ] Dependency rule respected (Domain pure, no Compose in `presentation`, Apollo only in `:data:graphql`)
- [ ] `CancellationException` is never swallowed
- [ ] Tests added or updated; no test disabled to get CI green
- [ ] No secrets, keystores or `local.properties` committed
