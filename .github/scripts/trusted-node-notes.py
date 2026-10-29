#!/usr/bin/env python3
"""Print the release note for one trusted-node image version from the changelog.

Usage: trusted-node-notes.py [--blocks] <CHANGELOG.md> <version> [locale]

Changelog layout: a `## <version>` heading per release; the text under it is the English note,
optional `### <locale>` subsections (es_ES, de_DE, ...) hold translations. A note is made of
paragraphs (separated by blank lines) and list items (lines starting with `- `).

Default output is the note on one line, with the list markers and `**` emphasis removed, for the
targets that take a single plain string (StartOS, PR bodies, the Security check).
With --blocks the output is a JSON list of {"kind": "p" | "li", "text": "..."}, one entry per
paragraph or list item, for the targets that keep the structure (the Umbrel store manifests).

Prints nothing (exit 0) when the version or locale is absent, so callers can fall back to a
generated note.
"""
import json
import re
import sys


def section_lines(text: str, version: str, locale: str | None) -> list[str]:
    section: list[str] = []
    inside = False
    for line in text.splitlines():
        if line.startswith("## "):
            if inside:
                break
            inside = line[3:].strip().split()[:1] == [version]
            continue
        if inside:
            section.append(line)
    wanted: list[str] = []
    current = None  # None = English text before the first ### heading
    for line in section:
        m = re.match(r"^### +(\S+)", line)
        if m:
            current = m.group(1)
            continue
        if current == locale:
            wanted.append(line)
    return wanted


def blocks(lines: list[str]) -> list[dict]:
    """Groups lines into paragraphs and list items; a list item runs until a blank line or the next item."""
    out: list[dict] = []
    current: dict | None = None
    for line in lines:
        stripped = line.strip()
        if not stripped:
            current = None
            continue
        if stripped.startswith("- "):
            current = {"kind": "li", "text": stripped[2:].strip()}
            out.append(current)
        elif current is None:
            current = {"kind": "p", "text": stripped}
            out.append(current)
        else:
            current["text"] += " " + stripped
    for block in out:
        block["text"] = " ".join(block["text"].split())
    return out


def flatten(note_blocks: list[dict]) -> str:
    text = " ".join(block["text"] for block in note_blocks)
    return " ".join(text.replace("**", "").split())


def main() -> int:
    args = sys.argv[1:]
    as_blocks = bool(args) and args[0] == "--blocks"
    if as_blocks:
        args = args[1:]
    if len(args) not in (2, 3):
        print(__doc__, file=sys.stderr)
        return 2
    path, version = args[0], args[1]
    locale = args[2] if len(args) == 3 else None
    with open(path, encoding="utf-8-sig") as f:  # -sig: drop a BOM so the note starts with its text
        note_blocks = blocks(section_lines(f.read(), version, locale))
    if note_blocks:
        print(json.dumps(note_blocks, ensure_ascii=False) if as_blocks else flatten(note_blocks))
    return 0


if __name__ == "__main__":
    sys.exit(main())
