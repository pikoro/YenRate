package net.owh.yenrate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.owh.yenrate.data.YenRatePreferences
import net.owh.yenrate.data.YenRateRepository
import net.owh.yenrate.ui.theme.YenRateTheme
import net.owh.yenrate.widget.YenRateWidgetProvider
import net.owh.yenrate.worker.YenRateUpdateWorker
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
            YenRateRepository.fetchJpyRate().onSuccess { newRate ->
                rateData = YenRatePreferences.saveRate(context, newRate)
                YenRateWidgetProvider.updateAllWidgets(context)
                net.owh.yenrate.widget.YenRateCompactWidgetProvider.updateAllWidgets(context)
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
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = if (rateData.currentRate > 0.0) {
                            String.format(Locale.US, "¥ %.2f", rateData.currentRate)
                        } else {
                            "¥ ---.--"
                        },
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rate Change Row
                if (rateData.currentRate > 0.0) {
                    val changeColor = when {
                        rateData.isPositive -> Color(0xFF4CAF50)
                        rateData.isNegative -> Color(0xFFEF5350)
                        else -> Color(0xFF94A3B8)
                    }

                    val arrowRes = when {
                        rateData.isPositive -> R.drawable.ic_arrow_up
                        rateData.isNegative -> R.drawable.ic_arrow_down
                        else -> R.drawable.ic_arrow_neutral
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = arrowRes),
                            contentDescription = stringResource(R.string.rate_change_indicator),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "%+.2f (%+.2f%%)",
                                rateData.change,
                                rateData.changePercentage
                            ),
                            color = changeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (rateData.lastUpdatedTime > 0) {
                        val timeFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        Text(
                            text = stringResource(R.string.last_updated_at, timeFormat.format(Date(rateData.lastUpdatedTime))),
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { refreshData() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isLoading) stringResource(R.string.refreshing) else stringResource(R.string.refresh_now))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Widget Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_screen_widget),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_instructions),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.author_label,
                        stringResource(R.string.author_name),
                        stringResource(R.string.author_email)
                    ),
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        }
    }
}
