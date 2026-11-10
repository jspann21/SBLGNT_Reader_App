# Third-party data, fonts, and software

These notices apply to the bundled Android database, `tools/corpus/source/`,
vocabulary data, and fonts wherever distributed, including APK releases. Original
app code is GPL-3.0-only, copyright (C) 2025–2026 jspann21 and contributors. That
license does not replace the separate terms below.

## SBL Greek New Testament

The Greek New Testament: SBL Edition, edited by Michael W. Holmes.
Copyright 2010 Society of Biblical Literature and Logos Bible Software.
The upstream text is available under Creative Commons Attribution 4.0 International.

- Source: <https://github.com/Faithlife/SBLGNT>
- Text and license: <https://sblgnt.com/license/>
- License copy: `app/src/main/assets/licenses/SBLGNT-CC-BY-4.0.txt`
- Paragraph divisions: <https://github.com/aaronshaf/sblgnt>, whose README refers to
  the SBLGNT license.

This app transforms and combines text into normalized JSON and SQLite tables,
paragraph and verse display, counts, and vocabulary statistics. It is not an
official publication or endorsement by these organizations.

## MorphGNT

Tauber, J. K., ed. (2017). MorphGNT: SBLGNT Edition. Version 6.12 [Data set].
DOI: 10.5281/zenodo.376200. <https://github.com/morphgnt/sblgnt>.
Morphological parsing and lemmatization are licensed under Creative Commons
Attribution-ShareAlike 3.0 Unported:
<https://creativecommons.org/licenses/by-sa/3.0/>.
The text has its own SBLGNT terms above. This app imports and normalizes analysis
into lexical fields and database tables; adapted MorphGNT material retains its
attribution and share-alike terms. License copy:
`app/src/main/assets/licenses/MorphGNT-CC-BY-SA-3.0.txt`.

## Lexham English Bible English–Greek Reverse Interlinear New Testament

Copyright 2010 Logos Bible Software. W. Hall Harris III (LEB editor), Michael W.
Holmes (SBLGNT editor), and Rick Brannan (alignment editor).
Source: <https://www.sblgnt.com/download/>.

Lexical glosses, literal meanings, alignments, and reference fields include
Lexham material credited to Logos Bible Software and used with permission.
Lexham material retains its own rights and is not covered by the app's GPL license.

## Vocabulary course mappings

The Mounce vocabulary course is keyed to William D. Mounce's
*Basics of Biblical Greek Grammar*, fourth edition. Credit for that course's
vocabulary and chapter ordering belongs to Mounce and the respective rights
holders. GNT Reader is not affiliated with or endorsed by William D. Mounce,
Zondervan, or the authors or publishers of the Smith course.

The Mounce and Smith course mappings are supplied as study aids. Third-party
course material retains its owners' rights; the app's GPL license does not
relicense that material or grant additional redistribution rights.

## SBL Greek font

SBL Greek, by John Hudson, Tiro Typeworks. Copyright 2003 and 2009 John Hudson,
Tiro Typeworks. All rights reserved. The font is distributed unmodified under its
embedded SBL Font User Agreement, copied verbatim to
`app/src/main/assets/licenses/SBL-Greek.txt`. Free redistribution and non-commercial
use have conditions; commercial use needs separate permission. The font is not GPL.
Official information: <https://www.sbl-site.org/resources/fonts/>.

## Application dependencies

AndroidX, Material Components, Hilt/Dagger, Kotlin and kotlinx.coroutines, Firebase
Android SDKs, and Coil retain their upstream copyright and license notices. Most
runtime components use Apache-2.0; its license is bundled at
`app/src/main/assets/licenses/Apache-2.0.txt`. Consult upstream distributions and each
resolved artifact's `META-INF` notices for the applicable versions. Development
tools and test dependencies retain their own licenses. No endorsement is implied.
