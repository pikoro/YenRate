package net.owh.yenrate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                }

                Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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
