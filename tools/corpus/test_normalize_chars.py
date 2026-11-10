import pytest
from tools.corpus.normalize_chars import has_unwanted_combining_chars

def test_no_combining_chars():
    # ASCII characters
    assert has_unwanted_combining_chars("abc") is False
    assert has_unwanted_combining_chars("123") is False
    assert has_unwanted_combining_chars("Hello World!") is False

    # Precomposed Greek characters (NFC)
    # ά is U+1F71 GREEK SMALL LETTER ALPHA WITH OXIA
    assert has_unwanted_combining_chars("ά") is False
    assert has_unwanted_combining_chars("λόγος") is False

def test_allowed_combining_char():
    # \u0345 is COMBINING GREEK YPOGEGRAMMENI, which should be allowed
    assert has_unwanted_combining_chars("\u0345") is False
    assert has_unwanted_combining_chars("α\u0345") is False # α + ypogegrammeni

def test_unwanted_combining_chars():
    # Range \u0300 to \u036F excluding \u0345
    assert has_unwanted_combining_chars("\u0300") is True # COMBINING GRAVE ACCENT
    assert has_unwanted_combining_chars("\u0301") is True # COMBINING ACUTE ACCENT
    assert has_unwanted_combining_chars("\u036F") is True # COMBINING LATIN SMALL LETTER X
    assert has_unwanted_combining_chars("a\u0301") is True # a + acute accent

def test_mixed_chars():
    # Mixture of allowed and unwanted
    assert has_unwanted_combining_chars("\u0345\u0301") is True
    assert has_unwanted_combining_chars("α\u0345\u0301") is True

def test_edge_cases():
    # Empty string
    assert has_unwanted_combining_chars("") is False

    # Just outside the range
    assert has_unwanted_combining_chars("\u02FF") is False
    assert has_unwanted_combining_chars("\u0370") is False

    # Range boundaries
    assert has_unwanted_combining_chars("\u0300") is True
    assert has_unwanted_combining_chars("\u036F") is True
