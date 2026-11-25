---
description: Draft release notes from merged PRs since the last tag, grouped by layer
---
Use `git log $(git describe --tags --abbrev=0)..HEAD` and merged PR titles/labels. Group by layer as in
`.github/release.yml`. Plain language, user-visible effects first. Output Markdown only; I will edit and tag.
