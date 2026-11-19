package com.fatflow.app.domain

import java.time.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MetabolicEngineTest {
    @Test
    fun baseline210() {
        val fat = MetabolicEngine.fatMassLbs(210.0, 31.7)
        val lean = MetabolicEngine.leanMassLbs(210.0, 31.7)
        assertEquals(66.57, fat, 0.01)
        assertEquals(143.43, lean, 0.01)
        assertEquals(1664, MetabolicEngine.maxFatReleaseKcal(fat, 25))
        assertEquals(1578, MetabolicEngine.safeFloorKcal(lean))
        assertEquals(3053, MetabolicEngine.seedTdee(lean))
        assertEquals(1578, MetabolicEngine.calorieTarget(3053, 1664, 1578))
        assertEquals(151, MetabolicEngine.macros(1578, lean, DayType.HIGH_CARB).proteinGrams)
        assertEquals(43, MetabolicEngine.macros(1578, lean, DayType.HIGH_CARB).fatGrams)
        assertEquals(55, MetabolicEngine.macros(1578, lean, DayType.MOD_CARB).fatGrams)
        assertEquals(69, MetabolicEngine.macros(1578, lean, DayType.LOW_CARB).fatGrams)
    }

    @Test
    fun baseline165() {
        val lean = MetabolicEngine.leanMassLbs(165.0, 13.1)
        val fat = MetabolicEngine.fatMassLbs(165.0, 13.1)
        assertEquals(21.615, fat, 0.01)
        assertEquals(540, MetabolicEngine.maxFatReleaseKcal(fat, 25))
        assertEquals(1577, MetabolicEngine.safeFloorKcal(lean))
        assertEquals(2085, MetabolicEngine.calorieTarget(2625, 540, 1577))
    }

    @Test
    fun ema() {
        assertEquals(210.0, MetabolicEngine.nextEma(null, 210.0))
        assertEquals(3148.0 / 15.0, MetabolicEngine.nextEma(210.0, 209.0), 1e-9)
    }

    @Test
    fun adaptiveTdeeFixture() {
        val start = LocalDate.of(2025, 1, 1)
        val obs = buildList {
            add(DayObservation(start, 210.0, 2000))
            for (i in 1..12) {
                add(DayObservation(start.plusDays(i.toLong()), null, 2000))
            }
            add(DayObservation(start.plusDays(13), 208.0, 2000))
        }
        val result = MetabolicEngine.updateAdaptiveTdee(3053, obs)
        assertEquals(2922, result.tdee)
    }

    @Test
    fun adaptiveTdeeSameWeight() {
        val start = LocalDate.of(2025, 1, 1)
        val obs = (0..5).map { DayObservation(start.plusDays(it.toLong()), 210.0, 2000) }
        assertEquals(3053, MetabolicEngine.updateAdaptiveTdee(3053, obs).tdee)
    }

    @Test
    fun adaptiveTdeeTooClose() {
        val start = LocalDate.of(2025, 1, 1)
        val obs = listOf(
            DayObservation(start, 210.0, 2000),
            DayObservation(start.plusDays(3), 209.0, 2000),
        )
        assertEquals(3053, MetabolicEngine.updateAdaptiveTdee(3053, obs).tdee)
    }

    @Test
    fun weeklyCalories() {
        val types = listOf(
            DayType.MOD_CARB,
            DayType.HIGH_CARB,
            DayType.MOD_CARB,
            DayType.HIGH_CARB,
            DayType.MOD_CARB,
            DayType.HIGH_CARB,
            DayType.LOW_CARB,
        )
        val cals = MetabolicEngine.weeklyCalories(1578, types)
        assertEquals(listOf(1503, 1766, 1503, 1766, 1503, 1766, 1239), cals)
        assertEquals(11046, cals.sum())
    }

    @Test
    fun escalationAndBonus() {
        assertEquals(1, MetabolicEngine.escalationSteps(66.57, 64.57))
        assertEquals(0, MetabolicEngine.escalationSteps(66.57, 64.58))
        assertEquals(350, MetabolicEngine.escalationPoolKcal(1, 25))
        val types = listOf(
            DayType.MOD_CARB,
            DayType.HIGH_CARB,
            DayType.MOD_CARB,
            DayType.HIGH_CARB,
            DayType.MOD_CARB,
            DayType.HIGH_CARB,
            DayType.LOW_CARB,
        )
        assertEquals(
            listOf(59, 59, 58, 58, 58, 58, 0),
            MetabolicEngine.distributeTrainingBonus(types, 350),
        )
    }

    @Test
    fun dietBreak() {
        assertTrue(MetabolicEngine.dietBreakDue(210.0, 186.0, 31.7, 31.7))
        assertTrue(!MetabolicEngine.dietBreakDue(210.0, 186.01, 31.7, 21.71))
        assertTrue(MetabolicEngine.dietBreakDue(210.0, 200.0, 31.7, 21.7))
    }

    @Test
    fun validation() {
        assertFailsWith<IllegalArgumentException> { MetabolicEngine.maxFatReleaseKcal(50.0, 21) }
        assertFailsWith<IllegalArgumentException> { MetabolicEngine.maxFatReleaseKcal(50.0, 32) }
        assertFailsWith<IllegalArgumentException> { MetabolicEngine.fatMassLbs(79.0, 20.0) }
    }
}