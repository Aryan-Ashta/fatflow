package com.fatflow.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrajectoryProjectorTest {
    @Test
    fun project210To165() {
        val lean = 143.43
        val fat = 66.57
        val points = TrajectoryProjector.project(lean, fat, 3053, 25, 1.0, 165.0)
        val week1 = points[1]
        assertEquals(143.43 + 66.57 - 2.95, week1.weightLbs, 0.01)
        val last = points.last()
        assertEquals(165 - 143.43, last.weightLbs - 143.43, 0.05)
        for (i in 1 until points.size) {
            assertTrue(points[i].calorieTarget >= points[i - 1].calorieTarget)
        }
    }
}