package net.owh.yenrate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class RateData(
    val currentRate: Double,
    val previousRate: Double,
    val lastUpdatedTime: Long
) {
    val change: Double get() = currentRate - previousRate
    val changePercentage: Double
        get() = if (previousRate > 0.0) {
            (change / previousRate) * 100.0
        } else 0.0

    val isPositive: Boolean get() = change > 0.00001
    val isNegative: Boolean get() = change < -0.00001
    val isNeutral: Boolean get() = !isPositive && !isNegative
}

object YenRatePreferences {

    private const val PREFS_NAME = "yen_rate_prefs"
    private const val KEY_CURRENT_RATE = "current_rate"
    private const val KEY_PREVIOUS_RATE = "previous_rate"
    private const val KEY_LAST_UPDATED = "last_updated"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveRate(context: Context, newRate: Double): RateData {
        val prefs = getPrefs(context)
        val existingCurrent = prefs.getFloat(KEY_CURRENT_RATE, 0.0f).toDouble()

        val previousRate = if (existingCurrent > 0.0) {
            existingCurrent
        } else {
            newRate
        }

        val now = System.currentTimeMillis()

        prefs.edit {
            putFloat(KEY_PREVIOUS_RATE, previousRate.toFloat())
            putFloat(KEY_CURRENT_RATE, newRate.toFloat())
            putLong(KEY_LAST_UPDATED, now)
        }

        return RateData(
            currentRate = newRate,
            previousRate = previousRate,
            lastUpdatedTime = now
        )
    }

    fun getRateData(context: Context): RateData {
        val prefs = getPrefs(context)
        val current = prefs.getFloat(KEY_CURRENT_RATE, 0.0f).toDouble()
        val previous = prefs.getFloat(KEY_PREVIOUS_RATE, 0.0f).toDouble()
        val updated = prefs.getLong(KEY_LAST_UPDATED, 0L)

        return RateData(
            currentRate = current,
            previousRate = if (previous > 0.0) previous else current,
            lastUpdatedTime = updated
        )
    }
}
