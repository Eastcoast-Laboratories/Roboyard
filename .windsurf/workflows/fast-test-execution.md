---
description: Fast test execution with immediate failure detection
---

# Fast Test Execution Guide

## Problem
When running tests, we need to know immediately if they fail without waiting for the entire test suite to complete. This guide documents how to run tests quickly with early failure detection.

## Key Principles
1. **Stop on first failure** - Do not let tests continue after the first failure
2. **Immediate feedback** - Get results within seconds, not minutes
3. **Clear output** - See the actual error immediately

## Unit Tests (Fast - 1-5 seconds)

### Run all unit tests
```bash
cd /var/www/Roboyard
./gradlew testDebugUnitTest 2>&1 | tail -20
```

### Run specific unit test class
```bash
./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest" 2>&1 | tail -20
```

### Run specific unit test method
```bash
./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest.testGameElementBasics" 2>&1 | tail -20
```

## Android Instrumented Tests (Medium - 10-30 seconds)

### Run specific E2E test with immediate failure detection
```bash
# Start test in background
adb logcat -c
./gradlew connectedAndroidTest --tests "roboyard.eclabs.ui.Level1FastE2ETest" 2>&1 &

# Monitor logs for failures (stop when you see FAILED or first error)
adb logcat | grep -E "FAILED|Error|Exception|finished" | head -1
```

### Run test and capture output immediately
```bash
# Kill any running tests first
pkill -f "gradlew connectedAndroidTest" 2>/dev/null || true

# Start fresh
adb logcat -c
./gradlew connectedAndroidTest --tests "roboyard.eclabs.ui.Level1FastE2ETest" 2>&1 &

# Wait and check logs
sleep 15
adb logcat -d | grep -E "FAILED|Error|Exception|finished" | head -5
```

## Key Commands

### Stop test immediately
```bash
pkill -f "gradlew connectedAndroidTest"
```

### View test report
```bash
# Unit tests
cat /var/www/Roboyard/app/build/reports/tests/testDebugUnitTest/index.html

# Android tests
cat /var/www/Roboyard/app/build/reports/androidTests/connected/debug/index.html
```

### Check logcat for specific test
```bash
# View logs for a specific test
adb logcat -d | grep "E2E_FAST" | head -20

# View first error
adb logcat -d | grep -E "Error|Exception|FATAL" | head -1
```

## Workflow for Debugging Failing Tests

1. **Run test** - Start the test in background
2. **Monitor immediately** - Check logs within 10-15 seconds
3. **Stop on failure** - Kill test as soon as you see a failure
4. **Analyze output** - Look at the specific error message
5. **Fix issue** - Make code changes
6. **Repeat** - Run test again

## Example: Debugging Level1FastE2ETest

```bash
# 1. Clear logs and start test
adb logcat -c
./gradlew connectedAndroidTest --tests "roboyard.eclabs.ui.Level1FastE2ETest" 2>&1 &

# 2. Wait 15 seconds and check for failures
sleep 15
adb logcat -d | grep -E "E2E_FAST|FAILED|Error" | head -20

# 3. If you see a failure, stop the test
pkill -f "gradlew connectedAndroidTest"

# 4. Read the test report
cat /var/www/Roboyard/app/build/reports/androidTests/connected/debug/roboyard.eclabs.ui.Level1FastE2ETest.html | grep -A 5 "failures"
```

## Performance Tips

- **Unit tests are fast** - Run these first (< 5 seconds)
- **Android tests are slower** - Expect 10-30 seconds per test
- **Use `--tests` filter** - Only run the specific test you're debugging
- **Check logs early** - Don't wait for the full test to finish
- **Kill hanging tests** - Use `pkill` if a test seems stuck

## Common Issues

### Test hangs or takes too long
```bash
# Kill it
pkill -f "gradlew connectedAndroidTest"

# Check if device is responsive
adb shell getprop ro.boot.serialno
```

### Logcat shows no output
```bash
# Reconnect to device
adb disconnect
adb connect localhost:5037

# Clear and restart
adb logcat -c
```

### Test report not updating
```bash
# Clean build
./gradlew clean

# Rebuild
./gradlew assembleDebugAndroidTest
```
