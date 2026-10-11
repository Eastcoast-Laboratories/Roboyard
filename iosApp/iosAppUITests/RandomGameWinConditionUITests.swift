// Verifies on the iOS simulator that a fresh install does not win a random
// game on the first move (regression: Preferences was never initialized on
// iOS, robotCount stayed 0 and every move completed the game).
//
// The test taps "Play", reads robot/target positions from the app's debug log
// (captured by `log stream` into ROBOYARD_LOG_FILE), moves a robot whose color
// does not match the target, and asserts the log shows a 1-robot win
// condition and no game completion.
//
// Run on the Mac (fresh install first so Preferences start from defaults):
//   S="iPhone 17"; L=/Users/m1/ry-stream.log
//   xcrun simctl uninstall "$S" de.z11.roboyard
//   (xcrun simctl spawn "$S" log stream --level debug --style compact \
//      --predicate 'process == "iosApp"' > $L &)
//   TEST_RUNNER_ROBOYARD_LOG_FILE=$L xcodebuild test -project iosApp.xcodeproj \
//     -scheme iosAppUITests -destination "platform=iOS Simulator,name=$S"
import XCTest

final class RandomGameWinConditionUITests: XCTestCase {

    private var logPath: String {
        ProcessInfo.processInfo.environment["ROBOYARD_LOG_FILE"] ?? "???MISSING(ROBOYARD_LOG_FILE)???"
    }

    private func readLog() -> String {
        (try? String(contentsOfFile: logPath, encoding: .utf8)) ?? ""
    }

    private func matches(_ pattern: String, in text: String) -> [[String]] {
        let regex = try! NSRegularExpression(pattern: pattern)
        let range = NSRange(text.startIndex..., in: text)
        return regex.matches(in: text, range: range).map { m in
            (1..<m.numberOfRanges).map { String(text[Range(m.range(at: $0), in: text)!]) }
        }
    }

    func testFirstMoveDoesNotWinRandomGame() throws {
        continueAfterFailure = false
        XCTAssertFalse(readLog().isEmpty, "[UITEST] log file \(logPath) empty — start `log stream` first")

        let app = XCUIApplication(bundleIdentifier: "de.z11.roboyard")
        app.launch()

        let play = app.descendants(matching: .any).matching(NSPredicate(format: "label == 'Play'")).firstMatch
        XCTAssertTrue(play.waitForExistence(timeout: 20), "[UITEST] Play button not found")
        // Compose Multiplatform accessibility nodes always report isHittable
        // == false, so taps go to the element's coordinates instead
        sleep(3)
        let logBeforeGame = readLog().count
        play.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()

        let board = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH 'Game board with'")).firstMatch
        XCTAssertTrue(board.waitForExistence(timeout: 60), "[UITEST] game board not shown")
        sleep(3)

        let gameLog = String(readLog().dropFirst(logBeforeGame))
        let size = matches("Game board with (\\d+)x(\\d+) cells", in: board.label).first!
        let (w, h) = (Double(size[0])!, Double(size[1])!)
        let target = matches("Adding target at \\((\\d+),(\\d+)\\) with color (\\d+)", in: gameLog).first
        let robots = matches("Added \\S+ robot at \\((\\d+),(\\d+)\\) with color ID (\\d+)", in: gameLog)
        XCTAssertNotNil(target, "[UITEST] no target in log")
        XCTAssertFalse(robots.isEmpty, "[UITEST] no robots in log")

        // Robot of a different color than the target cannot complete the game
        let robot = robots.first { $0[2] != target![2] }!
        let (rx, ry) = (Int(robot[0])!, Int(robot[1])!)
        let occupied = Set(robots.map { "\($0[0]),\($0[1])" })

        // Same cell geometry as GameScreen's tap handler
        let frame = board.frame
        let cell = min(frame.width / w, frame.height / h)
        let ox = (frame.width - w * cell) / 2
        let oy = (frame.height - h * cell) / 2
        func tapCell(_ x: Int, _ y: Int) {
            let point = board.coordinate(withNormalizedOffset: .zero).withOffset(
                CGVector(dx: ox + (Double(x) + 0.5) * cell, dy: oy + (Double(y) + 0.5) * cell))
            point.tap()
        }

        let neighbours = [(rx + 1, ry), (rx - 1, ry), (rx, ry + 1), (rx, ry - 1)]
            .filter { $0.0 >= 0 && $0.0 < Int(w) && $0.1 >= 0 && $0.1 < Int(h) && !occupied.contains("\($0.0),\($0.1)") }
        let logBeforeMove = readLog().count
        // A wall may block a direction; try neighbours until the robot moves
        for (nx, ny) in neighbours {
            tapCell(rx, ry)
            sleep(1)
            tapCell(nx, ny)
            sleep(3)
            if String(readLog().dropFirst(logBeforeMove)).contains("Movement INITIATED") { break }
        }
        sleep(2)

        let moveLog = String(readLog().dropFirst(logBeforeMove))
        XCTAssertTrue(moveLog.contains("Movement INITIATED"), "[UITEST] robot did not move:\n\(moveLog.suffix(2000))")
        XCTAssertTrue(moveLog.contains("(required: 1,"), "[UITEST] win condition does not require 1 robot:\n\(moveLog.suffix(2000))")
        XCTAssertFalse(moveLog.contains("Setting game complete: true"), "[UITEST] game completed after first move")
    }
}
