package com.mtrx.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mtrx.store.core.FileSizeFormatter
import com.mtrx.store.domain.model.AppStatus
import com.mtrx.store.domain.model.StoreApp
import com.mtrx.store.ui.theme.MtrxDarkBorder
import com.mtrx.store.ui.theme.MtrxGold
import com.mtrx.store.ui.theme.StatusBlue
import com.mtrx.store.ui.theme.StatusGreen
import com.mtrx.store.ui.theme.StatusOrange

@Composable
fun AppCard(
    app: StoreApp,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MtrxDarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                AsyncImage(
                    model = app.iconUrl,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MtrxDarkBorder),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Title & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.shortDescription.ifEmpty { app.category },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "v${app.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FileSizeFormatter.format(app.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = app.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MtrxGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Dynamic Action Button
                ActionButton(
                    status = app.status,
                    onActionClick = onActionClick
                )
            }

            // If downloading, show progress bar right below the row
            if (app.status is AppStatus.Downloading) {
                Spacer(modifier = Modifier.height(10.dp))
                DownloadProgressBar(
                    bytesDownloaded = (app.status.progressFraction * app.sizeBytes).toLong(),
                    totalBytes = app.sizeBytes,
                    progressFraction = app.status.progressFraction,
                    speedBytesPerSec = 0L
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    status: AppStatus,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (status) {
        is AppStatus.NotInstalled -> {
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = MtrxGold),
                shape = RoundedCornerShape(20.dp),
                modifier = modifier.height(34.dp)
            ) {
                Text(
                    text = "Get",
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        is AppStatus.UpdateAvailable -> {
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                shape = RoundedCornerShape(20.dp),
                modifier = modifier.height(34.dp)
            ) {
                Text(
                    text = "Update",
                    color = MaterialTheme.colorScheme.surface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        is AppStatus.Installed -> {
            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(20.dp),
                modifier = modifier.height(34.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen)
            ) {
                Text(
                    text = "Open",
                    color = StatusGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        is AppStatus.Downloading -> {
            Box(
                modifier = modifier
                    .size(36.dp)
                    .background(MtrxDarkBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { status.progressFraction },
                    modifier = Modifier.size(24.dp),
                    color = MtrxGold,
                    strokeWidth = 2.5.dp
                )
            }
        }

        is AppStatus.Installing -> {
            Box(
                modifier = modifier
                    .size(36.dp)
                    .background(MtrxDarkBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = StatusBlue,
                    strokeWidth = 2.dp
                )
            }
        }

        is AppStatus.Error -> {
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(20.dp),
                modifier = modifier.height(34.dp)
            ) {
                Text(
                    text = "Retry",
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 12.sp
                )
            }
        }
    }
}
