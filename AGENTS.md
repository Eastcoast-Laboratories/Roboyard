# Roboyard — Agent Rules

## Project Shape

- Kotlin Multiplatform project with modules `app` (Android), `shared`, and `composeApp` (Desktop/iOS).
- Goal: the app must run on Android, iOS, and Desktop. Keep changes platform-independent or resolve platform differences with `expect`/`actual`.
- The solver code under `shared/src/*/kotlin/driftingdroids/` is ported from the DriftingDroids project (local repo: `/var/www/DriftingDroids`). Do not delete files there; coordinate solver changes with the upstream source.

## Testing

- Before finishing a task, build the project and run the smoke tests:

  ```bash
  ./gradlew testDebugUnitTest --tests "roboyard.eclabs.RoboyardSmokeTest"
  ```

  (`app/src/test/java/roboyard/eclabs/RoboyardSmokeTest.java` — covers GameElement, GameMove, LevelCompletionData, GridElement, MapObjects, Achievement*, Constants, save-data parsing, streak/sync logic, timestamp parsing, URL construction.)

- After EVERY change that affects `composeApp/` or `shared/`, verify the ComposeApp still compiles and runs:

  ```bash
  ./gradlew :composeApp:compileKotlinDesktop
  ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.ComposeAppUiSmokeTest"
  ```

  The `ComposeAppUiSmokeTest` launches the app and clicks through main screens (MainMenu, Level Selection, Credits) to verify navigation works. If possible, also launch the desktop app briefly (`./gradlew :composeApp:run`) to confirm it starts without crashes.
- Verify fixes yourself using unit tests and logcat. Do not write testing instructions for the user — run the tests, check the logs, and confirm the fix works end-to-end.
- Document every new test (unit, Espresso, instrumented) in `dev/TESTSUITE.md` — the central source of truth for test status. Format: test name, type (Unit / E2E / Instrumented), status (✅ Passing / ❌ Failing / ⏳ Pending), description, tags.
- For Espresso UI tests, use the `espresso-testing` skill: always reuse and extend `TestHelper` (`app/src/androidTest/java/roboyard/eclabs/ui/TestHelper.java`) instead of writing inline interactions.

## Documentation Sync

- `dev/achievements.md` is the source of truth for achievement definitions — keep it synchronized with `shared/src/commonMain/kotlin/roboyard/logic/achievements/AchievementDefinitions.kt` when changing the achievement system.
- `dev/TESTSUITE.md` is the source of truth for the test suite status.

## Deployment

- The production community site is deployed via rsync using `/var/www/roboyard.z11/deploy-watch.sh` to `eclabs-vm06:/var/kunden/webs/z11/roboyard.z11.de` — it is **not** managed by git on the server.
- The same community site also serves `caveshuttle.z11.de` via different deploy paths — make sure you touch the right files for the right app.
- Workflow: test locally → deploy → test online → only then commit.
- Never change anything on production without an explicit user command: no git operations on the server, no DELETEs, no table drops, no data manipulation. Always ask first.
- See the `deploy` skill for the detailed recipe.

## Git

- Never run `git add` or `git commit` on your own — the user keeps full control over staging and committing. Only propose commit messages.
- After EVERY code change, before sending your response, check `git status` and suggest a commit message if there are uncommitted changes. Do not wait to be asked. Do not wait for "end of session" — there is no clear session end.
- Before EVERY commit message proposal, you MUST do the following steps in order:
  1. Run `git status --short` and show the output in your response
  2. Run `git diff --stat` and show the output in your response
  3. Read the full diff for every file listed (use `git diff <file>` or the read tool) to understand ALL uncommitted changes — not just the ones from the current turn
  4. Only then propose the commit message, covering ALL changes shown in the diff — not just the latest incremental edits
- The user commits between turns. Never assume a previous change was committed — always check `git status` first. If a file shows as modified, ALL its uncommitted changes must be reflected in the commit message, even if some were made in earlier turns.
- Never include changes from earlier commits in the commit message.
- In final commit messages, do not add lines like "update TESTSUITE.md with new test entry" or "all smoke tests passing (67 tests)" — they are redundant.
