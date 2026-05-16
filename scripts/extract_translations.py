#!/usr/bin/env python3
"""One-shot extractor: LabelTranslatorSingleton.java -> i18n .properties bundle.

Reads every _addTranslation("english", "localized") call grouped by language
method (Spanish/Italian/German/Hungarian/Turkish/Chinese/Vietnamese), produces:

  src/main/resources/i18n/messages.properties     (English = canonical source)
  src/main/resources/i18n/messages_es.properties  (Spanish)
  src/main/resources/i18n/messages_it.properties
  src/main/resources/i18n/messages_de.properties
  src/main/resources/i18n/messages_hu.properties
  src/main/resources/i18n/messages_tr.properties
  src/main/resources/i18n/messages_zh.properties
  src/main/resources/i18n/messages_vi.properties

Keys are deterministic readable slugs derived from the English literal,
with `__N` suffix to break collisions. The English bundle (messages.properties)
stores key=english-literal, which is also the lookup table the legacy
translate(String orig) shim inverts at boot to map orig -> key.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path
from collections import OrderedDict

REPO = Path(__file__).resolve().parent.parent
SRC = REPO / "src" / "main" / "java" / "com" / "tonikelope" / "megabasterd" / "LabelTranslatorSingleton.java"
OUT = REPO / "src" / "main" / "resources" / "i18n"

# Order matters: English keys are discovered in this method order. The first
# method in source (German) is processed first so its keys define ordering.
LANG_METHODS = [
    ("German",     "de"),
    ("Hungarian",  "hu"),
    ("Vietnamese", "vi"),
    ("Italian",    "it"),
    ("Chinese",    "zh"),
    ("Turkish",    "tr"),
    ("Spanish",    "es"),
]

CALL_RE = re.compile(
    r'_addTranslation\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*\)\s*;'
)


def unescape_java(s: str) -> str:
    """Decode a Java string literal body to its actual character content."""
    out = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == "\\" and i + 1 < len(s):
            nxt = s[i + 1]
            if nxt == "n":
                out.append("\n"); i += 2
            elif nxt == "t":
                out.append("\t"); i += 2
            elif nxt == "r":
                out.append("\r"); i += 2
            elif nxt == "b":
                out.append("\b"); i += 2
            elif nxt == "f":
                out.append("\f"); i += 2
            elif nxt == '"':
                out.append('"'); i += 2
            elif nxt == "'":
                out.append("'"); i += 2
            elif nxt == "\\":
                out.append("\\"); i += 2
            elif nxt == "u" and i + 5 < len(s):
                out.append(chr(int(s[i + 2:i + 6], 16))); i += 6
            elif nxt.isdigit():
                # octal, up to 3 digits (max \377)
                j = i + 1
                end = min(i + 4, len(s))
                while j < end and s[j].isdigit() and (j - (i + 1) < 3):
                    j += 1
                out.append(chr(int(s[i + 1:j], 8)))
                i = j
            else:
                out.append(nxt); i += 2
        else:
            out.append(c); i += 1
    return "".join(out)


def slugify(text: str, max_len: int = 60) -> str:
    """Readable kebab-style key derived from English text."""
    s = text.lower()
    s = re.sub(r"[^a-z0-9]+", "_", s)
    s = s.strip("_")
    if not s:
        s = "s"
    if len(s) > max_len:
        # cut at last underscore before max_len, else hard-cut
        cut = s.rfind("_", 0, max_len)
        s = s[:cut] if cut >= max_len // 2 else s[:max_len]
        s = s.strip("_") or "s"
    return s


def find_method_body(src: str, name: str) -> str:
    """Return the body of `private void <name>() { ... }` (excluding braces)."""
    pat = re.compile(r"private\s+void\s+" + re.escape(name) + r"\s*\(\s*\)\s*\{")
    m = pat.search(src)
    if not m:
        raise SystemExit(f"method {name}() not found")
    # walk braces to find matching close
    depth = 1
    i = m.end()
    while i < len(src) and depth > 0:
        c = src[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return src[m.end():i]
        i += 1
    raise SystemExit(f"unbalanced braces in {name}()")


def parse_method(body: str) -> "OrderedDict[str, str]":
    """Return english_literal -> localized_literal (decoded), order-preserving."""
    out: "OrderedDict[str, str]" = OrderedDict()
    for m in CALL_RE.finditer(body):
        eng = unescape_java(m.group(1))
        loc = unescape_java(m.group(2))
        if eng not in out:
            out[eng] = loc
    return out


def escape_properties_key(s: str) -> str:
    """Escape a key for Properties file format."""
    out = []
    for c in s:
        if c == "\\":
            out.append("\\\\")
        elif c == "\n":
            out.append("\\n")
        elif c == "\r":
            out.append("\\r")
        elif c == "\t":
            out.append("\\t")
        elif c in " :=":
            out.append("\\" + c)
        elif ord(c) < 0x20:
            out.append("\\u%04x" % ord(c))
        else:
            out.append(c)
    return "".join(out)


def escape_properties_value(s: str) -> str:
    out = []
    for i, c in enumerate(s):
        if c == "\\":
            out.append("\\\\")
        elif c == "\n":
            out.append("\\n")
        elif c == "\r":
            out.append("\\r")
        elif c == "\t":
            out.append("\\t")
        elif c == " " and i == 0:
            out.append("\\ ")
        elif ord(c) < 0x20:
            out.append("\\u%04x" % ord(c))
        else:
            out.append(c)
    return "".join(out)


def write_bundle(path: Path, header: str, entries) -> None:
    """entries: iterable of (key, value, eng_comment).
    Writes UTF-8 .properties. ResourceBundle is loaded with a UTF-8 Control
    on the Java side so non-ASCII content is preserved verbatim."""
    lines = [
        "# " + header,
        "# Generated by scripts/extract_translations.py from",
        "# LabelTranslatorSingleton.java -- DO NOT EDIT BY HAND on first generation.",
        "# Subsequent edits by translators ARE the source of truth.",
        "",
    ]
    last_section = None
    for key, value, eng in entries:
        if eng is not None and eng != value:
            lines.append("# EN: " + eng.replace("\n", " \\n ").replace("\r", ""))
        lines.append(escape_properties_key(key) + "=" + escape_properties_value(value))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    src = SRC.read_text(encoding="utf-8")

    # 1. Parse every language method.
    per_lang: dict = {}
    for method, code in LANG_METHODS:
        body = find_method_body(src, method)
        per_lang[code] = parse_method(body)
        print(f"  {method:11s} -> {len(per_lang[code]):5d} entries", file=sys.stderr)

    # 2. Build the canonical English key set (union, preserving discovery order).
    #    First method in LANG_METHODS seeds the order; later methods append any
    #    English strings that weren't seen yet.
    eng_order: list = []
    seen: set = set()
    for _, code in LANG_METHODS:
        for eng in per_lang[code].keys():
            if eng not in seen:
                seen.add(eng)
                eng_order.append(eng)
    print(f"  union: {len(eng_order)} unique English strings", file=sys.stderr)

    # 3. Assign stable keys (slug + collision suffix).
    eng_to_key: "OrderedDict[str, str]" = OrderedDict()
    used: dict = {}
    for eng in eng_order:
        base = slugify(eng)
        n = used.get(base, 0)
        if n == 0:
            key = base
        else:
            key = f"{base}__{n + 1}"
        used[base] = n + 1
        eng_to_key[eng] = key

    # 4. Write the English (default) bundle: key = english literal.
    write_bundle(
        OUT / "messages.properties",
        "English (canonical source, also serves as english-literal -> key index)",
        ((key, eng, None) for eng, key in eng_to_key.items()),
    )

    # 5. Write each localized bundle. Skip missing entries (ResourceBundle will
    #    fall back to messages.properties = English).
    for method, code in LANG_METHODS:
        rows = []
        for eng, key in eng_to_key.items():
            loc = per_lang[code].get(eng)
            if loc is None:
                continue
            rows.append((key, loc, eng))
        write_bundle(
            OUT / f"messages_{code}.properties",
            f"{method} translations",
            rows,
        )
        print(f"  wrote messages_{code}.properties ({len(rows)} entries)", file=sys.stderr)

    print(f"DONE. Output in {OUT}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
