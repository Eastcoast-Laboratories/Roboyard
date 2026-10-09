#!/usr/bin/env python3
"""Generate composeApp/src/commonMain/resources/strings/strings.json from the
Android strings.xml files (app/src/main/res/values*/strings.xml).

The Android strings.xml files are the single source of truth for all texts;
the JSON is fully regenerated on every run and must never be edited by hand.
It runs automatically before every composeApp resource processing step
(Gradle task :composeApp:generateStringsJson).

Android format args are converted to the JSON convention: positional
%1$s/%2$d/... become {0}/{1}/..., non-positional %s/%d are numbered in order.
JSON has no comments, so the generated file starts with a "_comment" object
(an object, because the desktop parser expects locale -> object entries).

Run manually: python3 dev/scripts/sync_strings_json.py
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
    "values-fr": "fr",
    "values-es": "es",
    "values-zh": "zh",
    "values-ko": "ko",
    "values-ja": "ja",
    "values-pt-rBR": "pt",
    "values-pl": "pl",
}

COMMENT = {
    "warning": "AUTO-GENERATED from app/src/main/res/values*/strings.xml by "
               "dev/scripts/sync_strings_json.py - DO NOT EDIT. Change the Android "
               "strings.xml files instead; this file is regenerated on every build."
}

FORMAT_ARG = re.compile(r"%(?:(\d+)\$)?[sdfoxeg]")
# Android/aapt decodes Java-style \uXXXX escapes in string values (e.g. the
# zero-width space \u200B used in history_detail_qualifies_no_hints_perfect).
# Escaped \\uXXXX stays literal, like aapt's \\ -> \ handling.
UNICODE_ESCAPE = re.compile(r"(?<!\\)\\u([0-9a-fA-F]{4})")


def android_to_json(value: str) -> str:
    """Unescape Android string escapes and convert format args to {N}."""
    value = UNICODE_ESCAPE.sub(lambda m: chr(int(m.group(1), 16)), value)
    value = value.replace("\\'", "'").replace('\\"', '"').replace("\\\\", "\\")
    counter = iter(range(1000))

    def convert(m: re.Match) -> str:
        index = int(m.group(1)) - 1 if m.group(1) else next(counter)
        return "{%d}" % index

    return FORMAT_ARG.sub(convert, value)


def load_xml_strings(xml_path: Path) -> dict:
    """Return {name: value} for all <string> elements."""
    strings = {}
    for elem in ET.parse(xml_path).getroot().iter("string"):
        name = elem.get("name")
        if name:
            strings[name] = android_to_json("".join(elem.itertext()).strip())
    return strings


def main() -> None:
    data = {"_comment": COMMENT}
    for res_dir, locale in LOCALE_DIRS.items():
        xml_path = RES / res_dir / "strings.xml"
        if not xml_path.exists():
            raise SystemExit(f"[STRINGS_JSON] missing {xml_path}")
        data[locale] = load_xml_strings(xml_path)
        print(f"[STRINGS_JSON] {locale}: {len(data[locale])} strings")
    content = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if JSON_PATH.exists() and JSON_PATH.read_text(encoding="utf-8") == content:
        print(f"[STRINGS_JSON] up to date: {JSON_PATH}")
        return
    JSON_PATH.write_text(content, encoding="utf-8")
    print(f"[STRINGS_JSON] written: {JSON_PATH}")


if __name__ == "__main__":
    main()
