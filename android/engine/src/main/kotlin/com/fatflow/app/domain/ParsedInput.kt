package com.fatflow.app.domain

sealed interface ParsedInput {
    data class Barcode(val code: String) : ParsedInput
    data class Food(val query: String, val grams: Double?) : ParsedInput
    data object Empty : ParsedInput
}