#!/usr/bin/env python3
"""Cross-check every i18n call site against the bundle.

Two passes:
  (1) Every I18n.tr("key", ...) -> "key" must exist in messages.properties.
  (2) Every LabelTranslatorSingleton.getInstance().translate("literal") ->
      "literal" should appear as a VALUE in messages.properties (so the
      english-index inversion can resolve it). Misses are not fatal but
      will surface untranslated text at runtime.

Exits non-zero on any (1) miss. Reports (2) misses as warnings.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src" / "main" / "java"
BUNDLE = ROOT / "src" / "main" / "resources" / "i18n" / "messages.properties"

I18N_RE = re.compile(r'I18n\.tr\(\s*"((?:[^"\\]|\\.)*)"')
TRANSLATE_RE = re.compile(
    r'LabelTranslatorSingleton\.getInstance\(\)\.translate\(\s*"((?:[^"\\]|\\.)*)"\s*\)'
)


def unescape_java(s: str) -> str:
    out = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == "\\" and i + 1 < len(s):
            n = s[i + 1]
            if n == "n":   out.append("\n"); i += 2
            elif n == "t": out.append("\t"); i += 2
            elif n == "r": out.append("\r"); i += 2
            elif n == '"': out.append('"'); i += 2
            elif n == "\\":out.append("\\"); i += 2
            elif n == "u" and i + 5 < len(s):
                out.append(chr(int(s[i+2:i+6], 16))); i += 6
            else:
                out.append(n); i += 2
        else:
            out.append(c); i += 1
    return "".join(out)


def parse_bundle(path: Path):
    keys = {}
    values_to_keys = {}
    cur_key, cur_val_lines = None, []
    for raw in path.read_text(encoding="utf-8").splitlines():
        if not raw or raw.lstrip().startswith("#") or raw.lstrip().startswith("!"):
            continue
        if "=" not in raw:
            continue
        k, v = raw.split("=", 1)
        # decode properties escapes (\\, \n, \t, \r)
        v = (v.replace("\\\\", "\x00")
             .replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r")
             .replace("\x00", "\\"))
        keys[k] = v
        values_to_keys.setdefault(v, k)
    return keys, values_to_keys


def main() -> int:
    keys, values = parse_bundle(BUNDLE)
    print(f"Bundle keys:   {len(keys)}")
    print(f"Bundle values: {len(values)}  (some keys share values)")

    tr_missing = []
    legacy_missing = []
    tr_count = 0
    legacy_count = 0

    for path in JAVA.rglob("*.java"):
        text = path.read_text(encoding="utf-8")

        for m in I18N_RE.finditer(text):
            tr_count += 1
            key = unescape_java(m.group(1))
            if key not in keys:
                line = text[:m.start()].count("\n") + 1
                tr_missing.append((str(path.relative_to(ROOT)), line, key))

        for m in TRANSLATE_RE.finditer(text):
            legacy_count += 1
            literal = unescape_java(m.group(1))
            if literal not in values:
                line = text[:m.start()].count("\n") + 1
                legacy_missing.append((str(path.relative_to(ROOT)), line, literal))

    print()
    print(f"I18n.tr(...) call sites:                  {tr_count}")
    print(f"  with KEY MISSING from bundle:           {len(tr_missing)}")
    print(f"LabelTranslatorSingleton.translate(...):  {legacy_count}")
    print(f"  with LITERAL not found as bundle VALUE: {len(legacy_missing)}")

    if tr_missing:
        print()
        print("== FATAL: I18n.tr() keys not in messages.properties ==")
        for p, l, k in tr_missing:
            print(f"  {p}:{l}  key={k!r}")

    if legacy_missing:
        print()
        print("== WARN: translate(literal) with no bundle entry (returns input unchanged) ==")
        for p, l, k in legacy_missing[:40]:
            print(f"  {p}:{l}  literal={k!r}")
        if len(legacy_missing) > 40:
            print(f"  ... and {len(legacy_missing) - 40} more")

    return 1 if tr_missing else 0


if __name__ == "__main__":
    raise SystemExit(main())
