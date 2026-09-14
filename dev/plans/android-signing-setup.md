# Android Signing Setup

This document describes how APK/AAB signing is configured for Lalumo (and can be applied to other apps like Caveshuttle, Roboyard, etc.).

## Overview

Each app uses **two signing keys**:

| Key | Purpose | Who manages it |
|-----|---------|----------------|
| **Upload key** (`upload-keystore.jks`) | Sign releases for Google Play upload | Developer (you) |
| **F-Droid key** (`fdroid-release.keystore`) | Sign local F-Droid builds | Developer (you) — F-Droid's own build server uses its own key instead |

Google Play App Signing re-signs your upload with Google's app signing key (managed by Google, not accessible to you).

## Files

### 1. `keystore.properties` (gitignored, NOT committed)

Location: `android/keystore.properties` (next to `settings.gradle`)

```properties
uploadStoreFile=/home/USER/android-keystore/upload-keystore.jks
uploadStorePassword=YOUR_KEYSTORE_PASSWORD_HERE
uploadKeyAlias=key0
uploadKeyPassword=YOUR_KEY_PASSWORD_HERE
```

This file is read by `build.gradle` so Android Studio can sign without prompting for passwords.

### 2. `keystore.properties.example` (committed)

Same location, placeholder values. New developers copy this to `keystore.properties` and fill in real values.

### 3. `fdroid-release.keystore` (gitignored)

Location: `android/app/fdroid-release.keystore`

Used for local F-Droid builds (`-Pfdroid` flag). Can be shared across apps or generated per app:

```bash
keytool -genkey -v -keystore fdroid-release.keystore -alias fdroidkey -keyalg RSA -keysize 2048 -validity 10000 -storepass fdroidpass -keypass fdroidpass -dname "CN=Fdroid, OU=Dev, O=Org, L=City, ST=State, C=DE"
```

### 4. `upload-keystore.jks` (shared across all apps)

Location: `/home/ruben/android-keystore/upload-keystore.jks`

One keystore signs multiple apps. No need to generate a new one per app.

## build.gradle Configuration

### Properties loader (top of file, before `android {`)

```groovy
apply plugin: 'com.android.application'

// Load keystore properties from file (works in Android Studio and CLI)
def keystoreProperties = new Properties()
def keystorePropertiesFile = rootProject.file('keystore.properties')
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}
```

### signingConfigs (inside `android { }`)

```groovy
signingConfigs {
    fdroidRelease {
        storeFile file('fdroid-release.keystore')
        storePassword 'fdroidpass'
        keyAlias 'fdroidkey'
        keyPassword 'fdroidpass'
    }
    uploadRelease {
        // Google Play upload key (Google re-signs with the app signing key)
        storeFile file(keystoreProperties['uploadStoreFile'] ?: '/home/ruben/android-keystore/upload-keystore.jks')
        storePassword keystoreProperties['uploadStorePassword'] ?: System.getenv('UPLOAD_KEYSTORE_PASSWORD')
        keyAlias keystoreProperties['uploadKeyAlias'] ?: 'key0'
        keyPassword keystoreProperties['uploadKeyPassword'] ?: System.getenv('UPLOAD_KEY_PASSWORD')
    }
}
```

### buildTypes (inside `android { }`)

```groovy
buildTypes {
    debug {
        debuggable true
        minifyEnabled false
        shrinkResources false
        buildConfigField "boolean", "IS_DEBUG_BUILD", "true"
    }
    release {
        minifyEnabled project.hasProperty('fdroid') ? false : true
        shrinkResources project.hasProperty('fdroid') ? false : true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        buildConfigField "boolean", "IS_DEBUG_BUILD", "false"
        signingConfig project.hasProperty('fdroid') ? signingConfigs.fdroidRelease : signingConfigs.uploadRelease
    }
}
```

## .gitignore

Ensure these entries exist in `android/.gitignore`:

```
keystore.properties
*.keystore
*.jks
```

## Building

### Android Studio (GUI)

Just build → Run/Debug. `keystore.properties` is loaded automatically. No password prompt needed.

### CLI — Google Play release

```bash
UPLOAD_KEYSTORE_PASSWORD='yourpassword' UPLOAD_KEY_PASSWORD='yourpassword' ./gradlew assembleRelease
```

Or if `keystore.properties` exists:

```bash
./gradlew assembleRelease
```

### CLI — F-Droid build

```bash
./gradlew assembleRelease -Pfdroid
```

### CLI — Debug build (for verification APKs)

```bash
./gradlew clean assembleDebug
```

Debug builds are automatically signed with `~/.android/debug.keystore`.

## Applying to a new app (e.g. Roboyard)

1. Copy `keystore.properties` and `keystore.properties.example` into the app's `android/` folder
2. Copy `fdroid-release.keystore` into `android/app/` (or generate a new one)
3. Add the properties loader block at the top of `app/build.gradle`
4. Add `fdroidRelease` and `uploadRelease` (or `release`) signing configs
5. Update `buildTypes.release` with the conditional `signingConfig`
6. Ensure `.gitignore` excludes `keystore.properties` and keystore files
7. Verify with `./gradlew signingReport`
