# Test Refactoring Progress

## Phase 1: TestHelper DRY Extraction
- [x] `executeSolutionMoves()` in TestHelper extrahiert
- [x] `waitForSolution()` in TestHelper extrahiert
- [x] `completeLevelWithSolver()` in TestHelper extrahiert
- [x] `selectRobotAndMove()` in TestHelper extrahiert
- [x] `getDirectionX/Y()` in TestHelper extrahiert

## Phase 2: Level-Navigation Fix (TestHelper.startLevelGame)
- [x] Level1FastE2ETest 
- [x] Level1SlowE2ETest 
- [x] Level1WrongE2ETest 
- [x] Level3E2ETest 
- [x] Level10E2ETest 
- [x] Level111DebugTest 
- [x] LiveMoveCounterE2ETest  (4/4)
- [x] GameModeMemoryE2ETest  (+ AchievementsFragment back-nav bug fix)

## Phase 3: DRY — executeSolutionMoves durch TestHelper ersetzt
- [x] Level3E2ETest 
- [x] Level10E2ETest 
- [x] Level111DebugTest 
- [x] Level11With2StarsE2ETest 
- [x] Level140E2ETest (DRY only, [long] not re-run)
- [x] BoardSizeResetLevel2E2ETest 
- [x] PerfectRandom5E2ETest (DRY only, [long] not re-run)
- [x] RandomGame11E2ETest (DRY only, [long] not re-run)

## Phase 4: Restliche failing Tests
- [ ] AchievementsFragmentTest
- [ ] AlternativeLayoutTest
- [ ] BackgroundSoundSettingsTest
- [ ] DebugHistoryTest
- [ ] HistoryCompletionE2ETest
- [ ] HistoryPaginationTest
- [ ] LevelEditorDebugTest
- [ ] LevelSelectionLandscapeTest
- [ ] LoadGameDifficultyTest
- [ ] HintAutoModeE2ETest

## Phase 5: Non-UI Tests
- [ ] GameHistoryManager
- [ ] ShareParsing
- [ ] WallSerialization

## Phase 6: Achievement Tests
- [ ] AchievementManager
- [ ] GimmeFive
- [ ] ThreeStar

## Phase 7: Documentation
- [ ] TESTSUITE.md aktualisieren
- [ ] test-repair.md aktualisieren
