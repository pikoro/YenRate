package net.owh.yenrate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.owh.yenrate.data.RateData
import net.owh.yenrate.data.YenRatePreferences
import net.owh.yenrate.data.YenRateRepository
import net.owh.yenrate.ui.theme.YenRateTheme
import net.owh.yenrate.worker.YenRateUpdateWorker
import net.owh.yenrate.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure hourly background worker is scheduled
        YenRateUpdateWorker.schedulePeriodicWork(this)

        setContent {
            YenRateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    YenRateMainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun YenRateMainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rateData by remember { mutableStateOf(YenRatePreferences.getRateData(context)) }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshData() {
        scope.launch {
            isLoading = true
            val result = YenRateRepository.fetchJpyRate()
            result.onSuccess { newRate ->
                rateData = YenRatePreferences.saveRate(context, newRate)
                WidgetUpdater.updateAllWidgets(context)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (rateData.currentRate <= 0.0) {
            refreshData()
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF0F172A))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.yen_rate_tracker),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.live_exchange_rate_title),
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Exchange Rate Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.one_usd_equals),
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    val rateText = if (rateData.currentRate > 0.0) {
                        String.format(Locale.US, "¥ %.2f", rateData.currentRate)
                    } else {
                        "¥ ---.--"
                    }
                    Text(
                        text = rateText,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (rateData.currentRate > 0.0) {
                        Text(
                            text = stringResource(R.string.real_rate_title) + ": " + String.format(Locale.US, "¥ %.2f", rateData.realRate),
                            color = Color(0xFFFFD600),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val changeText = if (rateData.currentRate > 0.0) {
                    String.format(
                        Locale.US,
                        "%+.2f (%+.2f%%)",
                        rateData.change,
                        rateData.changePercentage
                    )
                } else {
                    "-- (--%)"
                }
                Text(
                    text = changeText,
                    color = if (rateData.isPositive) Color(0xFF4CAF50) else if (rateData.isNegative) Color(0xFFEF5350) else Color(0xFF94A3B8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val lastUpdatedText = if (rateData.lastUpdatedTime > 0) {
                    val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                    stringResource(R.string.last_updated_at, sdf.format(Date(rateData.lastUpdatedTime)))
                } else {
                    stringResource(R.string.updating_rate)
                }
                Text(
                    text = lastUpdatedText,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 24-Hour Trend Chart Card
        RateHistoryCard(data = rateData)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { refreshData() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.refresh_button),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Widget Setup Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_screen_widget),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.widget_instructions),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun RateHistoryCard(data: RateData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.rate_history_24h),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.real_rate_indicator) + ": " + String.format(Locale.US, "¥%.2f", data.realRate),
                    color = Color(0xFFFFD600),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (data.history.size >= 2) {
                val minRate = data.minRate
                val maxRate = data.maxRate
                val midRate = (minRate + maxRate) / 2.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Y-Axis Labels (Rate Scale)
                    Column(
                        modifier = Modifier
                            .height(140.dp)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = String.format(Locale.US, "¥%.2f", maxRate),
                            color = Color(0xFF4CAF50),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "¥%.2f", midRate),
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "¥%.2f", minRate),
                            color = Color(0xFFEF5350),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Main Chart Canvas
                    RateHistoryCanvas(
                        data = data,
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // X-Axis Labels (Time Scale)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.time_24h_ago),
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.time_12h_ago),
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.time_now),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RateHistoryCanvas(
    data: RateData,
    modifier: Modifier = Modifier
) {
    val history = data.history
    val lineColor = Color(0xFF3B82F6) // Always blue line
    val fillColor = Color(0x333B82F6) // Always blue gradient fill
    val gridColor = Color(0xFF334155)

    val highDottedColor = Color(0xAA4CAF50) // Light green dotted line for 24h High
    val lowDottedColor = Color(0xAAEF5350)  // Light red dotted line for 24h Low

    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

        val width = size.width
        val height = size.height

        // Draw horizontal gridlines (Top, Mid, Bottom)
        drawLine(color = gridColor, start = Offset(0f, 4f), end = Offset(width, 4f), strokeWidth = 1.dp.toPx())
        drawLine(color = gridColor, start = Offset(0f, height / 2f), end = Offset(width, height / 2f), strokeWidth = 1.dp.toPx())
        drawLine(color = gridColor, start = Offset(0f, height - 4f), end = Offset(width, height - 4f), strokeWidth = 1.dp.toPx())

        val minRate = data.minRate
        val maxRate = data.maxRate
        val rateRange = (maxRate - minRate).let { if (it <= 0.001) 1.0 else it }

        val padding = 8f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // Draw Light Dotted Lines for High and Low Rates
        val historyMax = history.maxOf { it.rate }
        val historyMin = history.minOf { it.rate }

        val highY = padding + chartHeight - (((historyMax - minRate) / rateRange).toFloat() * chartHeight)
        val lowY = padding + chartHeight - (((historyMin - minRate) / rateRange).toFloat() * chartHeight)

        val dottedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        // Light dotted line for High rate
        drawLine(
            color = highDottedColor,
            start = Offset(0f, highY),
            end = Offset(width, highY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dottedEffect
        )

        // Light dotted line for Low rate
        drawLine(
            color = lowDottedColor,
            start = Offset(0f, lowY),
            end = Offset(width, lowY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dottedEffect
        )

        // Main Rate Points (Market Rate) - Straight Line Segments
        val mainPoints = history.mapIndexed { index, point ->
            val x = padding + (index.toFloat() / (history.size - 1)) * chartWidth
            val normalizedRate = (point.rate - minRate) / rateRange
            val y = padding + chartHeight - (normalizedRate.toFloat() * chartHeight)
            Pair(x, y)
        }

        // Construct Main Path & Gradient Fill Path using STRAIGHT LINE SEGMENTS (lineTo)
        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(mainPoints[0].first, mainPoints[0].second)
        fillPath.moveTo(mainPoints[0].first, height)
        fillPath.lineTo(mainPoints[0].first, mainPoints[0].second)

        for (i in 1 until mainPoints.size) {
            strokePath.lineTo(mainPoints[i].first, mainPoints[i].second)
            fillPath.lineTo(mainPoints[i].first, mainPoints[i].second)
        }

        fillPath.lineTo(mainPoints.last().first, height)
        fillPath.close()

        // Draw Fill Gradient under Main Line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw Blue Main Market Rate Line (Straight Segments)
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
