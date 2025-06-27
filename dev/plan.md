# Roboyard Accessibility & Solver Target Bugfix Plan

## Notes
- Multi-color targets were incorrectly mapped to red due to missing handling in GameState.getGridElements(); this is now fixed.
- Logging for target conversion in GameStateManager and SolverDD was marked as possibly unreachable, confirming alternate code paths.
- User reported accessibility mode issues: missing coordinates in target announcements, unwanted solution computation message, and hint announcements should only include the last step, not history.
- Confirmed `target_a11y` string exists and is used for target announcements; bug may be in usage (possible double-appending or formatting error).
- Located code for "KI berechnet Lösung ..." announcement and hint accessibility announcement logic in ModernGameFragment.java for targeted fixes.
- User requested: Accessibility hint message should use translated direction words from strings.xml instead of arrows.
- New feature request: Add a second pre-hint that lists the involved robot colors in the solution (before the first-move pre-hint).
- Fixed robot color list formatting in pre-hint: no comma before "and"; update propagated to all languages.
- In all strings.xml translations, always escape apostrophes (') with a backslash (\').
- For all translations except English, the string for `pre_hint_involved_robots` must match the German version: "Bewege die Roboter".
- This standardization has been implemented in Spanish, French, Chinese, and Korean as well, using the equivalent phrase in each language.
- New QA requirement: Ensure all English strings exist in every other language's strings.xml.
- Translation check revealed large numbers of missing strings in non-English translations, especially French and Korean. Next step: systematically fill in missing translations.
- Create a dev/translate.md file to log missing translations and store the check script.
- dev/translate.md and dev/check_translations.sh have been created to log and analyze missing translations; reports are now generated in dev/translation_analysis/.
- The translation analysis script was simplified to avoid errors; it now only generates basic per-language missing string reports and summary tables.
- After further errors, a super simple script (simple_check.sh) was created and executed; it sorts files before comparison and reliably generates translation status reports in dev/translation_stats.
- New request: Create a script to parse the English strings.xml and list all string names.
- User renamed the script to remove_unused_strings.sh and is preparing to extend it to search for unused strings in Java files.
- New request: Remove all unused strings from all language strings.xml files and log exactly which strings are removed per file in dev/logs/remove_strings_en.log, remove_strings_de.log, etc.
- User reverted the removal after discovering a string was still in use; requests a --dry-run option for the script to preview removals without modifying files or creating logs.
- --dry-run option for unused string removal script is now implemented and has been verified to work as intended. Script now also uses improved string usage detection patterns.
- User requested: The unused string removal script should only search for usages in Java files, not in XML or other files, to avoid false positives from other translations.
- User reconsidered: The script should search for usages in all files except strings.xml files, to catch references in other code (e.g. Kotlin, C++, Gradle, etc). Next step: run a dry-run and analyze the results.
- User noticed that the string `robot_move_initiated` is not detected as unused, even though it appears to be unused, indicating a possible limitation or bug in the detection logic. Further investigation may be needed.
- Investigation revealed `robot_move_initiated` is only referenced in build artifacts and translation reports, not in actual code, so the script's logic is correct in not marking it as unused.
- Script was improved to exclude build, logs, and translation analysis directories from the unused string search, so now truly unused strings (like `robot_move_initiated`) are detected correctly.

## Task List
- [x] Diagnose and fix multi-color target mapping bug
- [x] Add/mark logging for unreachable code paths
- [x] Fix accessibility announcement: include coordinates in target announcement
- [x] Remove announcement: "KI berechnet Lösung ..."
- [x] Announce only the last step in hints, not the history
- [x] Use translated direction words (not arrows) in accessibility hints
- [x] Implement second pre-hint: show involved robot colors before first-move pre-hint
- [x] Verify all English strings exist in all other language translations
- [x] Create dev/translate.md to log missing translations and save the check script
- [x] Create a script that lists all string names from English strings.xml
- [x] Extend remove_unused_strings.sh to find unused strings in codebase
- [x] Remove all unused strings from all language strings.xml files and log removals per file
- [x] Implement --dry-run option for unused string removal script
- [x] Restrict unused string detection to all files except strings.xml
- [x] Run dry-run and analyze unused string detection results
- [x] Improve unused string detection by excluding build, logs, and translation analysis dirs
- [ ] Fill in missing translations for all non-English languages

## Current Goal
Use generated reports to systematically fill in missing translations