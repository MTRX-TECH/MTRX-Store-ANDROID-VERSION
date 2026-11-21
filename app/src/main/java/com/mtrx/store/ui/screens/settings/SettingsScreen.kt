package com.mtrx.store.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtrx.store.data.local.preferences.AppTheme
import com.mtrx.store.ui.theme.MtrxDarkBorder
import com.mtrx.store.ui.theme.MtrxDarkCard
import com.mtrx.store.ui.theme.MtrxGold
import com.mtrx.store.ui.theme.StatusRed
import com.mtrx.store.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var catalogUrlInput by remember(uiState.catalogUrl) { mutableStateOf(uiState.catalogUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Theme Setting
            item {
                SectionCard(
                    title = "Appearance & Theme",
                    icon = Icons.Default.ColorLens
                ) {
                    Text(
                        text = "Choose your preferred display theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ThemeOptionChip(
                            label = "Dark",
                            isSelected = uiState.currentTheme == AppTheme.DARK,
                            onClick = { viewModel.setTheme(AppTheme.DARK) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeOptionChip(
                            label = "Light",
                            isSelected = uiState.currentTheme == AppTheme.LIGHT,
                            onClick = { viewModel.setTheme(AppTheme.LIGHT) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeOptionChip(
                            label = "System",
                            isSelected = uiState.currentTheme == AppTheme.SYSTEM,
                            onClick = { viewModel.setTheme(AppTheme.SYSTEM) }
                        )
                    }
                }
            }

            // Catalog Backend URL Setting
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(
                    title = "App Catalog Backend",
                    icon = Icons.Default.Language
                ) {
                    Text(
                        text = "Direct raw JSON URL for app discovery. Hosted on GitHub with zero server dependencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = catalogUrlInput,
                        onValueChange = { catalogUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedIndicatorColor = MtrxGold,
                            unfocusedIndicatorColor = MtrxDarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                viewModel.setCatalogUrl(catalogUrlInput)
                                Toast.makeText(context, "Catalog URL updated & reloaded", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MtrxGold),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = "Save & Reload", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.resetCatalogUrlToDefault()
                                Toast.makeText(context, "Reset to default GitHub URL", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MtrxDarkBorder)
                        ) {
                            Text(text = "Reset Default", color = Color.White)
                        }
                    }
                }
            }

            // Background Updates Interval
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(
                    title = "Periodic Background Checks",
                    icon = Icons.Default.Schedule
                ) {
                    Text(
                        text = "WorkManager automatically checks for newer APK versions in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IntervalChip("6 Hours", uiState.updateIntervalHours == 6L) { viewModel.setUpdateInterval(6L) }
                        Spacer(modifier = Modifier.width(6.dp))
                        IntervalChip("12 Hours", uiState.updateIntervalHours == 12L) { viewModel.setUpdateInterval(12L) }
                        Spacer(modifier = Modifier.width(6.dp))
                        IntervalChip("24 Hours", uiState.updateIntervalHours == 24L) { viewModel.setUpdateInterval(24L) }
                    }
                    if (uiState.lastCheckTimestamp > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val formatted = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(uiState.lastCheckTimestamp))
                        Text(
                            text = "Last checked: $formatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MtrxGold
                        )
                    }
                }
            }

            // Storage Cache Management
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(
                    title = "Storage & Downloads",
                    icon = Icons.Default.Cached
                ) {
                    Text(
                        text = "Clean up temporary downloaded APK files from cache storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.clearCache()
                            Toast.makeText(context, "APK download cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Cache", tint = StatusRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Clear APK Cache", color = StatusRed)
                    }
                }
            }

            // About MTRX Store
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(
                    title = "About MTRX Store",
                    icon = Icons.Default.Info
                ) {
                    Text(
                        text = "MTRX Store for Android",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Version 1.0.0 • Build 1 • Target SDK 35 (Android 15)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MtrxGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Developed by MTRX Tech (Marapathran Groups). A custom independent app marketplace engineered with Kotlin, Jetpack Compose, Material 3, Room, OkHttp, and PackageInstaller.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔒 100% Free & Open Ecosystem • Zero Telemetry",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MtrxDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, MtrxDarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MtrxGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ThemeOptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MtrxGold,
            selectedLabelColor = Color.Black,
            containerColor = MaterialTheme.colorScheme.background,
            labelColor = TextSecondaryDark
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) MtrxGold else MtrxDarkBorder,
            enabled = true,
            selected = isSelected
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun IntervalChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MtrxGold,
            selectedLabelColor = Color.Black,
            containerColor = MaterialTheme.colorScheme.background,
            labelColor = TextSecondaryDark
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) MtrxGold else MtrxDarkBorder,
            enabled = true,
            selected = isSelected
        ),
        shape = RoundedCornerShape(16.dp)
    )
}
