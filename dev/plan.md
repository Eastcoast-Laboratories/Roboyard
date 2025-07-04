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

## Task List
- [x] Diagnose and fix multi-color target mapping bug
- [x] Add/mark logging for unreachable code paths
- [x] Fix accessibility announcement: include coordinates in target announcement
- [x] Remove announcement: "KI berechnet Lösung ..."
- [x] Announce only the last step in hints, not the history
- [x] Use translated direction words (not arrows) in accessibility hints
- [x] Implement second pre-hint: show involved robot colors before first-move pre-hint

## Current Goal
All planned accessibility and solver fixes complete