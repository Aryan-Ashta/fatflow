package com.fatflow.app.data.repo

import com.fatflow.app.data.local.ProfileStore
import com.fatflow.app.data.local.dao.FoodCacheDao
import com.fatflow.app.data.local.entity.FoodCacheEntity
import com.fatflow.app.domain.MealQueryParser
import com.fatflow.app.domain.ParsedInput
import java.net.URLEncoder
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.roundToInt

class FoodSearchRepositoryImpl(
    private val cacheDao: FoodCacheDao,
    private val profileStore: ProfileStore,
    private val client: OkHttpClient,
) : FoodSearchRepository {
  override suspend fun search(raw: String): FoodSearchResult {
        val parsed = MealQueryParser.parse(raw)
        if (parsed is ParsedInput.Empty) {
            return FoodSearchResult(emptyList(), parsed, null)
        }
        if (parsed is ParsedInput.Barcode) {
            val product = product(parsed.code)
            return FoodSearchResult(
                product?.let { listOf(it) } ?: emptyList(),
                parsed,
                if (product == null) "No food found for that barcode." else null,
            )
        }
        val query = (parsed as ParsedInput.Food).query
        val local = cacheDao.searchName(query).map { it.toCached() }
        if (local.isNotEmpty()) return FoodSearchResult(local, parsed, null)
        return try {
            val off = searchOff(query)
            val profile = profileStore.profile.first()
            val usda =
                if (profile.usdaApiKey.isNotBlank()) {
                    searchUsda(query, profile.usdaApiKey)
                } else {
                    emptyList()
                }
            val combined = (local + off + usda).distinctBy { it.name }
            if (combined.isEmpty()) {
                FoodSearchResult(emptyList(), parsed, "No results.")
            } else {
                FoodSearchResult(combined, parsed, null)
            }
        } catch (_: Exception) {
            FoodSearchResult(
                local,
                parsed,
                if (local.isEmpty()) "Offline. Try a saved food or enter macros manually." else null,
            )
        }
    }

    override suspend fun product(barcode: String): CachedFood? {
        cacheDao.byBarcode(barcode)?.toCached()?.let { return it }
        val off = fetchOffBarcode(barcode) ?: return null
        cacheDao.insert(off)
        return off.toCached()
    }

    override suspend fun savePreset(
        name: String,
        grams: Double,
        kcal: Int,
        protein: Float,
        carb: Float,
        fat: Float,
    ) {
        val scale = (100.0 / grams).toFloat()
        cacheDao.insert(
            FoodCacheEntity(
                name = name,
                source = "CUSTOM",
                barcode = null,
                kcalPer100g = kcal * scale,
                proteinPer100g = protein * scale,
                carbPer100g = carb * scale,
                fatPer100g = fat * scale,
            ),
        )
    }

    private suspend fun searchOff(query: String): List<CachedFood> {
        val enc = URLEncoder.encode(query, Charsets.UTF_8)
        val req =
            Request.Builder()
                .url(
                    "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$enc&search_simple=1&action=process&json=1&page_size=8",
                )
                .header("User-Agent", "FatFlow/1.0 (android)")
                .build()
        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body?.string().orEmpty())
            val products = json.optJSONArray("products") ?: return emptyList()
            return (0 until products.length()).mapNotNull { i ->
                val p = products.getJSONObject(i)
                val kcal = p.optJSONObject("nutriments")?.optDouble("energy-kcal_100g") ?: return@mapNotNull null
                val entity =
                    FoodCacheEntity(
                        barcode = p.optString("code").takeIf { it.isNotBlank() },
                        source = "OFF",
                        name = p.optString("product_name", "Unknown"),
                        kcalPer100g = kcal.toFloat(),
                        proteinPer100g = p.optJSONObject("nutriments")?.optDouble("proteins_100g")?.toFloat() ?: 0f,
                        carbPer100g = p.optJSONObject("nutriments")?.optDouble("carbohydrates_100g")?.toFloat() ?: 0f,
                        fatPer100g = p.optJSONObject("nutriments")?.optDouble("fat_100g")?.toFloat() ?: 0f,
                    )
                cacheDao.insert(entity)
                entity.toCached()
            }
        }
    }

    private suspend fun fetchOffBarcode(barcode: String): FoodCacheEntity? {
        val req =
            Request.Builder()
                .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
                .header("User-Agent", "FatFlow/1.0 (android)")
                .build()
        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body?.string().orEmpty())
            if (json.optInt("status") != 1) return null
            val p = json.getJSONObject("product")
            val n = p.getJSONObject("nutriments")
            val kcal = n.optDouble("energy-kcal_100g", Double.NaN)
            if (kcal.isNaN()) return null
            return FoodCacheEntity(
                barcode = barcode,
                source = "OFF",
                name = p.optString("product_name", "Unknown"),
                kcalPer100g = kcal.toFloat(),
                proteinPer100g = n.optDouble("proteins_100g", 0.0).toFloat(),
                carbPer100g = n.optDouble("carbohydrates_100g", 0.0).toFloat(),
                fatPer100g = n.optDouble("fat_100g", 0.0).toFloat(),
            )
        }
    }

    private suspend fun searchUsda(query: String, apiKey: String): List<CachedFood> {
        val enc = URLEncoder.encode(query, Charsets.UTF_8)
        val req =
            Request.Builder()
                .url(
                    "https://api.nal.usda.gov/fdc/v1/foods/search?api_key=$apiKey&query=$enc&pageSize=8",
                )
                .build()
        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body?.string().orEmpty())
            val foods = json.optJSONArray("foods") ?: return emptyList()
            return (0 until foods.length()).mapNotNull { i ->
                val food = foods.getJSONObject(i)
                val nutrients = food.optJSONArray("foodNutrients") ?: return@mapNotNull null
                fun nutrient(id: Int): Double? {
                    for (j in 0 until nutrients.length()) {
                        val n = nutrients.getJSONObject(j)
                        if (n.optInt("nutrientId") == id || n.optInt("number") == id) {
                            return n.optDouble("value")
                        }
                    }
                    return null
                }
                val kcal = nutrient(1008) ?: return@mapNotNull null
                var protein = nutrient(1003) ?: 0.0
                var carb = nutrient(1005) ?: 0.0
                var fat = nutrient(1004) ?: 0.0
                val serving = food.optDouble("servingSize", Double.NaN)
                if (!serving.isNaN() && food.optString("servingSizeUnit").equals("g", true) && serving != 100.0) {
                    val scale = 100.0 / serving
                    protein *= scale
                    carb *= scale
                    fat *= scale
                }
                val entity =
                    FoodCacheEntity(
                        barcode = null,
                        source = "USDA",
                        name = food.optString("description", "Food"),
                        kcalPer100g = kcal.toFloat(),
                        proteinPer100g = protein.toFloat(),
                        carbPer100g = carb.toFloat(),
                        fatPer100g = fat.toFloat(),
                    )
                cacheDao.insert(entity)
                entity.toCached()
            }
        }
    }
}

private fun FoodCacheEntity.toCached() =
    CachedFood(name, kcalPer100g, proteinPer100g, carbPer100g, fatPer100g, barcode, source)