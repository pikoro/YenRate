package net.owh.yenrate.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object YenRateRepository {

    private const val PRIMARY_API_URL = "https://open.er-api.com/v6/latest/USD"
    private const val FALLBACK_API_URL = "https://api.exchangerate-api.com/v4/latest/USD"

    suspend fun fetchJpyRate(): Result<Double> = withContext(Dispatchers.IO) {
        val primaryResult = fetchFromUrl(PRIMARY_API_URL)
        if (primaryResult.isSuccess) {
            primaryResult
        } else {
            fetchFromUrl(FALLBACK_API_URL)
        }
    }

    private fun fetchFromUrl(urlString: String): Result<Double> {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonText)
                val rates = json.getJSONObject("rates")
                val jpyRate = rates.getDouble("JPY")
                Result.success(jpyRate)
            } else {
                Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
