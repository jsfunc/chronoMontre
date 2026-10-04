#!/usr/bin/env sh
# Minimal POSIX launcher for the official Gradle wrapper jar.
set -eu
WATCH_PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then
    WATCH_JAVA="$JAVA_HOME/bin/java"
else
    WATCH_JAVA=java
fi
exec "$WATCH_JAVA" -classpath "$WATCH_PROJECT_ROOT/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"
