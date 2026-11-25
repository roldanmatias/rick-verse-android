---
description: Refresh recorded API fixtures and check nothing broke
---
Run `scripts/refresh-fixtures.sh`, show `git diff --stat` for the fixtures, then run
`./gradlew test` and `./gradlew :contract-tests:contractTest`. Summarise any API change (new/removed/renamed fields)
and which mappers or models are affected. Do not edit tests to make them pass without telling me why.
