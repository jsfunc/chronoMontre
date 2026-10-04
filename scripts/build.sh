#!/usr/bin/env bash
# Build, test, lint, and sign the watch APK, including AndroidX tile/complication libraries.
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
WATCH_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
WATCH_BUILD_TOOLS="${WATCH_BUILD_TOOLS:-36.0.0}"
WATCH_JDK="${JAVA_HOME:-$HOME/android-studio/jbr}"
WATCH_VERSION="$(sed -n 's/^versionName=//p' "$PROJECT_ROOT/app/version.properties")"
WATCH_APK="$PROJECT_ROOT/artifacts/chronomontre-$WATCH_VERSION.apk"
WATCH_TOOLS="$WATCH_SDK/build-tools/$WATCH_BUILD_TOOLS"

for executable in "$WATCH_JDK/bin/java" "$WATCH_JDK/bin/keytool" "$WATCH_TOOLS/apksigner"; do
    if [ ! -x "$executable" ]; then
        printf 'Missing executable: %s\nSet JAVA_HOME, ANDROID_SDK_ROOT, or WATCH_BUILD_TOOLS to your installation.\n' "$executable" >&2
        exit 1
    fi
done

mkdir -p "$PROJECT_ROOT/.local" "$PROJECT_ROOT/artifacts"
if [ ! -f "$PROJECT_ROOT/.local/debug.keystore" ]; then
    "$WATCH_JDK/bin/keytool" -genkeypair \
        -keystore "$PROJECT_ROOT/.local/debug.keystore" \
        -storepass android -keypass android -alias androiddebugkey \
        -dname 'CN=Android Debug,O=Android,C=US' \
        -keyalg RSA -keysize 2048 -validity 10000
    chmod 600 "$PROJECT_ROOT/.local/debug.keystore"
fi

# Gradle downloads the AndroidX dependencies on the first build.
JAVA_HOME="$WATCH_JDK" ANDROID_HOME="$WATCH_SDK" ANDROID_SDK_ROOT="$WATCH_SDK" \
    "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon \
    :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease "$@"

WATCH_RELEASE="$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"
JAVA_HOME="$WATCH_JDK" "$WATCH_TOOLS/apksigner" verify "$WATCH_RELEASE"
cp "$WATCH_RELEASE" "$WATCH_APK"
(cd "$PROJECT_ROOT/artifacts" && sha256sum "chronomontre-$WATCH_VERSION.apk" > "chronomontre-$WATCH_VERSION.apk.sha256")
printf 'Built, tested, linted, and signature verified: %s\n' "$WATCH_APK"
