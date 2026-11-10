import json
import sqlite3
from pathlib import Path

# --- Configuration ---
CORPUS_ROOT = Path(__file__).resolve().parent
JSON_DATA_PATH = CORPUS_ROOT / "source"
BOOKS_LIST_PATH = JSON_DATA_PATH / "books.json"
PARAGRAPHS_ROOT_PATH = JSON_DATA_PATH / "paragraphs"
DATABASE_OUTPUT_PATH = CORPUS_ROOT / "generated-base.db" # Compare with base.db before replacing it

# --- Database Schema (Base: Books, Chapters, Verses, Words, Paragraphs) ---
BASE_SCHEMA = """
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS books (
    book_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    osis_name TEXT UNIQUE NOT NULL -- Store the filename/OSIS-like name e.g., "1Corinthians"
);

CREATE TABLE IF NOT EXISTS chapters (
    chapter_id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL,
    chapter_number INTEGER NOT NULL,
    FOREIGN KEY(book_id) REFERENCES books(book_id),
    UNIQUE(book_id, chapter_number)
);

CREATE TABLE IF NOT EXISTS verses (
    verse_id INTEGER PRIMARY KEY AUTOINCREMENT,
    chapter_id INTEGER NOT NULL,
    verse_number TEXT NOT NULL, -- Use TEXT to handle verse numbers like "1a", "1b" if they exist
    FOREIGN KEY(chapter_id) REFERENCES chapters(chapter_id),
    UNIQUE(chapter_id, verse_number)
);

-- Note: Word structure based on schema description and script.js inspection
CREATE TABLE IF NOT EXISTS words (
    word_id INTEGER PRIMARY KEY AUTOINCREMENT,
    verse_id INTEGER NOT NULL,
    word_index INTEGER NOT NULL, -- Position of the word within the verse
    text TEXT NOT NULL,          -- The actual Greek word text
    normalized_text TEXT,        -- Text without punctuation for easier searching/matching
    pos_tag TEXT,                -- Part of speech tag
    gloss TEXT,                  -- English gloss
    lemma TEXT,                  -- Lexical form (derived from word_forms[3])
    literal TEXT,                -- Literal meaning
    louw TEXT,                   -- Louw-Nida reference
    strong TEXT,                 -- Strong's number
    type TEXT,                   -- Word type (e.g., 'proper' for proper nouns)
    vocab_levels TEXT NOT NULL DEFAULT '{}',
    FOREIGN KEY(verse_id) REFERENCES verses(verse_id),
    UNIQUE(verse_id, word_index)
);


CREATE TABLE IF NOT EXISTS paragraphs (
    paragraph_id INTEGER PRIMARY KEY AUTOINCREMENT,
    chapter_id INTEGER NOT NULL,
    paragraph_index INTEGER NOT NULL, -- Order of paragraph within the chapter
    verse_spans TEXT NOT NULL,        -- Store verse ranges like "1-3", "4a", "4b-5"
    FOREIGN KEY(chapter_id) REFERENCES chapters(chapter_id),
    UNIQUE(chapter_id, paragraph_index)
);

-- Table to store aggregated counts for unique words
CREATE TABLE IF NOT EXISTS word_counts (
    word_key TEXT NOT NULL PRIMARY KEY,
    lemma TEXT,
    normalized_text TEXT,
    count INTEGER NOT NULL
);

-- Materialized statistics tables
CREATE TABLE IF NOT EXISTS book_word_totals (
    book_id INTEGER NOT NULL PRIMARY KEY,
    book_name TEXT NOT NULL,
    total_words INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS book_lemma_stats (
    stat_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL,
    lemma TEXT,
    normalized_text TEXT,
    count INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS pos_lemma_stats (
    stat_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    pos TEXT NOT NULL,
    lemma TEXT,
    normalized_text TEXT,
    count INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS proper_noun_forms (
    form_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    lemma TEXT,
    normalized_text TEXT
);

-- Table to store global statistics such as total word count
CREATE TABLE IF NOT EXISTS global_stats (
    stat_key TEXT NOT NULL PRIMARY KEY,
    stat_value INTEGER NOT NULL
);

-- Table to store metadata about each vocabulary set
CREATE TABLE IF NOT EXISTS vocab_metadata (
    vocab_name TEXT PRIMARY KEY NOT NULL,
    display_name TEXT NOT NULL,
    level_label TEXT NOT NULL,
    min_level INTEGER NOT NULL,
    max_level INTEGER NOT NULL
);

-- --- Indexes for Performance ---
CREATE INDEX IF NOT EXISTS idx_words_verse_id ON words(verse_id);
CREATE INDEX IF NOT EXISTS idx_words_lemma ON words(lemma);
CREATE INDEX IF NOT EXISTS idx_verses_chapter_id ON verses(chapter_id);
CREATE INDEX IF NOT EXISTS idx_chapters_book_id ON chapters(book_id);
CREATE INDEX IF NOT EXISTS idx_paragraphs_chapter_id ON paragraphs(chapter_id);
CREATE INDEX IF NOT EXISTS idx_word_counts_lemma ON word_counts(lemma);
CREATE INDEX IF NOT EXISTS idx_word_counts_normalized ON word_counts(normalized_text);
CREATE INDEX IF NOT EXISTS idx_book_lemma_stats_book_id ON book_lemma_stats(book_id);
CREATE INDEX IF NOT EXISTS idx_book_lemma_stats_lemma ON book_lemma_stats(lemma);
CREATE INDEX IF NOT EXISTS idx_book_lemma_stats_normalized ON book_lemma_stats(normalized_text);
CREATE INDEX IF NOT EXISTS idx_pos_lemma_stats_pos ON pos_lemma_stats(pos);
CREATE INDEX IF NOT EXISTS idx_pos_lemma_stats_lemma ON pos_lemma_stats(lemma);
CREATE INDEX IF NOT EXISTS idx_pos_lemma_stats_normalized ON pos_lemma_stats(normalized_text);
CREATE INDEX IF NOT EXISTS idx_proper_noun_forms_lemma ON proper_noun_forms(lemma);
CREATE INDEX IF NOT EXISTS idx_proper_noun_forms_normalized ON proper_noun_forms(normalized_text);
"""

# --- Helper Functions ---
def normalize_greek_text(text):
    """Removes common Greek punctuation for matching."""
    if not isinstance(text, str):
        return text
    # Simple normalization: remove '.', ',', ';', '·', maybe others?
    punctuation_to_remove = ['.', ',', ';', '·', '’'] # Add more if needed
    for punc in punctuation_to_remove:
        text = text.replace(punc, '')
    return text.strip()

def update_word_counts(aggregated_word_counts, lemma_text, normalized_text, surface_text):
    """Updates the aggregated counts map with the provided word information."""
    lemma_clean = lemma_text.strip() if isinstance(lemma_text, str) and lemma_text.strip() else None
    normalized_clean = normalized_text.strip() if isinstance(normalized_text, str) and normalized_text.strip() else None
    surface_clean = surface_text.strip() if isinstance(surface_text, str) and surface_text.strip() else None

    if lemma_clean:
        word_key = f"lemma::{lemma_clean}"
        normalized_for_storage = normalized_clean or surface_clean
    else:
        normalized_for_key = normalized_clean or surface_clean
        if not normalized_for_key:
            return
        word_key = f"normalized::{normalized_for_key}"
        lemma_clean = None
        normalized_for_storage = normalized_for_key

    entry = aggregated_word_counts.get(word_key)
    if entry:
        entry[2] += 1
    else:
        aggregated_word_counts[word_key] = [lemma_clean, normalized_for_storage, 1]

def refresh_materialized_table(cursor, conn, table_name, insert_sql):
    """Clears and repopulates a materialized statistics table."""
    cursor.execute(f"DELETE FROM {table_name}")
    cursor.execute(insert_sql)
    conn.commit()
    cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
    total_rows = cursor.fetchone()[0]
    print(f"  {table_name}: inserted {total_rows} rows.")

def build_materialized_stats(cursor, conn):
    """Populates the derived statistics tables from the core word data."""
    print("Building materialized statistics tables...")
    refresh_materialized_table(cursor, conn, "book_word_totals", """
        INSERT INTO book_word_totals (book_id, book_name, total_words)
        SELECT b.book_id AS book_id,
               b.name AS book_name,
               COUNT(*) AS total_words
        FROM words w
        JOIN verses v ON w.verse_id = v.verse_id
        JOIN chapters c ON v.chapter_id = c.chapter_id
        JOIN books b ON c.book_id = b.book_id
        GROUP BY b.book_id, b.name
        ORDER BY b.book_id
    """)
    refresh_materialized_table(cursor, conn, "book_lemma_stats", """
        INSERT INTO book_lemma_stats (book_id, lemma, normalized_text, count)
        SELECT b.book_id AS book_id,
               w.lemma AS lemma,
               w.normalized_text AS normalized_text,
               COUNT(*) AS count
        FROM words w
        JOIN verses v ON w.verse_id = v.verse_id
        JOIN chapters c ON v.chapter_id = c.chapter_id
        JOIN books b ON c.book_id = b.book_id
        GROUP BY b.book_id, w.lemma, w.normalized_text
    """)
    refresh_materialized_table(cursor, conn, "pos_lemma_stats", """
        INSERT INTO pos_lemma_stats (pos, lemma, normalized_text, count)
        SELECT SUBSTR(w.pos_tag, 1, 1) AS pos,
               w.lemma AS lemma,
               w.normalized_text AS normalized_text,
               COUNT(*) AS count
        FROM words w
        WHERE w.pos_tag IS NOT NULL AND TRIM(w.pos_tag) <> ''
        GROUP BY pos, w.lemma, w.normalized_text
    """)
    refresh_materialized_table(cursor, conn, "proper_noun_forms", """
        INSERT INTO proper_noun_forms (lemma, normalized_text)
        SELECT DISTINCT
            NULLIF(TRIM(w.lemma), '') AS lemma,
            NULLIF(TRIM(COALESCE(w.normalized_text, w.text)), '') AS normalized_text
        FROM words w
        WHERE w.type IS NOT NULL
          AND LOWER(w.type) = 'proper'
          AND TRIM(COALESCE(w.lemma, w.normalized_text, w.text)) <> ''
    """)

# --- Main Processing Logic ---
def create_database():
    print(f"Database will be created at: {DATABASE_OUTPUT_PATH}")
    print(f"Corpus source: {CORPUS_ROOT}")

    # Delete existing database file if it exists
    if DATABASE_OUTPUT_PATH.exists():
        print("Deleting existing database file...")
        DATABASE_OUTPUT_PATH.unlink()

    conn = sqlite3.connect(DATABASE_OUTPUT_PATH)
    cursor = conn.cursor()

    try:
        print("Creating base database schema (books, chapters, verses, words, paragraphs)...")
        cursor.executescript(BASE_SCHEMA)
        conn.commit()

        # Process books.json.
        print("Processing books...")
        with open(BOOKS_LIST_PATH, 'r', encoding='utf-8') as f:
            book_names = json.load(f)
        books_to_insert = [(name, name) for name in book_names] # Using filename as osis_name initially
        cursor.executemany("INSERT INTO books (name, osis_name) VALUES (?, ?)", books_to_insert)
        conn.commit()
        print(f"Inserted {len(book_names)} books.")

        # Create a mapping from book name to book_id
        cursor.execute("SELECT osis_name, book_id FROM books")
        book_name_to_id = {row[0]: row[1] for row in cursor.fetchall()}
        print("Book name to ID mapping created.")


        # Process each book.
        print("Processing book chapters, verses, and words...")
        total_chapters = 0
        total_verses = 0
        total_words = 0
        aggregated_word_counts = {}
        for book_name, book_id in book_name_to_id.items():
            book_file_path = JSON_DATA_PATH / f"{book_name}.json"
            if not book_file_path.exists():
                print(f"Warning: JSON file not found for book {book_name}, skipping.")
                continue

            print(f"  Processing book: {book_name}...")
            with open(book_file_path, 'r', encoding='utf-8') as f:
                book_data = json.load(f) # Expecting format: { "BookName": { "ChapterNum": { "VerseNum": [WordObject, ...], ... }, ... }, ... }

            # Access the actual chapter data, which is nested under the book name key
            if book_name not in book_data:
                print(f"    Error: Book name key '{book_name}' not found in {book_file_path}. Skipping book.")
                continue
            book_chapters_data = book_data[book_name]

            chapters_to_insert = []
            verses_to_insert = []

            for chapter_num_str, chapter_data in book_chapters_data.items():
                try:
                    chapter_number = int(chapter_num_str)
                except ValueError:
                    print(f"    Warning: Invalid chapter number '{chapter_num_str}' in {book_name}, skipping.")
                    continue

                chapters_to_insert.append((book_id, chapter_number))

            # Insert chapters and get their IDs
            cursor.executemany("INSERT OR IGNORE INTO chapters (book_id, chapter_number) VALUES (?, ?)", chapters_to_insert)
            conn.commit() # Commit chapter inserts
            total_chapters += len(chapters_to_insert) # Rough count, duplicates ignored

            # Get chapter_id mapping for the current book AFTER inserting
            cursor.execute("SELECT chapter_number, chapter_id FROM chapters WHERE book_id = ?", (book_id,))
            chapter_num_to_id = {row[0]: row[1] for row in cursor.fetchall()}

            # Prepare and insert verses
            batch_verses = []
            verse_lookup_for_words = {} # Store chapter_num, verse_num -> words list
            for chapter_num_str, chapter_data in book_chapters_data.items():
                 try:
                    chapter_num = int(chapter_num_str)
                    chapter_id = chapter_num_to_id.get(chapter_num)
                    if chapter_id is None: continue # Skip if chapter wasn't inserted/found
                    for verse_num_str, verse_words in chapter_data.items():
                        batch_verses.append((chapter_id, verse_num_str))
                        verse_lookup_for_words[(chapter_num, verse_num_str)] = verse_words
                 except ValueError:
                     continue # Already warned

            if batch_verses:
                cursor.executemany("INSERT OR IGNORE INTO verses (chapter_id, verse_number) VALUES (?, ?)", batch_verses)
                conn.commit() # Commit verse inserts
                total_verses += len(batch_verses) # Rough count

            # Get verse_id mapping for the current book's chapters AFTER inserting
            cursor.execute("""
                SELECT c.chapter_number, v.verse_number, v.verse_id
                FROM verses v JOIN chapters c ON v.chapter_id = c.chapter_id
                WHERE c.book_id = ?
            """, (book_id,))
            verse_key_to_id = {(row[0], row[1]): row[2] for row in cursor.fetchall()} # {(chapter_num, verse_num): verse_id}

            # Prepare and insert words.
            batch_words = []
            for (chapter_num, verse_num), verse_words in verse_lookup_for_words.items():
                 verse_id = verse_key_to_id.get((chapter_num, verse_num))
                 if verse_id is None:
                     print(f"    Error: Could not find verse_id for {book_name} {chapter_num}:{verse_num}. Skipping words.")
                     continue

                 for word_index, word_obj in enumerate(verse_words):
                     if not isinstance(word_obj, dict):
                         print(f"    Warning: Expected dict for word object, got {type(word_obj)} in {book_name} {chapter_num}:{verse_num}. Skipping word.")
                         continue

                     word_forms_list = word_obj.get('word_forms', [])
                     word_text = word_forms_list[0] if len(word_forms_list) > 0 else ''
                     lemma_text = word_obj.get('word_forms', [None, None, None, None])[3] if len(word_obj.get('word_forms', [])) > 3 else None
                     normalized = normalize_greek_text(word_text)

                     batch_words.append((
                         verse_id,
                         word_index,
                         word_text,
                         normalized,
                         word_obj.get('pos_tag'),
                         word_obj.get('gloss'),
                         lemma_text,
                         word_obj.get('literal'),
                         word_obj.get('louw'),
                         word_obj.get('strong'),
                         word_obj.get('type')
                     ))
                     update_word_counts(aggregated_word_counts, lemma_text, normalized, word_text)

            if batch_words:
                cursor.executemany("""
                    INSERT OR IGNORE INTO words (verse_id, word_index, text, normalized_text, pos_tag, gloss, lemma, literal, louw, strong, type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batch_words)
                conn.commit() # Commit word inserts
                total_words += len(batch_words) # Rough count
            print(f"    Finished {book_name}. Processed chapters, verses, words.")


        # Process paragraphs.
        print("Processing paragraph data...")
        total_paragraphs = 0
        for book_name, book_id in book_name_to_id.items():
            book_para_path = PARAGRAPHS_ROOT_PATH / book_name
            if not book_para_path.is_dir():
                print(f"  Warning: Paragraph directory not found for book {book_name}, skipping.")
                continue

            print(f"  Processing paragraphs for: {book_name}...")
            paragraphs_to_insert = []

            # Get chapter IDs for this book again
            cursor.execute("SELECT chapter_number, chapter_id FROM chapters WHERE book_id = ?", (book_id,))
            chapter_num_to_id_para = {row[0]: row[1] for row in cursor.fetchall()}

            for para_file in sorted(book_para_path.glob('*.json')): # Ensure chapter order if possible
                 try:
                    # Extract chapter number from filename like "001-paragraphs.json"
                    chapter_num_str = para_file.stem.split('-')[0]
                    chapter_number = int(chapter_num_str)
                 except (ValueError, IndexError):
                    print(f"    Warning: Could not parse chapter number from filename {para_file.name}. Skipping.")
                    continue

                 chapter_id = chapter_num_to_id_para.get(chapter_number)
                 if chapter_id is None:
                     print(f"    Warning: Could not find chapter_id for {book_name} chapter {chapter_number} (paragraphs). Skipping.")
                     continue

                 with open(para_file, 'r', encoding='utf-8') as f:
                    try:
                        para_data = json.load(f) # Expecting a list of paragraph definitions
                        if not isinstance(para_data, list):
                             print(f"    Warning: Expected list in paragraph file {para_file.name}, got {type(para_data)}. Skipping.")
                             continue
                    except json.JSONDecodeError:
                        print(f"    Warning: Could not decode JSON from {para_file.name}. Skipping.")
                        continue

                 for para_index, para_span_info in enumerate(para_data):
                     verse_spans_text = json.dumps(para_span_info)
                     paragraphs_to_insert.append((
                         chapter_id,
                         para_index,
                         verse_spans_text
                     ))

            if paragraphs_to_insert:
                cursor.executemany("""
                    INSERT OR IGNORE INTO paragraphs (chapter_id, paragraph_index, verse_spans)
                    VALUES (?, ?, ?)
                """, paragraphs_to_insert)
                conn.commit()
                total_paragraphs += len(paragraphs_to_insert)
                print(f"    Finished {book_name}. Inserted {len(paragraphs_to_insert)} paragraph entries.")

        # 5. Persist aggregated word statistics
        print("Persisting aggregated word statistics...")
        if aggregated_word_counts:
            word_count_records = [
                (word_key, values[0], values[1], values[2])
                for word_key, values in aggregated_word_counts.items()
            ]
            cursor.executemany("""
                INSERT OR REPLACE INTO word_counts (word_key, lemma, normalized_text, count)
                VALUES (?, ?, ?, ?)
            """, word_count_records)
            conn.commit()
            print(f"  Stored {len(word_count_records)} aggregated word entries.")
        else:
            print("  No aggregated word statistics to store.")

        cursor.execute("""
            INSERT OR REPLACE INTO global_stats (stat_key, stat_value)
            VALUES (?, ?)
        """, ("total_word_count", total_words))
        conn.commit()
        print(f"  Stored total word count statistic: {total_words}")

        build_materialized_stats(cursor, conn)

        print("\n--- Processing Summary ---")
        print(f"Total books: {len(book_name_to_id)}")
        # Totals might be slightly off if INSERT OR IGNORE skipped items on rerun
        print(f"Total chapters processed: {total_chapters}")
        print(f"Total verses processed: {total_verses}")
        print(f"Total words processed: {total_words}")
        print(f"Total paragraphs processed: {total_paragraphs}")
        print(f"Unique word count entries stored: {len(aggregated_word_counts)}")
        print("--------------------------\n")

        print("Database creation/update successful!")

    except sqlite3.Error as e:
        print(f"An error occurred: {e}")
        conn.rollback() # Rollback changes on error
        raise
    except FileNotFoundError as e:
        print(f"Error: Required file not found: {e}. Make sure source files exist.")
        raise
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        conn.rollback()
        raise
    finally:
        if conn:
            conn.close()
            print("Database connection closed.")

# --- Execution ---
if __name__ == "__main__":
    # Create the directory for the script if it doesn't exist
    CORPUS_ROOT.mkdir(parents=True, exist_ok=True)
    print(f"Corpus directory: {CORPUS_ROOT}")

    # Check the corpus input files.
    if not BOOKS_LIST_PATH.exists() or not JSON_DATA_PATH.is_dir() or not PARAGRAPHS_ROOT_PATH.is_dir():
         print("Error: One or more source data paths not found (books, text json, paragraphs). Please check configuration and file locations.")
         print(f"  BOOKS_LIST_PATH: {BOOKS_LIST_PATH} (Exists: {BOOKS_LIST_PATH.exists()})")
         print(f"  JSON_DATA_PATH: {JSON_DATA_PATH} (Exists: {JSON_DATA_PATH.is_dir()})")
         print(f"  PARAGRAPHS_ROOT_PATH: {PARAGRAPHS_ROOT_PATH} (Exists: {PARAGRAPHS_ROOT_PATH.is_dir()})")
         raise SystemExit(1)
    else:
        create_database() 
