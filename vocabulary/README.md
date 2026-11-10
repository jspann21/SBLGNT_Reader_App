# Add a vocabulary list

Add one JSON file to this directory and submit a pull request. The build discovers
the file and includes the list in Vocabulary Systems, word details, highlighting,
and learning progress. You do not need to edit Kotlin, SQL, Gradle, or a database.

## File format

Create `vocabulary/my-course.json`:

```json
{
  "$schema": "./schema/course.schema.json",
  "version": 1,
  "id": "my-course",
  "name": "My Greek Course",
  "levelLabel": "Lesson",
  "levels": {
    "1": ["λόγος", "θεός"],
    "2": ["ἀγάπη", "ζωή"]
  }
}
```

| Field | Meaning |
| --- | --- |
| `$schema` | Optional link to the editor schema. Keep this value when copying the example. |
| `version` | Format version. Use `1`. |
| `id` | Stable lowercase identifier matching the filename, without `.json`. Use letters, digits, and single hyphens, such as `my-course`. Start with a letter. `user` and `proper` are reserved. |
| `name` | Display name in the app. Maximum 100 characters. |
| `levelLabel` | Singular label such as `Chapter`, `Lesson`, or `Unit`. Maximum 30 characters. |
| `levels` | Level numbers mapped to arrays of Greek words. |

Levels are cumulative: selecting Lesson 2 includes Lessons 1 and 2. Levels can
start above 1 and can have gaps. Use numbers from 1 to 1000, without leading zeros.
Each level must contain at least one word.

For a list without lessons, put all its words in level `1`. The app shows a switch
without a level slider when a list has only one level.

## Words and matching

- Use UTF-8 Greek text, including accents and breathing marks.
- Prefer lexical forms, such as `λόγος`. An explicit inflected form can have its
  own level; it takes precedence over that word's lexical form.
- Include each word only once in a list, at the first level where it is taught.
- Use single words without spaces. Entries and display names cannot have
  surrounding whitespace. A word can contain at most 100 characters.
- Unicode-equivalent spellings match. The validator normalizes text to NFC and
  rejects duplicates across levels as well as duplicate JSON keys.
- Matching uses corpus text, surface forms without punctuation, and lexical forms.
  Optional endings in corpus forms, such as `ἐστί(ν)`, also match the form with or
  without that ending.

The validator reports entries that do not match the New Testament corpus. These
can be legitimate course vocabulary; they remain in the list and do not affect
coverage statistics. A list with no matching entries is rejected.

Keep the `id` stable when editing an existing list: the app uses it to store the
user's selected level and enabled state. Changing `name` changes only the display
name.

## Validate and submit

Install Python 3.11 or newer and run this from the repository root:

```sh
python tools/vocabulary/compile_lists.py
```

The command validates every list and reports errors with the filename and level
where applicable. No additional Python packages are required. Files must be no
larger than 2 MiB and contain at most 50,000 entries. The catalog supports up to
100 lists. JSON editors can also use [the schema](schema/course.schema.json).

Open a pull request containing your JSON file. Describe the list and its level
ordering in the pull request. GitHub Actions validates the files and runs the app's
checks and build automatically.

To build locally, follow [the development guide](../docs/DEVELOPMENT.md). The Gradle
build compiles the vocabulary into a generated database asset; leave generated
files out of the pull request. This format adds a built-in list through a new app
build. Personal vocabulary export/import in the app uses its own format.
