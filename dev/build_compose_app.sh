#!/usr/bin/env bash
#
# Build Roboyard Compose Multiplatform artifacts and publish them to the
# Downloads folder.
#
# Usage:
#   bash dev/build_compose_app.sh                 # build all three platforms
#   bash dev/build_compose_app.sh android         # only the Compose Android APK
#   bash dev/build_compose_app.sh desktop         # only the Desktop uber-jar
#   bash dev/build_compose_app.sh ios             # only the iOS frameworks
#   bash dev/build_compose_app.sh desktop android # several platforms at once
#
# Output layout (DOWNLOAD_DIR defaults to ./download):
#   <DOWNLOAD_DIR>/Roboyard_android/Roboyard_v<version>_android.apk
#   <DOWNLOAD_DIR>/Roboyard_desktop/Roboyard_v<version>_desktop.jar
#   <DOWNLOAD_DIR>/Roboyard_ios/Roboyard_v<version>_ios_<arch>.framework.zip
#
# Notes:
#   - The version is read from composeAndroidApp/build.gradle (versionName).
#   - iOS frameworks can only be linked on macOS (Kotlin/Native + Xcode).
#     On other systems the iOS step is skipped with a warning.
#   - The Android release build is signed with the debug key for now,
#     matching the app module's current release configuration.
#
# Environment:
#   DOWNLOAD_DIR   override the output base directory

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# --- version (single source of truth: composeAndroidApp versionName) ---------
VERSION="$(sed -n "s/.*versionName *'\([^']*\)'.*/\1/p" composeAndroidApp/build.gradle | head -1)"
if [ -z "$VERSION" ]; then
    echo "ERROR: could not read versionName from composeAndroidApp/build.gradle" >&2
    exit 1
fi

OUT_BASE="${DOWNLOAD_DIR:-./download}"

usage() {
    sed -n '2,26p' "${BASH_SOURCE[0]}" | sed 's/^#\s\?//'
}

# --- platform selection ------------------------------------------------------
WANT_ANDROID=0
WANT_DESKTOP=0
WANT_IOS=0

if [ $# -eq 0 ]; then
    WANT_ANDROID=1; WANT_DESKTOP=1; WANT_IOS=1
else
    for arg in "$@"; do
        case "$arg" in
            all)             WANT_ANDROID=1; WANT_DESKTOP=1; WANT_IOS=1 ;;
            android|apk)     WANT_ANDROID=1 ;;
            desktop|jar)     WANT_DESKTOP=1 ;;
            ios)             WANT_IOS=1 ;;
            -h|--help|help)  usage; exit 0 ;;
            *) echo "ERROR: unknown platform '$arg'" >&2; usage; exit 2 ;;
        esac
    done
fi

FAILED=()
SKIPPED=()
BUILT=()

# --- Android APK --------------------------------------------------------------
build_android() {
    local out_dir="$OUT_BASE/Roboyard_android"
    mkdir -p "$out_dir"
    echo "=== Android: ./gradlew :composeAndroidApp:assembleRelease ==="
    if ! ./gradlew :composeAndroidApp:assembleRelease; then
        FAILED+=("android"); return
    fi
    local apk
    apk="$(find composeAndroidApp/build/outputs/apk/release -maxdepth 1 -name '*.apk' | head -1)"
    if [ -z "$apk" ]; then
        echo "ERROR: release APK not found under composeAndroidApp/build/outputs/apk/release" >&2
        FAILED+=("android"); return
    fi
    local target="$out_dir/Roboyard_v${VERSION}_android.apk"
    cp -f "$apk" "$target"
    echo "OK: $target"
    BUILT+=("$target")
}

# --- Desktop uber-jar ---------------------------------------------------------
build_desktop() {
    local out_dir="$OUT_BASE/Roboyard_desktop"
    mkdir -p "$out_dir"
    echo "=== Desktop: ./gradlew :composeApp:packageReleaseUberJarForCurrentOS ==="
    if ! ./gradlew :composeApp:packageReleaseUberJarForCurrentOS; then
        FAILED+=("desktop"); return
    fi
    local jar
    jar="$(find composeApp/build/compose/jars -name '*.jar' 2>/dev/null | head -1)"
    if [ -z "$jar" ]; then
        echo "ERROR: uber-jar not found under composeApp/build/compose/jars" >&2
        FAILED+=("desktop"); return
    fi
    local target="$out_dir/Roboyard_v${VERSION}_desktop.jar"
    cp -f "$jar" "$target"
    echo "OK: $target"
    BUILT+=("$target")
}

# --- iOS frameworks (macOS only) ----------------------------------------------
build_ios() {
    if [ "$(uname -s)" != "Darwin" ]; then
        echo "SKIP: iOS frameworks can only be built on macOS (Kotlin/Native + Xcode required)." >&2
        SKIPPED+=("ios"); return
    fi
    local out_dir="$OUT_BASE/Roboyard_ios"
    mkdir -p "$out_dir"
    echo "=== iOS: link release frameworks (arm64 device + simulator) ==="
    if ! ./gradlew :composeApp:linkReleaseFrameworkIosArm64 \
                  :composeApp:linkReleaseFrameworkIosSimulatorArm64; then
        FAILED+=("ios"); return
    fi
    local spec arch framework target
    for spec in "iosArm64:device" "iosSimulatorArm64:simulator"; do
        arch="${spec%%:*}"
        local label="${spec##*:}"
        framework="composeApp/build/bin/${arch}/releaseFramework/Roboyard.framework"
        if [ -d "$framework" ]; then
            target="$out_dir/Roboyard_v${VERSION}_ios_${label}.framework.zip"
            (cd "$(dirname "$framework")" && zip -qr "$target" "$(basename "$framework")")
            echo "OK: $target"
            BUILT+=("$target")
        else
            echo "WARN: framework not found: $framework" >&2
        fi
    done
    if [ ${#BUILT[@]} -eq 0 ]; then FAILED+=("ios"); fi
}

# --- run ----------------------------------------------------------------------
echo "Roboyard Compose build — version ${VERSION}"
echo "Output base: $OUT_BASE"

[ "$WANT_ANDROID" -eq 1 ] && build_android
[ "$WANT_DESKTOP" -eq 1 ] && build_desktop
[ "$WANT_IOS" -eq 1 ]     && build_ios

echo
echo "=== Summary ==="
for f in "${BUILT[@]:-}";  do [ -n "$f" ] && echo "built:   $f"; done
for p in "${SKIPPED[@]:-}"; do [ -n "$p" ] && echo "skipped: $p"; done
for p in "${FAILED[@]:-}";  do [ -n "$p" ] && echo "FAILED:  $p"; done

if [ ${#FAILED[@]} -gt 0 ]; then
    exit 1
fi
# iOS requested explicitly but skipped → non-zero so scripts notice
if [ "$WANT_IOS" -eq 1 ] && [ "$WANT_ANDROID" -eq 0 ] && [ "$WANT_DESKTOP" -eq 0 ] && [ ${#SKIPPED[@]} -gt 0 ]; then
    exit 1
fi
exit 0
