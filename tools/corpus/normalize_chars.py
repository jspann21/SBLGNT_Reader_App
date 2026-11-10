import os
import json
import unicodedata
from collections import defaultdict
import re

# Mapping from ancient polytonic → modern monotonic
char_map = {
    'ά': 'ά',
    'έ': 'έ',
    'ή': 'ή',
    'ί': 'ί',
    'ό': 'ό',
    'ύ': 'ύ',
    'ώ': 'ώ',
    # Uppercase not included to preserve breathing marks
}

# Manual check for decomposed combining marks, excluding U+0345 (ypogegrammeni)
def has_unwanted_combining_chars(text):
    return any(c != '\u0345' and '\u0300' <= c <= '\u036F' for c in text)

if __name__ == "__main__":
    # Directory setup
    script_dir = os.path.dirname(os.path.abspath(__file__))
    source_dir = script_dir
    output_dir = os.path.join(script_dir, 'output')
    os.makedirs(output_dir, exist_ok=True)

    print(f"Searching for JSON files in: {source_dir}")
    print(f"Outputting modified files to: {output_dir}")

    for filename in os.listdir(source_dir):
        if filename.lower().endswith('.json'):
            input_filepath = os.path.join(source_dir, filename)
            output_filepath = os.path.join(output_dir, filename)
            log_filepath = os.path.join(output_dir, filename + '.log')

            print(f"Processing {filename}...")

            try:
                with open(input_filepath, 'r', encoding='utf-8') as f_in:
                    content = f_in.read()

                # Normalize to NFC
                normalized_content = unicodedata.normalize('NFC', content)

                # Check for unwanted combining characters
                decomposed_found = has_unwanted_combining_chars(normalized_content)

                # Replacement and logging
                modified_content_str = normalized_content
                replacement_log = defaultdict(int)

                for ancient, modern in char_map.items():
                    count = modified_content_str.count(ancient)
                    if count > 0:
                        replacement_log[f"'{ancient}' → '{modern}'"] += count
                        modified_content_str = modified_content_str.replace(ancient, modern)

                # Convert and normalize the final JSON string before writing
                modified_data = json.loads(modified_content_str)
                json_str = json.dumps(modified_data, ensure_ascii=False, indent=4)
                json_str = unicodedata.normalize('NFC', json_str)

                with open(output_filepath, 'w', encoding='utf-8') as f_out:
                    f_out.write(json_str)

                # Write log file
                with open(log_filepath, 'w', encoding='utf-8') as log_file:
                    if decomposed_found:
                        log_file.write("⚠️ Found decomposed Unicode characters (combining accents) before normalization.\n\n")
                    if replacement_log:
                        log_file.write("🔄 Replacements made:\n")
                        for change, count in sorted(replacement_log.items()):
                            log_file.write(f"  {change}: {count} times\n")
                    if not decomposed_found and not replacement_log:
                        log_file.write("✅ No replacements were needed.\n")

                print(f"Finished {filename} — replacements: {sum(replacement_log.values())}, log saved.")

            except json.JSONDecodeError as e:
                print(f"Error decoding JSON from {filename}: {e}")
            except Exception as e:
                print(f"An error occurred while processing {filename}: {e}")

    print("Script finished.")
