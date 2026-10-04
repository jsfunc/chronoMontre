#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
WATCH_JDK="${JAVA_HOME:-$HOME/android-studio/jbr}"
WATCH_DEPENDENCIES="$HOME/.gradle/caches/modules-2/files-2.1"

locate_jar() {
    python3 - "$WATCH_DEPENDENCIES" "$1" <<'PY'
import pathlib
import sys
matches = sorted(pathlib.Path(sys.argv[1]).glob(sys.argv[2]))
if matches:
    print(matches[0])
PY
}

WATCH_JUNIT_JAR="${WATCH_JUNIT_JAR:-$(locate_jar 'junit/junit/4.13.2/*/junit-4.13.2.jar')}"
WATCH_HAMCREST_JAR="${WATCH_HAMCREST_JAR:-$(locate_jar 'org.hamcrest/hamcrest-core/1.3/*/hamcrest-core-1.3.jar')}"
if [ ! -f "$WATCH_JUNIT_JAR" ] || [ ! -f "$WATCH_HAMCREST_JAR" ]; then
    printf 'JUnit 4.13.2 and Hamcrest Core 1.3 are needed. Set WATCH_JUNIT_JAR and WATCH_HAMCREST_JAR to their jar files.\n' >&2
    exit 1
fi

mkdir -p "$PROJECT_ROOT/.local"
WATCH_TEST_BUILD="$(mktemp -d "$PROJECT_ROOT/.local/unit-tests.XXXXXX")"
"$WATCH_JDK/bin/javac" --release 17 -classpath "$WATCH_JUNIT_JAR" \
    -d "$WATCH_TEST_BUILD" \
    "$PROJECT_ROOT/app/src/main/java/fr/chronomontre/prototype/Stopwatch.java" \
    "$PROJECT_ROOT/app/src/main/java/fr/chronomontre/prototype/SessionCodec.java" \
    "$PROJECT_ROOT/app/src/test/java/fr/chronomontre/prototype/StopwatchTest.java"
"$WATCH_JDK/bin/java" \
    -classpath "$WATCH_TEST_BUILD:$WATCH_JUNIT_JAR:$WATCH_HAMCREST_JAR" \
    org.junit.runner.JUnitCore fr.chronomontre.prototype.StopwatchTest
