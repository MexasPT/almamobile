package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsLocation
import com.example.ui.theme.AlmaBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun MapLocationCard(
    location: GpsLocation,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Localização GPS do Dispositivo",
    showAddressDetail: Boolean = true
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_location_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "GPS Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = location.signalStrength,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (location.accuracy <= 10f) SuccessGreen else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_refresh_gps")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar GPS",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Stylized Interactive Visual Map Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .clickable {
                        // Open in external Google Maps
                        try {
                            val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${Uri.encode(location.street)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(location.mapsUrl))
                            context.startActivity(browserIntent)
                        }
                    }
            ) {
                // Vector Map Drawing Canvas (Modern dark cartographic style)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Background grid
                    val gridSpacing = 40.dp.toPx()
                    for (x in 0..(w / gridSpacing).toInt()) {
                        drawLine(
                            color = Color(0x1A64748B),
                            start = Offset(x * gridSpacing, 0f),
                            end = Offset(x * gridSpacing, h),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..(h / gridSpacing).toInt()) {
                        drawLine(
                            color = Color(0x1A64748B),
                            start = Offset(0f, y * gridSpacing),
                            end = Offset(w, y * gridSpacing),
                            strokeWidth = 1f
                        )
                    }

                    // Water / River curved feature
                    val riverPath = Path().apply {
                        moveTo(0f, h * 0.75f)
                        cubicTo(w * 0.3f, h * 0.9f, w * 0.6f, h * 0.55f, w, h * 0.7f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = riverPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x2B0284C7), Color(0x400284C7))
                        )
                    )

                    // Secondary roads
                    val roadPaint = Color(0x3394A3B8)
                    drawLine(color = roadPaint, start = Offset(0f, h * 0.35f), end = Offset(w, h * 0.25f), strokeWidth = 12f)
                    drawLine(color = roadPaint, start = Offset(w * 0.2f, 0f), end = Offset(w * 0.35f, h), strokeWidth = 14f)
                    drawLine(color = roadPaint, start = Offset(w * 0.75f, 0f), end = Offset(w * 0.65f, h), strokeWidth = 10f)

                    // Primary Main Avenue crossing near center
                    drawLine(
                        color = Color(0x5538BDF8),
                        start = Offset(0f, h * 0.55f),
                        end = Offset(w, h * 0.45f),
                        strokeWidth = 18f
                    )
                    drawLine(
                        color = Color(0x99FFFFFF),
                        start = Offset(0f, h * 0.55f),
                        end = Offset(w, h * 0.45f),
                        strokeWidth = 2f
                    )

                    // Pulsing GPS Accuracy Wave
                    drawCircle(
                        color = AlmaBlue.copy(alpha = pulseAlpha),
                        radius = pulseRadius * 2f,
                        center = center
                    )

                    // Fixed GPS Range Circle
                    drawCircle(
                        color = Color(0x3338BDF8),
                        radius = 24.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color(0x8038BDF8),
                        radius = 24.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Center Target Marker
                    drawCircle(
                        color = Color(0xFFFFFFFF),
                        radius = 8.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF0284C7),
                        radius = 5.dp.toPx(),
                        center = center
                    )
                }

                // Floating Coordinates Chip on Map
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC0B132B),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.5f, %.5f".format(location.latitude, location.longitude),
                            color = Color(0xFFF1F5F9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Map Action Overlay (Open Maps)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0284C7),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Abrir Mapa",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Address Details
            if (showAddressDetail) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Nome da Rua / Morada Detetada:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = location.streetWithNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Satélites: ${location.satellitesCount} ativos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " • Alt: ${location.altitude.toInt()}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Precisão: ±${location.accuracy.toInt()}m",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
