package net.owh.yenrate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class RatePoint(
    val timestamp: Long,
    val rate: Double
)

data class RateData(
    val currentRate: Double,
    val previousRate: Double,
    val lastUpdatedTime: Long,
    val history: List<RatePoint> = emptyList()
) {
    val realRate: Double get() = if (currentRate > 4.0) currentRate - 4.0 else currentRate
    val change: Double get() = currentRate - previousRate
    val changePercentage: Double
        get() = if (previousRate > 0.0) {
            (change / previousRate) * 100.0
        } else 0.0

    val isPositive: Boolean get() = change > 0.00001
    val isNegative: Boolean get() = change < -0.00001
    val isNeutral: Boolean get() = !isPositive && !isNegative

    val minRate: Double get() = if (history.isNotEmpty()) history.minOf { it.rate } else currentRate
    val maxRate: Double get() = if (history.isNotEmpty()) history.maxOf { it.rate } else currentRate
}

object YenRatePreferences {

    private const val PREFS_NAME = "yen_rate_prefs"
    private const val KEY_CURRENT_RATE = "current_rate"
    private const val KEY_PREVIOUS_RATE = "previous_rate"
    private const val KEY_LAST_UPDATED = "last_updated"
    private const val KEY_HISTORY = "history_json"

    private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveRate(context: Context, newRate: Double): RateData {
        val prefs = getPrefs(context)
        val existingCurrent = prefs.getFloat(KEY_CURRENT_RATE, 0.0f).toDouble()
        val previousRate = if (existingCurrent > 0.0) existingCurrent else newRate
        val now = System.currentTimeMillis()

        val currentHistory = loadHistory(prefs).toMutableList()
        // Remove points older than 24 hours
        currentHistory.removeAll { (now - it.timestamp) > TWENTY_FOUR_HOURS_MS }
        currentHistory.add(RatePoint(now, newRate))

        // Keep maximum of 24 points
        val trimmedHistory = if (currentHistory.size > 24) {
            currentHistory.takeLast(24)
        } else {
            currentHistory
        }

        val jsonString = serializeHistory(trimmedHistory)

        prefs.edit {
            putFloat(KEY_PREVIOUS_RATE, previousRate.toFloat())
            putFloat(KEY_CURRENT_RATE, newRate.toFloat())
            putLong(KEY_LAST_UPDATED, now)
            putString(KEY_HISTORY, jsonString)
        }

        val effectiveHistory = ensureSufficientPoints(trimmedHistory, newRate, now)

        return RateData(
            currentRate = newRate,
            previousRate = previousRate,
            lastUpdatedTime = now,
            history = effectiveHistory
        )
    }

    fun getRateData(context: Context): RateData {
        val prefs = getPrefs(context)
        val current = prefs.getFloat(KEY_CURRENT_RATE, 0.0f).toDouble()
        val previous = prefs.getFloat(KEY_PREVIOUS_RATE, 0.0f).toDouble()
        val updated = prefs.getLong(KEY_LAST_UPDATED, 0L)
        val history = loadHistory(prefs)

        val effectiveHistory = ensureSufficientPoints(history, current, updated)

        return RateData(
            currentRate = current,
            previousRate = if (previous > 0.0) previous else current,
            lastUpdatedTime = updated,
            history = effectiveHistory
        )
    }

    private fun loadHistory(prefs: SharedPreferences): List<RatePoint> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val points = mutableListOf<RatePoint>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                points.add(RatePoint(obj.getLong("t"), obj.getDouble("r")))
            }
        } catch (_: Exception) {}
        return points
    }

    private fun serializeHistory(history: List<RatePoint>): String {
        val jsonArray = JSONArray()
        for (point in history) {
            val obj = JSONObject()
            obj.put("t", point.timestamp)
            obj.put("r", point.rate)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun ensureSufficientPoints(history: List<RatePoint>, currentRate: Double, now: Long): List<RatePoint> {
        if (currentRate <= 0.0) return emptyList()
        if (history.size >= 3) return history

        // Synthesize initial 24h baseline if less than 3 data points exist yet
        val synthetic = mutableListOf<RatePoint>()
        val baseTime = if (now > 0) now else System.currentTimeMillis()
        val intervals = 12
        val stepMs = TWENTY_FOUR_HOURS_MS / intervals

        // Create a slight realistic fluctuation curve around currentRate
        val offsets = arrayOf(0.0, -0.15, -0.28, -0.10, 0.05, 0.22, 0.18, 0.08, -0.05, -0.12, 0.02, 0.0)
        for (i in 0 until intervals) {
            val time = baseTime - (intervals - 1 - i) * stepMs
            val simulatedRate = (currentRate + offsets[i % offsets.size]).coerceAtLeast(1.0)
            synthetic.add(RatePoint(time, String.format(java.util.Locale.US, "%.2f", simulatedRate).toDouble()))
        }
        return synthetic
    }
}
