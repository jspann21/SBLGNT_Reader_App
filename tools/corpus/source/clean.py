import os
import json
import re

# Define regex to match numbers within strings
number_regex = re.compile(r"\d+")

# Create a "clean" directory
clean_dir = "clean"
os.makedirs(clean_dir, exist_ok=True)


# Function to clean word_forms in JSON data
def clean_word_forms(data):
    if isinstance(data, dict):
        # If it's a dictionary, check keys and recursively process values
        for key, value in data.items():
            if key == "word_forms" and isinstance(value, list):
                # Clean numbers within word_forms lists
                data[key] = [number_regex.sub("", item) for item in value]
            else:
                # Recursively process other items
                clean_word_forms(value)
    elif isinstance(data, list):
        # If it's a list, recursively process each item
        for item in data:
            clean_word_forms(item)


# Iterate through JSON files in the current directory
for file_name in os.listdir("."):
    if file_name.endswith(".json"):
        with open(file_name, 'r', encoding='utf-8') as file:
            data = json.load(file)

        # Clean the data
        clean_word_forms(data)

        # Save the cleaned data to the "clean" directory
        clean_path = os.path.join(clean_dir, file_name)
        with open(clean_path, 'w', encoding='utf-8') as clean_file:
            json.dump(data, clean_file, ensure_ascii=False, indent=4)

print(f"Cleaning complete. Cleaned files are saved in the '{clean_dir}' directory.")
