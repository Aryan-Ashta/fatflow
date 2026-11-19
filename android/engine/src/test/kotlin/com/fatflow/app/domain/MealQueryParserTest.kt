package com.fatflow.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MealQueryParserTest {
    @Test
    fun parses() {
        val a = MealQueryParser.parse("200g chicken breast")
        assertIs<ParsedInput.Food>(a)
        assertEquals(200.0, a.grams)
        assertEquals("chicken breast", a.query)

        val b = MealQueryParser.parse("1.5 kg oats")
        assertIs<ParsedInput.Food>(b)
        assertEquals(1500.0, b.grams)

        val c = MealQueryParser.parse("2 oz almonds")
        assertIs<ParsedInput.Food>(c)
        assertEquals(56.699, c.grams!!, 0.001)

        assertIs<ParsedInput.Food>(MealQueryParser.parse("chicken breast"))
        assertIs<ParsedInput.Barcode>(MealQueryParser.parse("012345678905"))
        assertIs<ParsedInput.Empty>(MealQueryParser.parse(""))
    }
}