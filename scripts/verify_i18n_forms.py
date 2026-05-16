#!/usr/bin/env python3
"""Audit every English text inside the NetBeans .form files.

For each `<Property name="text|toolTipText|title" value="..."/>` in a form,
verify the value appears as a VALUE in messages.properties so the legacy
LabelTranslatorSingleton.translate(...) inverse lookup can resolve it.

Misses surface as untranslated UI strings at runtime, even though
`translateLabels(this)` walks the component tree -- it just doesn't find
a key in the bundle for that English literal.

Filters out values that are clearly not user-localizable (placeholders like
"---", "[0 B]", bare punctuation, single non-alpha tokens).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FORMS_DIR = ROOT / "src" / "main" / "java"
BUNDLE = ROOT / "src" / "main" / "resources" / "i18n" / "messages.properties"

PROP_RE = re.compile(
    r'<Property\s+name="(text|toolTipText|title)"\s+type="java\.lang\.String"\s+value="([^"]*)"\s*/>'
)


def decode_xml(s: str) -> str:
    """NetBeans serializes .form values with XML entity escaping; the
    runtime label receives the decoded text. Decode here for accurate
    bundle-value comparison."""
    return (
        s.replace("&apos;", "'")
         .replace("&quot;", '"')
         .replace("&lt;", "<")
         .replace("&gt;", ">")
         .replace("&#xa;", "\n")
         .replace("&#xd;", "\r")
         .replace("&#x9;", "\t")
         .replace("&amp;", "&")  # last so we don't double-decode
    )


def parse_bundle_values(path: Path):
    values = set()
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.lstrip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if "=" not in line:
            continue
        _, v = line.split("=", 1)
        v = (v.replace("\\\\", "\x00")
             .replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r")
             .replace("\x00", "\\"))
        values.add(v)
    return values


def looks_translatable(s: str) -> bool:
    """Return True if `s` is plausibly user-localizable text."""
    if not s:
        return False
    if not any(c.isalpha() for c in s):
        return False
    # Whitelist of NetBeans defaults, runtime placeholders, format specs, and
    # GitHub handles (translator credits) that intentionally stay as-is.
    s_strip = s.strip()
    if s_strip in {
        "---", "...", "?", "!",
        "jLabel1", "jLabel2", "jLabel3", "jLabel4", "jLabel5", "jLabel6",
        "jPasswordField1",
        "CBC-MAC 000%",
        "[*]IP:PORT[@user_b64:password_b64] OR #PROXY_LIST_URL",
        "FabrieI", "NieckLikesCode", "Roschach96", "bovirus", "linkea131",
        "rattybox",
    }:
        return False
    return True


def main() -> int:
    values = parse_bundle_values(BUNDLE)
    print(f"Bundle values: {len(values)}")

    misses_by_value = {}
    total_props = 0

    for path in sorted(FORMS_DIR.rglob("*.form")):
        text = path.read_text(encoding="utf-8")
        for m in PROP_RE.finditer(text):
            total_props += 1
            attr, val = m.group(1), decode_xml(m.group(2))
            if not looks_translatable(val):
                continue
            if val in values:
                continue
            line = text[:m.start()].count("\n") + 1
            misses_by_value.setdefault(val, []).append(
                (str(path.relative_to(ROOT)), line, attr)
            )

    print(f".form text/toolTipText/title properties scanned: {total_props}")
    print(f"Distinct values NOT in bundle: {len(misses_by_value)}")
    print()
    for val, sites in sorted(misses_by_value.items()):
        print(f"  {val!r}")
        for p, l, a in sites[:3]:
            print(f"      {p}:{l} ({a})")
        if len(sites) > 3:
            print(f"      ... and {len(sites) - 3} more")
    return 1 if misses_by_value else 0


if __name__ == "__main__":
    raise SystemExit(main())
