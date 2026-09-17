#!/usr/bin/env python3
"""Sync missing <string> entries from the Android strings.xml files into
composeApp/src/commonMain/resources/strings/strings.json.

Android format args (%1$s, %2$d, ...) are converted to the JSON convention
({0}, {1}, ...). Existing JSON entries are never overwritten — the JSON is
the hand-maintained source for Compose, this only fills gaps.

Run: python3 dev/scripts/sync_strings_json.py
"""
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
RES = REPO / "app/src/main/res"
JSON_PATH = REPO / "composeApp/src/commonMain/resources/strings/strings.json"

# strings.xml resource dir -> strings.json locale key
LOCALE_DIRS = {
    "values": "en",
    "values-de": "de",
    "values-es": "es",
    "values-fr": "fr",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-pl": "pl",
    "values-pt-rBR": "pt",
    "values-zh": "zh",
}

FORMAT_ARG = re.compile(r"%(\d+)\$[sdfoxeg]")


def android_to_json(value: str) -> str:
    """Unescape Android string escapes and convert %N$x args to {N-1}."""
    value = value.replace("\\'", "'").replace('\\"', '"').replace("\\\\", "\\")
    return FORMAT_ARG.sub(lambda m: "{%d}" % (int(m.group(1)) - 1), value)


def load_xml_strings(xml_path: Path) -> dict:
    """Return {name: value} for all <string> elements."""
    strings = {}
    for elem in ET.parse(xml_path).getroot().iter("string"):
        name = elem.get("name")
        if name:
            strings[name] = android_to_json("".join(elem.itertext()).strip())
    return strings


def main() -> None:
    data = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    total_added = 0
    for res_dir, locale in LOCALE_DIRS.items():
        xml_path = RES / res_dir / "strings.xml"
        if not xml_path.exists():
            print(f"SKIP {res_dir}: no strings.xml")
            continue
        xml_strings = load_xml_strings(xml_path)
        existing = data.setdefault(locale, {})
        added = 0
        for name, value in xml_strings.items():
            if name not in existing:
                existing[name] = value
                added += 1
        total_added += added
        print(f"{locale}: {added} added, {len(existing)} total")
    JSON_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"Done: {total_added} keys added -> {JSON_PATH}")


if __name__ == "__main__":
    main()
