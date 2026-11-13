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

    val minRate: Double
        get() {
            if (history.isEmpty()) return currentRate
            val rawMin = history.minOf { it.rate }
            val rawMax = history.maxOf { it.rate }
            val mid = (rawMin + rawMax) / 2.0
            return minOf(rawMin, mid - 0.5)
        }

    val maxRate: Double
        get() {
            if (history.isEmpty()) return currentRate
            val rawMin = history.minOf { it.rate }
            val rawMax = history.maxOf { it.rate }
            val mid = (rawMin + rawMax) / 2.0
            return maxOf(rawMax, mid + 0.5)
        }
}

object YenRatePreferences {

    private const val PREFS_NAME = "yen_rate_prefs"
    private const val KEY_CURRENT_RATE = "current_rate"
    private const val KEY_PREVIOUS_RATE = "previous_rate"
    private const val KEY_LAST_UPDATED = "last_updated"
    private const val KEY_HISTORY = "history_json"

    private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    private const val THIRTY_MINUTES_MS = 30 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveRate(context: Context, newRate: Double): RateData {
        val prefs = getPrefs(context)
        val existingCurrent = prefs.getFloat(KEY_CURRENT_RATE, 0.0f).toDouble()
        val previousRate = if (existingCurrent > 0.0) existingCurrent else newRate
        val now = System.currentTimeMillis()

        var history = loadHistory(prefs).toMutableList()

        if (history.size < 6) {
            history = bootstrap24hHistory(newRate, now).toMutableList()
        } else {
            // Remove points older than 24 hours
            history.removeAll { (now - it.timestamp) > TWENTY_FOUR_HOURS_MS }

            val lastPoint = history.lastOrNull()
            if (lastPoint != null && (now - lastPoint.timestamp) < THIRTY_MINUTES_MS) {
                // Update latest entry if within 30 minutes
                history[history.size - 1] = RatePoint(now, newRate)
            } else {
                // Append new hourly entry
                history.add(RatePoint(now, newRate))
            }
        }

        // Keep maximum of 24 points
        val trimmedHistory = if (history.size > 24) {
            history.takeLast(24)
        } else {
            history
        }

        val jsonString = serializeHistory(trimmedHistory)

        prefs.edit {
            putFloat(KEY_PREVIOUS_RATE, previousRate.toFloat())
            putFloat(KEY_CURRENT_RATE, newRate.toFloat())
            putLong(KEY_LAST_UPDATED, now)
            putString(KEY_HISTORY, jsonString)
        }

        return RateData(
            currentRate = newRate,
            previousRate = previousRate,
            lastUpdatedTime = now,
            history = trimmedHistory
        )
    }

    fun getRateData(context: Context): RateData {
        val prefs = getPrefs(context)
        val current = prefs.getFloat(KEY_CURRENT_RATE, 0.0f).toDouble()
        val previous = prefs.getFloat(KEY_PREVIOUS_RATE, 0.0f).toDouble()
        val updated = prefs.getLong(KEY_LAST_UPDATED, 0L)
        var history = loadHistory(prefs)

        if (history.size < 6 && current > 0.0) {
            history = bootstrap24hHistory(current, if (updated > 0) updated else System.currentTimeMillis())
            prefs.edit {
                putString(KEY_HISTORY, serializeHistory(history))
            }
        }

        return RateData(
            currentRate = current,
            previousRate = if (previous > 0.0) previous else current,
            lastUpdatedTime = updated,
            history = history
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

    private fun bootstrap24hHistory(currentRate: Double, now: Long): List<RatePoint> {
        val list = mutableListOf<RatePoint>()
        val count = 24
        val stepMs = TWENTY_FOUR_HOURS_MS / count
        val offsets = arrayOf(
            0.0, -0.12, -0.25, -0.38, -0.22, -0.08, 0.15, 0.32, 0.45, 0.28,
            0.10, -0.05, -0.18, -0.30, -0.15, 0.05, 0.20, 0.35, 0.25, 0.12,
            -0.04, -0.10, 0.02, 0.0
        )
        for (i in 0 until count) {
            val time = now - (count - 1 - i) * stepMs
            val simulated = (currentRate + offsets[i]).coerceAtLeast(1.0)
            val rounded = String.format(java.util.Locale.US, "%.2f", simulated).toDouble()
            list.add(RatePoint(time, rounded))
        }
        return list
    }
}
