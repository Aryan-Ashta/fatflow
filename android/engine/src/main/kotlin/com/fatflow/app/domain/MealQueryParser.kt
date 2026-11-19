package com.fatflow.app.domain

object MealQueryParser {
    private val barcodePattern = Regex("^\\d{8,14}$")
    private val quantityPattern =
        Regex("^(\\d+(?:\\.\\d+)?)\\s*(g|kg|oz|lb|ml)?\\s+(.+)$", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedInput {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ParsedInput.Empty
        if (barcodePattern.matches(trimmed)) return ParsedInput.Barcode(trimmed)

        val match = quantityPattern.find(trimmed) ?: return ParsedInput.Food(trimmed, null)
        val value = match.groupValues[1].toDouble()
        val unit = match.groupValues[2]
        val query = match.groupValues[3].trim()
        val grams = when (unit.lowercase()) {
            "g", "ml", "" -> value
            "kg" -> value * 1000.0
            "oz" -> value * 28.3495
            "lb" -> value * 453.592
            else -> value
        }
        return ParsedInput.Food(query, grams)
    }
}