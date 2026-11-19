package com.fatflow.app.data.repo

import com.fatflow.app.domain.ParsedInput

interface FoodSearchRepository {
    suspend fun search(raw: String): FoodSearchResult

    suspend fun product(barcode: String): CachedFood?

    suspend fun savePreset(
        name: String,
        grams: Double,
        kcal: Int,
        protein: Float,
        carb: Float,
        fat: Float,
    )
}

data class CachedFood(
    val name: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbPer100g: Float,
    val fatPer100g: Float,
    val barcode: String?,
    val source: String,
)

data class FoodSearchResult(
    val foods: List<CachedFood>,
    val parsed: ParsedInput,
    val error: String?,
)