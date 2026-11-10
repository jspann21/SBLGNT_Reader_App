package com.lexapps.sblgntreader.ui.util

internal fun greekBookName(english: String): String {
    val map = mapOf(
        "Matthew" to "ΚΑΤΑ ΜΑΘΘΑΙΟΝ",
        "Mark" to "ΚΑΤΑ ΜΑΡΚΟΝ",
        "Luke" to "ΚΑΤΑ ΛΟΥΚΑΝ",
        "John" to "ΚΑΤΑ ΙΩΑΝΝΗΝ",
        "Acts" to "ΠΡΑΞΕΙΣ ΑΠΟΣΤΟΛΩΝ",
        "Romans" to "ΠΡΟΣ ΡΩΜΑΙΟΥΣ",
        "1Corinthians" to "ΠΡΟΣ ΚΟΡΙΝΘΙΟΥΣ Α",
        "2Corinthians" to "ΠΡΟΣ ΚΟΡΙΝΘΙΟΥΣ Β",
        "Galatians" to "ΠΡΟΣ ΓΑΛΑΤΑΣ",
        "Ephesians" to "ΠΡΟΣ ΕΦΕΣΙΟΥΣ",
        "Philippians" to "ΠΡΟΣ ΦΙΛΙΠΠΗΣΙΟΥΣ",
        "Colossians" to "ΠΡΟΣ ΚΟΛΟΣΣΑΕΙΣ",
        "1Thessalonians" to "ΠΡΟΣ ΘΕΣΣΑΛΟΝΙΚΕΙΣ Α",
        "2Thessalonians" to "ΠΡΟΣ ΘΕΣΣΑΛΟΝΙΚΕΙΣ Β",
        "1Timothy" to "ΠΡΟΣ ΤΙΜΟΘΕΟΝ Α",
        "2Timothy" to "ΠΡΟΣ ΤΙΜΟΘΕΟΝ Β",
        "Titus" to "ΠΡΟΣ ΤΙΤΟΝ",
        "Philemon" to "ΠΡΟΣ ΦΙΛΗΜΟΝΑ",
        "Hebrews" to "ΠΡΟΣ ΕΒΡΑΙΟΥΣ",
        "James" to "ΙΑΚΩΒΟΥ",
        "1Peter" to "ΠΕΤΡΟΥ Α",
        "2Peter" to "ΠΕΤΡΟΥ Β",
        "1John" to "ΙΩΑΝΝΟΥ Α",
        "2John" to "ΙΩΑΝΝΟΥ Β",
        "3John" to "ΙΩΑΝΝΟΥ Γ",
        "Jude" to "ΙΟΥΔΑ",
        "Revelation" to "ΑΠΟΚΑΛΥΨΙΣ ΙΩΑΝΝΟΥ"
    )
    return map[english] ?: english
}
