#!/usr/bin/env python3
import subprocess
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]

def run_lint():
    print("Running Android Lint for missing translations...")
    
    result = subprocess.run(
        ["./gradlew", "lintDebug"],
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True
    )

    report = PROJECT_ROOT / "app/build/reports/lint-results-debug.html"

    if report.exists():
        print(f"Lint report generated: {report}")
    else:
        print("Lint report not found")

    print(result.stdout)

if __name__ == "__main__":
    run_lint()