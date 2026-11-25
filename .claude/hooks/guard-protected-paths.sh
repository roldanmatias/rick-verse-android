#!/usr/bin/env bash
# PreToolUse hook: block edits to generated or protected files. Exit code 2 = block, stderr is shown to the model.
f=$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))' 2>/dev/null)
case "$f" in
  */build/*|*/data/local/schemas/*|*/gradle/wrapper/gradle-wrapper.jar|*/local.properties|*.jks|*.keystore)
    echo "Blocked: $f is generated or protected (see CLAUDE.md 'Never')." >&2
    exit 2 ;;
esac
exit 0
