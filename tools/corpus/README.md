# Corpus tooling and source data

Source data and Python tools for generating the Android app's bundled database.
Normal Android builds use the checked-in base corpus and compile vocabulary into
the packaged database automatically.

- `source/`: Greek text, lexical fields, and paragraph divisions.
- `base.db`: checked-in corpus without course-specific vocabulary assignments.
- `create_database.py`: Python standard-library SQLite generator.
- `normalize_chars.py`: normalization utility writing copies into an `output/` directory.

From the repository root:

```sh
python tools/corpus/create_database.py
python -m pytest tools/corpus/test_normalize_chars.py
```

The generator writes ignored `tools/corpus/generated-base.db`. It does not
overwrite `base.db`. Before replacing the base corpus, compare text, lexical
fields, word IDs, tables, indices, data counts, and Room schemas, and run the
Android storage and regression checks.

Course data lives in [`vocabulary/`](../../vocabulary/README.md). The Gradle build
validates those JSON files and compiles their assignments into a copy of the base
corpus. Course IDs, names, level labels, and per-word levels use the same schema
for every list. Adding a list does not require corpus regeneration or SQL changes.

The data is a transformed compilation, not an original text by the app author.
Attribution and separate data license terms apply to these sources and the generated
database: see [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md). In particular,
Lexham-derived gloss, literal, and reference fields are not relicensed under the app's GPL.
