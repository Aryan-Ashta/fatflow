# Product Specification: **FatFlow** (Android App)

**FatFlow** is a native Android nutrition and metabolic-pacing application that dynamically adjusts your daily calorie deficit based on your body fat’s maximum energy release rate (Seymour Alpert’s model) while cycling carbohydrates around your resistance training and tennis schedule.

---

## 1. Core Metabolic & Carb-Cycling Engine

The app computes daily targets every morning using your latest rolling weight and body fat percentage, combined with your scheduled training load.

### A. Variable Deficit Calculation

1. **Lean Body Mass (LBM) & Fat Mass (FM):**
   $$\text{FM} = \text{Weight} \times \left(\frac{\text{BodyFat}\%}{100}\right), \quad \text{LBM} = \text{Weight} - \text{FM}$$

2. **Maximum Fat Energy Release ($\text{Deficit}_{\max}$):**
   $$\text{Deficit}_{\max} = \text{FM}_{\text{lbs}} \times R_{\text{release}}$$
   *Default $R_{\text{release}} = 25\text{ kcal/lb/day}$ (user-configurable slider from $22$ to $31\text{ kcal/lb/day}$).*

3. **Adaptive TDEE & Safe Calorie Floor:**
   * **Adaptive TDEE:** Uses an exponential moving average (EMA) of scale weight vs. logged intake over a 14-day window to update TDEE automatically.
   * **Daily Calorie Target ($\text{Cal}_{\text{target}}$):**
     $$\text{Cal}_{\text{target}} = \max\Big(\text{SafeFloor},\; \text{TDEE}_{\text{day}} - \text{Deficit}_{\max}\Big)$$
   * **SafeFloor:** Defaults to $\max(1500,\; 11 \times \text{LBM}_{\text{lbs}})$ (e.g., **~1,575–1,600 kcal** at $143.4\text{ lbs}$ LBM) so court endurance and recovery never crash during initial high-fat phases.

### B. Daily Carb-Cycling Allocation

Rather than eating the same macros every day, the app holds **Weekly Average Calories** equal to $\text{Cal}_{\text{target}} \times 7$, shifting carbs across three day types based on your workout schedule:

| Day Type | Example Schedule (6x/wk) | Daily Calorie Multiplier | Protein | Fat | Carbs |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **High-Carb (Performance)** | Tennis Match / Heavy Full-Body Lift (3 days) | **+15% to +20%** above baseline | Fixed: $1.05\text{g} / \text{lb LBM}$ (~150g) | Low: $0.30\text{g} / \text{lb LBM}$ (~43g) | **Remainder** (~215–250g) |
| **Moderate-Carb (Training)** | Standard Resistance Training (2–3 days) | **Baseline (100%)** | Fixed: $1.05\text{g} / \text{lb LBM}$ (~150g) | Moderate: $0.38\text{g} / \text{lb LBM}$ (~55g) | **Remainder** (~135–160g) |
| **Low-Carb (Rest / Recovery)** | Rest Day / Light Mobility (1–2 days) | **-15% to -20%** below baseline | Fixed: $1.05\text{g} / \text{lb LBM}$ (~150g) | Higher: $0.48\text{g} / \text{lb LBM}$ (~69g) | **Remainder** (~50–75g) |

* **Auto-Escalation Rule:** Every time your 7-day rolling Fat Mass drops by **2.0 lbs**, the app automatically raises your weekly calorie budget by **$2.0 \times R_{\text{release}}$ (~50 kcal/day)** and routes 100% of those added calories into your **High-Carb** and **Moderate-Carb** days.

### C. Example Progression Baseline (210 lbs @ 31.7% BF $\rightarrow$ 165 lbs)

| Phase | Target Weight | Body Fat % | Remaining Fat Mass | Max Fat Release (25 kcal/lb) | Est. TDEE | Daily Calorie Target | Est. Weekly Loss |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Phase 1 (Start)** | 210 lbs | 31.7% | 66.6 lbs | ~1,665 kcal/day | ~3,050 kcal | **1,600 – 1,750 kcal** | ~2.7 – 2.9 lbs/wk |
| **Phase 2** | 195 lbs | 26.5% | 51.6 lbs | ~1,290 kcal/day | ~2,925 kcal | **1,650 – 1,750 kcal** | ~2.4 – 2.6 lbs/wk |
| **Phase 3** | 182 lbs | 21.2% | 38.6 lbs | ~965 kcal/day | ~2,800 kcal | **1,800 – 1,900 kcal** | ~1.8 – 2.0 lbs/wk |
| **Phase 4** | 172 lbs | 16.6% | 28.6 lbs | ~715 kcal/day | ~2,700 kcal | **1,950 – 2,050 kcal** | ~1.3 – 1.5 lbs/wk |
| **Phase 5 (Finish)** | 165 lbs | 13.1% | 21.6 lbs | ~540 kcal/day | ~2,625 kcal | **2,050 – 2,150 kcal** | ~1.0 – 1.1 lbs/wk |

---

## 2. Core Features & User Flows

### Screen 1: Today Dashboard (Home)
* **Fat-Release Gauge:** Visual progress ring showing **Current Deficit vs. Max Fat Release Ceiling** (e.g., *"Deficit: 1,350 kcal / Max Fat Supply: 1,665 kcal — Muscle Safe"*).
* **Day-Type Switcher Pill:** Displays today’s auto-assigned mode (**High Carb – Tennis Day**, **Mod Carb – Lifting Day**, or **Low Carb – Rest**) with a 1-tap override if you reschedule a match or workout.
* **Macro & Pre-Workout Carb Timer:**
  * Remaining Protein / Carbs / Fat progress bars.
  * **Court/Gym Fuel Prompt:** Highlights how many grams of carbs to eat 60–90 minutes before your scheduled tennis session or resistance workout.
* **Quick-Log Bar:** Barcode scanner, natural-language meal entry, and *"Repeat Yesterday's Pre-Tennis Meal"* shortcut.

### Screen 2: Weekly Carb-Cycle & Workout Planner
* **7-Day Drag-and-Drop Grid:** Assign **Tennis**, **Lifting**, **Double Session**, or **Rest** to each day of the week.
* **Weekly Energy Balance Bar:** Ensures that high-carb surpluses on tennis days are mathematically offset by low-carb rest days so your 7-day net fat loss stays locked to your target.
* **Diet Break Scheduler:** Automatically flags a **7-day Maintenance Refeed** every ~20–28 lbs lost (or ~10% body fat drop) to restore glycogen, support leptin, and sustain athletic performance.

### Screen 3: Metabolic Trajectory & Phase Roadmap
* **Interactive Phase Chart:** Plots your starting point down to your goal weight, displaying:
  1. Projected Weight & Body Fat % curve.
  2. Rising Daily Calorie Floor curve as fat mass shrinks.
  3. Estimated weeks remaining at your current adherence rate.
* **Weekly Check-In Modal:** Prompts for morning weight, optional smart-scale BF% or waist measurement, and subjective tennis/gym energy score (1–5).

---

## 3. Android Architecture & Tech Stack

| Layer | Technology / API | Purpose |
| :--- | :--- | :--- |
| **Language & UI** | **Kotlin** + **Jetpack Compose** (Material 3) | Smooth, reactive sliders, charts (Vico / Compose Canvas), and dynamic theming. |
| **Local Storage** | **Room (SQLite)** + **DataStore** | Offline-first food logs, weight trends, and metabolic state snapshots. |
| **Health Sync** | **Android Health Connect API** (`WeightRecord`, `BodyFatRecord`, `ExerciseSessionRecord`, `NutritionRecord`) | Automatically pulls morning smart-scale weight/BF% and tennis/workout sessions from Wear OS / Garmin / Samsung Health, and writes logged macros back. |
| **Food Database** | **Open Food Facts API** + **USDA FoodData Central** (cached locally) | Fast, zero-subscription barcode scanning and verified whole-food macros. |
| **Background Jobs** | **WorkManager** | Runs nightly TDEE recalculation, updates morning calorie targets, and triggers pre-tennis carb notifications. |
| **Home Screen Widget** | **Jetpack Glance** | 2x2 and 4x2 Android widgets showing today's Carb-Cycle Day Type, remaining macros, and 1-tap barcode log. |

---

## 4. Data Schema (Room Entities)

```kotlin
package com.fatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class DayType {
    HIGH_CARB,
    MOD_CARB,
    LOW_CARB
}

@Entity(tableName = "daily_targets")
data class DailyTargetEntity(
    @PrimaryKey val date: LocalDate,
    val dayType: DayType,
    val estimatedTdee: Int,
    val maxFatReleaseKcal: Int,        // fatMassLbs * releaseRate (22..31)
    val targetCalories: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val isDietBreak: Boolean = false
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val date: LocalDate,
    val rawWeightLbs: Float,
    val emaWeightLbs: Float,           // 14-day exponential moving average
    val bodyFatPct: Float,
    val leanMassLbs: Float,
    val fatMassLbs: Float
)

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mealName: String,
    val isPreWorkout: Boolean = false,
    val calories: Int,
    val proteinGrams: Float,
    val carbGrams: Float,
    val fatGrams: Float
)
```

---

## 5. MVP Build Phases (4-Week Roadmap)

1. **Week 1 — Metabolic Core & Data Layer:** Implement Room database, the Alpert variable-deficit math engine, 14-day EMA weight smoothing, and unit tests for phase transitions ($210\text{ lbs} \rightarrow 165\text{ lbs}$).
2. **Week 2 — Carb-Cycling Scheduler & Today UI:** Build the Jetpack Compose dashboard, 7-day workout/carb-cycle planner, and dynamic macro splitter.
3. **Week 3 — Food Logging & Barcode Scanner:** Integrate CameraX + ML Kit Barcode Scanning paired with Open Food Facts / USDA caching and custom meal presets.
4. **Week 4 — Health Connect & Glance Widget:** Wire up Health Connect permissions (auto-importing smart-scale weight/body fat and tennis workouts) and ship the home screen macro widget.