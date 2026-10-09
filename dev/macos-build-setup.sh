#!/usr/bin/env bash
# Roboyard build environment setup for the virtual Mac (m1@62.210.150.197).
# macOS 26.6.1 arm64, Xcode 26.5 already installed, license already accepted.
# Everything installs user-local under ~/devtools — no sudo required.
# Run on the Mac itself:  ssh m1@62.210.150.197 'bash -s' < dev/macos-build-setup.sh
set -euo pipefail

# ----------------------------------------------------------------------
# 1) Temurin JDK 17 (project needs Java 17). macOS tarballs wrap the JDK in a
#    bundle, so JAVA_HOME points into Contents/Home.
# ----------------------------------------------------------------------
mkdir -p ~/devtools
cd ~/devtools
curl -sL -o jdk17.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse"
mkdir -p jdk-17
tar xzf jdk17.tar.gz -C jdk-17 --strip-components 1
export JAVA_HOME="$HOME/devtools/jdk-17/Contents/Home"
"$JAVA_HOME/bin/java" -version

# ----------------------------------------------------------------------
# 2) Android SDK cmdline-tools + platform-tools + platform android-36 +
#    build-tools 36.0.0 (app/composeAndroidApp use compileSdk 36).
# ----------------------------------------------------------------------
cd ~/devtools
curl -sL -o cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip"
mkdir -p Android/sdk/cmdline-tools
unzip -q cmdline-tools.zip -d Android/sdk/cmdline-tools
mv Android/sdk/cmdline-tools/cmdline-tools Android/sdk/cmdline-tools/latest || true
export ANDROID_HOME="$HOME/devtools/Android/sdk"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# ----------------------------------------------------------------------
# 3) Environment for every login shell.
# ----------------------------------------------------------------------
cat >> ~/.zshrc <<'EOF'

# --- Roboyard build environment ---
export JAVA_HOME="$HOME/devtools/jdk-17/Contents/Home"
export ANDROID_HOME="$HOME/devtools/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
EOF
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# ----------------------------------------------------------------------
# 4) iOS platform support + simulator runtime (~8.5 GB download).
#    Needed for iosSimulatorArm64 targets and running on the simulator.
# ----------------------------------------------------------------------
xcodebuild -downloadPlatform iOS

# ----------------------------------------------------------------------
# 5) Get the sources. SSH clone needs a GitHub deploy key on the Mac; the
#    https clone works for public repos, otherwise ask for the key.
# ----------------------------------------------------------------------
mkdir -p ~/repos && cd ~/repos
git clone https://github.com/rubo77/Roboyard.git || \
  git clone git@github.com:rubo77/Roboyard.git
cd Roboyard

# Android Gradle plugin finds the SDK via local.properties (sdk.dir)
echo "sdk.dir=$HOME/devtools/Android/sdk" > local.properties

# ----------------------------------------------------------------------
# 6) Build checks (first run downloads Kotlin/Native + Gradle deps, slow).
#    Android + Desktop were verified green on 2026-10-09.
# ----------------------------------------------------------------------
./gradlew --version                       # Gradle 9.4.1 via wrapper
./gradlew :app:assembleDebug              # Android APK  -> BUILD SUCCESSFUL
./gradlew :composeAndroidApp:assembleDebug # Compose APK -> BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop  # also runs generateStringsJson
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64  # iOS framework
./gradlew :shared:compileKotlinIosSimulatorArm64           # iOS klib

# ----------------------------------------------------------------------
# 5b) Optional: Android emulator + one AVD (arm64 system image, no sudo).
# ----------------------------------------------------------------------
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "emulator" "system-images;android-36;google_apis;arm64-v8a"
echo "no" | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd \
  -n roboyard -k "system-images;android-36;google_apis;arm64-v8a" --force
# start it headless later with:
# "$ANDROID_HOME/emulator/emulator" -avd roboyard -no-window &

# SOLVED (2026-10-09): the iOS targets compile. The ~595 commonMain errors
# were fixed in the sources (explicit kotlin.jvm.* imports, JsonCompat shim
# replacing Gson, TimeProvider/expect-actual for java.lang.System, full
# iosMain actuals). Board.SIZE_MAX is @HiddenFromObjC because it clashes
# with the SIZE_MAX macro from <stdint.h> in the generated framework header.

# ----------------------------------------------------------------------
# 7) XcodeGen (generates iosApp/iosApp.xcodeproj from iosApp/project.yml).
#    User-local install; the zip ships bin/ + share/xcodegen presets.
# ----------------------------------------------------------------------
cd ~/devtools
curl -sL -o xcodegen.zip \
  "https://github.com/yonaskolb/XcodeGen/releases/download/2.43.0/xcodegen.zip"
unzip -q xcodegen.zip -d xcodegen-2.43.0
mkdir -p ~/devtools/bin
ln -sf ~/devtools/xcodegen-2.43.0/xcodegen/bin/xcodegen ~/devtools/bin/xcodegen
export PATH="$HOME/devtools/bin:$PATH"
~/devtools/bin/xcodegen --version

# ----------------------------------------------------------------------
# 8) iOS framework + Xcode project + simulator build.
#    project.yml's pre-build script exports JAVA_HOME/ANDROID_HOME itself
#    (xcodebuild runs scripts without a login shell).
# ----------------------------------------------------------------------
cd ~/repos/Roboyard
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
cd iosApp
xcodegen generate
xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build

# ----------------------------------------------------------------------
# 9) Boot a simulator, install + launch the app, take a screenshot.
#    Device type/runtime names: xcrun simctl list devicetypes / runtimes
# ----------------------------------------------------------------------
xcrun simctl create roboyard-test \
  "com.apple.CoreSimulator.SimDeviceType.iPhone-17" \
  "com.apple.CoreSimulator.SimRuntime.iOS-26-5" || true
xcrun simctl boot roboyard-test || true
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator -name "iosApp.app" | head -1)
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted de.z11.roboyard
sleep 10
xcrun simctl io booted screenshot /var/tmp/roboyard-ios.png
# copy it back to the laptop:
#   scp m1@62.210.150.197:/var/tmp/roboyard-ios.png /var/tmp/devin/
# crash reports (if the app dies): ~/Library/Logs/DiagnosticReports/

# ----------------------------------------------------------------------
# Shared unit tests on the iOS simulator (70 tests):
# ----------------------------------------------------------------------
#   cd ~/repos/Roboyard && ./gradlew :shared:iosSimulatorArm64Test

# ----------------------------------------------------------------------
# Still open / may need a VNC session (sudo password required):
# - Rosetta 2: /usr/sbin/softwareupdate --install-rosetta --agree-to-license
#   (only needed if an x86-only tool surfaces during the build)
# - Xcode first-launch extras if xcodebuild complains: sudo xcodebuild -runFirstLaunch
# - GitHub SSH key for git@github.com clone/push
# - App Store Connect web work (app record, metadata, screenshots) — cannot
#   be done via SSH
#
# ----------------------------------------------------------------------
# 10) Signing + release archive (paid Apple Developer Program, team SVFZHJF78U).
#
# Certificates were created WITHOUT Xcode (openssl CSR + portal download)
# because Xcode's "Manage Certificates" dialog froze and its automatic flow
# tries to revoke the existing dev cert (GUI-only consent):
#
#   # on the Mac, once per certificate type:
#   mkdir -p ~/devtools/certs && cd ~/devtools/certs
#   openssl req -new -newkey rsa:2048 -nodes \
#     -keyout apple-dev.key -out apple-dev.csr \
#     -subj "/emailAddress=apple@eclabs.de/CN=Ruben Barkow-Kuder/C=DE"
#   openssl req -new -newkey rsa:2048 -nodes \
#     -keyout apple-dist.key -out apple-dist.csr \
#     -subj "/emailAddress=apple@eclabs.de/CN=Ruben Barkow-Kuder/C=DE"
#   # upload the .csr files at developer.apple.com/account/resources/certificates
#   # ("Apple Development" + "Apple Distribution"), download the .cer files
#   # back to dev/xcode/ on the laptop, then scp them to ~/devtools/certs/
#
# Import into the login keychain. The private keys must be exported as .p12
# first (security cannot import bare .key files); the keychain must be
# unlocked — `security unlock-keychain -p <pw>` or Keychain Access in VNC:
#
#   openssl x509 -in distribution.cer -inform DER -out distribution.pem
#   openssl pkcs12 -export -out apple-dist.p12 \
#     -inkey apple-dist.key -in distribution.pem -password pass:rb-tmp-p12
#   security unlock-keychain -p "$MAC_PASSWORD" ~/Library/Keychains/login.keychain-db
#   security import apple-dist.p12 -k ~/Library/Keychains/login.keychain-db \
#     -P rb-tmp-p12 -T /usr/bin/codesign
#   security find-identity -v -p codesigning
#   # must list BOTH "Apple Development: Ruben Barkow-Kuder (7U462K6UCH)"
#   # and  "Apple Distribution: Ruben Barkow-Kuder (SVFZHJF78U)"
#
# Provisioning profile (portal, browser): Identifiers "+" -> App ID
# de.z11.roboyard (explicit); Profiles "+" -> "App Store Connect" ->
# distribution cert -> name "Roboyard AppStore" -> download .mobileprovision.
# Install it:
#   mkdir -p ~/Library/MobileDevice/Provisioning\ Profiles
#   cp /path/to/Roboyard_AppStore.mobileprovision \
#      ~/Library/MobileDevice/Provisioning\ Profiles/
#
# project.yml pins Release to CODE_SIGN_STYLE=Manual + "Apple Distribution" +
# PROVISIONING_PROFILE_SPECIFIER="Roboyard AppStore" — regenerate after
# project.yml edits: cd iosApp && xcodegen generate
#
# Archive + export (manual signing, no -allowProvisioningUpdates needed):
#   cd ~/repos/Roboyard/iosApp
#   xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Release \
#     -sdk iphoneos -destination 'generic/platform=iOS' \
#     -archivePath build/iosApp.xcarchive archive
#   xcodebuild -exportArchive -archivePath build/iosApp.xcarchive \
#     -exportPath build/ipa -exportOptionsPlist ExportOptions.plist
#   # ExportOptions.plist has destination=upload -> uploads DIRECTLY to
#   # App Store Connect (verified 2026-10-09: "Upload succeeded").
#   # For a local .ipa instead, set destination=export in ExportOptions.plist.
#
# After upload: App Store Connect web UI (appstoreconnect.apple.com) —
#   build appears under the app's TestFlight/"Builds" once processed
#   (~minutes). App record de.z11.roboyard, metadata, screenshots,
#   review submission all need the web UI.
#
# KNOWN ISSUES:
# - -allowProvisioningUpdates fails headless — Xcode wants to revoke+recreate
#   the "Apple Development" cert "for this machine" (GUI consent). Manual
#   signing for Release avoids that path entirely.
# - codesign errSecInternalComponent during archive: (a) unlock the keychain
#   before the build and disable auto-lock with `set-keychain-settings
#   -t 36000`; (b) run `security set-key-partition-list -S
#   apple-tool:,apple:,codesign: -s -k <pw>` once so codesign may use the
#   private key non-interactively; (c) IMPORTANT: stop the Gradle daemon
#   (`./gradlew --stop`) — a daemon spawned before the keychain unlock keeps
#   the locked security context and will keep failing.
# - exportArchive fails with "Missing required icon file" / CFBundleIconName
#   unless the app has an asset-catalog icon: iosApp/iosApp/Assets.xcassets/
#   AppIcon.appiconset is generated from dev/images/IconKitchen-Output/ios/
#   (PNG set + Contents.json) plus ASSETCATALOG_COMPILER_APPICON_NAME=AppIcon
#   in project.yml.
#
#
# ----------------------------------------------------------------------
# FUTURE: the same Mac will also build the Capacitor apps
#   Lalumo      (/var/www/Musici)      and
#   CaveShuttle (/var/www/CaveShuttle)
# Both are web apps packaged with Capacitor — for those the Mac additionally
# needs Node.js/npm + @capacitor/cli and `npx cap add ios` +
# `npx cap sync ios` + xcodebuild on the generated ios/App/App.xcworkspace.
# Node can be installed user-local the same way:
#   cd ~/devtools && curl -sL -o node.tar.xz \
#     "https://nodejs.org/dist/v22.20.0/node-v22.20.0-darwin-arm64.tar.xz"
#   tar xJf node.tar.xz && ln -sf ~/devtools/node-v22.20.0-darwin-arm64 ~/devtools/node
#   export PATH="$HOME/devtools/node/bin:$PATH"
# ----------------------------------------------------------------------
