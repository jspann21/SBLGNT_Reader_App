"""Validate course JSON files and compile vocabulary into the Android corpus."""

import argparse
import json
import re
import sqlite3
import sys
import unicodedata
from contextlib import closing
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESERVED_IDS = {"user", "proper"}


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key {key!r}")
        result[key] = value
    return result


def normalize(word):
    return unicodedata.normalize("NFC", word)


def word_forms(text, normalized_text, lemma):
    match = re.fullmatch(r"(.*?)\((.+?)\)", text)
    forms = [match[1], match[1] + match[2]] if match else [text]
    if normalized_text:
        forms.append(normalized_text)
    if lemma:
        forms.append(lemma)
    return list(dict.fromkeys(normalize(form) for form in forms))


def load_courses(directory):
    courses = []
    for path in sorted(directory.glob("*.json")):
        try:
            if path.stat().st_size > 2 * 1024 * 1024:
                raise ValueError("file exceeds 2 MiB")
            data = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)
            required = {"version", "id", "name", "levelLabel", "levels"}
            if not isinstance(data, dict) or not required <= data.keys() or data.keys() - required - {"$schema"}:
                raise ValueError("expected version, id, name, levelLabel, levels (and optional $schema)")
            if type(data["version"]) is not int or data["version"] != 1:
                raise ValueError("version must be 1")
            if "$schema" in data and not isinstance(data["$schema"], str):
                raise ValueError("$schema must be a string")
            identifier = data["id"]
            if not isinstance(identifier, str) or not re.fullmatch(r"[a-z][a-z0-9]*(?:-[a-z0-9]+)*", identifier):
                raise ValueError("id must be a lowercase identifier, such as my-course")
            if identifier in RESERVED_IDS or identifier != path.stem:
                raise ValueError("id must match the filename and cannot be user or proper")
            for key, limit in [("name", 100), ("levelLabel", 30)]:
                value = data[key]
                if not isinstance(value, str) or not value.strip() or value != value.strip() or len(value) > limit or any(ord(c) < 32 for c in value):
                    raise ValueError(f"{key} must be nonempty text, at most {limit} characters, without surrounding whitespace or control characters")
            levels = data["levels"]
            if not isinstance(levels, dict) or not levels:
                raise ValueError("levels must be a nonempty object")
            words = {}
            for level, entries in levels.items():
                if not re.fullmatch(r"[1-9][0-9]{0,2}|1000", level):
                    raise ValueError(f"level {level!r} must be an integer from 1 to 1000, without leading zeros")
                if not isinstance(entries, list) or not entries:
                    raise ValueError(f"level {level} must contain a nonempty array of words")
                for word in entries:
                    if not isinstance(word, str) or not word.strip() or word != word.strip() or len(word) > 100 or any(c.isspace() or ord(c) < 32 for c in word):
                        raise ValueError(f"level {level}: each entry must be a single word without whitespace, at most 100 characters")
                    word = normalize(word)
                    if not any("GREEK" in unicodedata.name(c, "") for c in word):
                        raise ValueError(f"level {level}: {word!r} contains no Greek letters")
                    if word in words:
                        raise ValueError(f"level {level}: duplicate word {word!r} (already in level {words[word]})")
                    words[word] = int(level)
            if len(words) > 50_000:
                raise ValueError("list exceeds 50,000 entries")
            courses.append(dict(data, words=words))
        except (ValueError, OSError) as error:
            raise ValueError(f"{path.name}: {error}") from None
    if not courses:
        raise ValueError(f"no vocabulary lists found in {directory}")
    if len(courses) > 100:
        raise ValueError("at most 100 vocabulary lists are supported")
    return courses


def compile_courses(base, directory, output=None):
    courses = load_courses(directory)
    with closing(sqlite3.connect(base.resolve().as_uri() + "?mode=ro", uri=True)) as source:
        rows = source.execute("SELECT word_id, text, normalized_text, lemma FROM words ORDER BY word_id").fetchall()
        forms = [(word_id, word_forms(text, normalized_text, lemma)) for word_id, text, normalized_text, lemma in rows]
        known = {form for _, candidates in forms for form in candidates}
        for course in courses:
            unknown = sorted(course["words"].keys() - known)
            if len(unknown) == len(course["words"]):
                raise ValueError(f"{course['id']}.json: none of the entries match the corpus")
            print(f"{course['id']}: {len(course['words'])} words, levels {min(map(int, course['levels']))}–{max(map(int, course['levels']))}")
            if unknown:
                print(f"  Unmatched entries ({len(unknown)}): {', '.join(unknown)}")
        if output is None:
            return
        if output.resolve() == base.resolve():
            raise ValueError("output must not overwrite the base corpus")
        output.parent.mkdir(parents=True, exist_ok=True)
        temporary = output.with_suffix(".tmp")
        try:
            with closing(sqlite3.connect(temporary)) as target:
                source.backup(target)
                target.execute("DELETE FROM vocab_metadata")
                target.executemany(
                    "INSERT INTO vocab_metadata(vocab_name, display_name, level_label, min_level, max_level) VALUES(?,?,?,?,?)",
                    [(c["id"], c["name"], c["levelLabel"], min(map(int, c["levels"])), max(map(int, c["levels"]))) for c in courses],
                )
                updates = []
                for word_id, candidates in forms:
                    levels = {}
                    for course in courses:
                        level = next((course["words"][form] for form in candidates if form in course["words"]), None)
                        if level is not None:
                            levels[course["id"]] = level
                    updates.append((json.dumps(levels, sort_keys=True, separators=(",", ":")), word_id))
                target.executemany("UPDATE words SET vocab_levels = ? WHERE word_id = ?", updates)
                target.execute("DROP TABLE IF EXISTS room_master_table")
                target.execute("PRAGMA user_version = 1")
                target.commit()
                if target.execute("PRAGMA integrity_check").fetchone()[0] != "ok" or target.execute("PRAGMA foreign_key_check").fetchone():
                    raise ValueError("compiled corpus failed SQLite integrity checks")
            temporary.replace(output)
        finally:
            temporary.unlink(missing_ok=True)
    print(f"Compiled {len(courses)} lists for {len(rows)} word occurrences: {output}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path, default=ROOT / "tools/corpus/base.db")
    parser.add_argument("--lists", type=Path, default=ROOT / "vocabulary")
    parser.add_argument("--output", type=Path, help="write an Android database; omit to validate only")
    args = parser.parse_args()
    try:
        compile_courses(args.base, args.lists, args.output)
    except (ValueError, sqlite3.Error, OSError) as error:
        parser.exit(1, f"Vocabulary error: {error}\n")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
