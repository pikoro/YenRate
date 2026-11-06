package net.owh.yenrate

import net.owh.yenrate.data.RateData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateDataTest {

    @Test
    fun testPositiveRateChange() {
        val rateData = RateData(
            currentRate = 155.50,
            previousRate = 155.00,
            lastUpdatedTime = System.currentTimeMillis()
        )

        assertEquals(0.50, rateData.change, 0.001)
        assertTrue(rateData.isPositive)
        assertFalse(rateData.isNegative)
        assertFalse(rateData.isNeutral)
        assertEquals(0.3225, rateData.changePercentage, 0.001)
    }

    @Test
    fun testNegativeRateChange() {
        val rateData = RateData(
            currentRate = 154.20,
            previousRate = 155.00,
            lastUpdatedTime = System.currentTimeMillis()
        )

        assertEquals(-0.80, rateData.change, 0.001)
        assertFalse(rateData.isPositive)
        assertTrue(rateData.isNegative)
        assertFalse(rateData.isNeutral)
        assertEquals(-0.5161, rateData.changePercentage, 0.001)
    }

    @Test
    fun testUnchangedRate() {
        val rateData = RateData(
            currentRate = 155.00,
            previousRate = 155.00,
            lastUpdatedTime = System.currentTimeMillis()
        )

        assertEquals(0.00, rateData.change, 0.001)
        assertFalse(rateData.isPositive)
        assertFalse(rateData.isNegative)
        assertTrue(rateData.isNeutral)
        assertEquals(0.0, rateData.changePercentage, 0.001)
    }
}
