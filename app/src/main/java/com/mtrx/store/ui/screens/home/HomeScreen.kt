package com.mtrx.store.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mtrx.store.R
import com.mtrx.store.core.FileSizeFormatter
import com.mtrx.store.domain.model.AppStatus
import com.mtrx.store.domain.model.StoreApp
import com.mtrx.store.ui.components.AppCard
import com.mtrx.store.ui.components.AppIcon
import com.mtrx.store.ui.components.PermissionDialog
import com.mtrx.store.ui.theme.MtrxDarkBorder
import com.mtrx.store.ui.theme.MtrxGold
import com.mtrx.store.ui.theme.TextSecondaryDark

@Composable
fun HomeScreen(
    onAppClick: (String) -> Unit,
    onProfileClick: () -> Unit = {},
    onNavigateToUpdates: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    if (uiState.showPermissionDialog) {
        PermissionDialog(
            onDismiss = { viewModel.dismissPermissionDialog() },
            onGrantPermission = {
                val intent = viewModel.getManageUnknownSourcesIntent()
                context.startActivity(intent)
                viewModel.dismissPermissionDialog()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF090A0D)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Top Brand Header Bar with 3D M Logo and User Avatar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.mtrx_logo_mark),
                            contentDescription = "MTRX Logo",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color(0x33D4AF37))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MTRX STORE",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "MARAPATHRAN GROUPS",
                                color = Color(0xFF8E8E98),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // User Profile Icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onProfileClick() }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.5.dp, Color(0x60D4AF37), CircleShape)
                                .background(Color(0xFF161720), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "User",
                            fontSize = 10.sp,
                            color = Color(0xFF9E9EA8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Search Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 3.dp)
                        .background(Color(0xFF14151C), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFF22242F), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF7A7D8E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search Apps...",
                                        color = Color(0xFF6B6E7E),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Filter",
                            tint = MtrxGold,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { viewModel.refreshCatalog() }
                        )
                    }
                }
            }

            // 3. Category Tabs
            item {
                val displayCategories = if (uiState.categories.isNotEmpty()) uiState.categories else listOf("All")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(displayCategories) { category ->
                        val isSelected = category.equals(uiState.selectedCategory, ignoreCase = true)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.onCategorySelected(category) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) MtrxGold else Color(0xFF8A8D9F),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFBD821E),
                                                    Color(0xFFF5C767),
                                                    Color(0xFFBD821E)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.height(2.5.dp))
                            }
                        }
                    }
                }
            }

            // 4. Featured Hero Banner
            if (uiState.searchQuery.isEmpty() && uiState.selectedCategory == "All") {
                item {
                    val featured = uiState.featuredApps.firstOrNull() ?: uiState.allApps.firstOrNull()
                    if (featured != null) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(MtrxGold, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FEATURED MTRX SUITE",
                                    color = Color(0xFFC4A052),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            FeaturedHeroCard(
                                app = featured,
                                onClick = { onAppClick(featured.packageName) },
                                onActionClick = { viewModel.onAppActionClick(featured) }
                            )
                        }
                    }
                }
            }

            // 5. "Recently Added" Section (Horizontal Scroll of Apps)
            if (uiState.searchQuery.isEmpty() && uiState.selectedCategory == "All") {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(16.dp)
                                        .background(MtrxGold, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recently Added",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { /* Show all recent */ }
                            ) {
                                Text(
                                    text = "See All",
                                    color = MtrxGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MtrxGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            val itemsToShow = if (uiState.recentlyUpdatedApps.isNotEmpty()) {
                                uiState.recentlyUpdatedApps
                            } else {
                                uiState.allApps
                            }

                            items(itemsToShow) { app ->
                                RecentlyAddedCard(
                                    app = app,
                                    onClick = { onAppClick(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. "Updates Available" Section - Show ONLY when real catalog updates exist
            if (uiState.searchQuery.isEmpty() && uiState.selectedCategory == "All" && uiState.appsWithUpdates.isNotEmpty()) {
                item {
                    val updateApp = uiState.appsWithUpdates.first()
                    val updatesCount = uiState.appsWithUpdates.size

                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(16.dp)
                                        .background(MtrxGold, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Updates Available",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(0xFFC78824), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = updatesCount.toString(),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToUpdates() }
                            ) {
                                Text(
                                    text = "See All",
                                    color = MtrxGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MtrxGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // App Update Card from Catalog
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 6.dp)
                                .clickable { onNavigateToUpdates() },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14151C)),
                            border = BorderStroke(1.dp, Color(0xFF222430))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1C1D26))
                                        .border(1.dp, Color(0xFF2A2B38), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIcon(
                                        packageName = updateApp.packageName,
                                        iconUrl = updateApp.iconUrl,
                                        contentDescription = updateApp.name,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 12.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = updateApp.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (updateApp.changelog.isNotBlank()) updateApp.changelog.lines().first()
                                        else updateApp.shortDescription.ifEmpty { "Update available" },
                                        color = Color(0xFF8E8E98),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${FileSizeFormatter.format(updateApp.sizeBytes)} • v${updateApp.versionName}",
                                        color = Color(0xFF6B6E7E),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = { viewModel.onAppActionClick(updateApp) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                listOf(Color(0xFFE5A93C), Color(0xFFC78824))
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                ) {
                                    Text(
                                        text = "Update",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. All Applications Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .background(MtrxGold, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.selectedCategory == "All") "All Applications" else "${uiState.selectedCategory} Apps",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${uiState.allApps.size} available",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            // App Cards List
            if (uiState.allApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No applications found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                items(uiState.allApps, key = { it.packageName }) { app ->
                    AppCard(
                        app = app,
                        onClick = { onAppClick(app.packageName) },
                        onActionClick = { viewModel.onAppActionClick(app) },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedHeroCard(
    app: StoreApp,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131418)),
        border = BorderStroke(1.dp, Color(0xFF28251E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Soft, wide golden ambient wave
                    val swooshPath = Path().apply {
                        moveTo(width * 0.25f, height)
                        cubicTo(
                            width * 0.50f, height * 0.92f,
                            width * 0.60f, height * 0.40f,
                            width, height * 0.08f
                        )
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = swooshPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x35E5A93C), Color(0x06E5A93C))
                        )
                    )

                    val crestPath = Path().apply {
                        moveTo(width * 0.25f, height)
                        cubicTo(
                            width * 0.50f, height * 0.92f,
                            width * 0.60f, height * 0.40f,
                            width, height * 0.08f
                        )
                    }
                    drawPath(
                        path = crestPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x00E5A93C), Color(0x99F5C767), Color(0x33E5A93C))
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: App Icon + Title & Tagline directly from Catalog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0F1015))
                            .border(1.dp, Color(0xFF2E2B24), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            packageName = app.packageName,
                            iconUrl = app.iconUrl,
                            contentDescription = app.name,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 18.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = app.shortDescription.ifEmpty { app.description },
                            color = Color(0xFF9E9EA8),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Category & Version on left, Action Button on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.category,
                            color = MtrxGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "  •  ",
                            color = Color(0xFF5A5D6E),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "v${app.versionName} (${FileSizeFormatter.format(app.sizeBytes)})",
                            color = Color(0xFF9E9EA8),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Gold Pill Action Button
                    when (app.status) {
                        is AppStatus.Downloading -> {
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF222430))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFFE5A93C),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${(app.status.progressFraction * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        else -> {
                            val buttonText = when (app.status) {
                                is AppStatus.Installed -> "OPEN"
                                is AppStatus.UpdateAvailable -> "UPDATE"
                                else -> "INSTALL"
                            }
                            Button(
                                onClick = onActionClick,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFE5A93C),
                                                Color(0xFFF4C563),
                                                Color(0xFFC78824)
                                            )
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Text(
                                    text = buttonText,
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Tagline: INNOVATE • BUILD • LEAD
                Text(
                    text = "INNOVATE  •  BUILD  •  LEAD",
                    color = Color(0xFF6B6E7E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun RecentlyAddedCard(
    app: StoreApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(106.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14151C)),
        border = BorderStroke(1.dp, Color(0xFF222430))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1C25)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    packageName = app.packageName,
                    iconUrl = app.iconUrl,
                    contentDescription = app.name,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 14.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = app.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.category,
                color = Color(0xFF8A8D9F),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "v${app.versionName}",
                color = MtrxGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp
            )
        }
    }
}
