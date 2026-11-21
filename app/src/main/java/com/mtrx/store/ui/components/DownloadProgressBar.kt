package com.mtrx.store.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtrx.store.core.FileSizeFormatter
import com.mtrx.store.ui.theme.MtrxGold

@Composable
fun DownloadProgressBar(
    bytesDownloaded: Long,
    totalBytes: Long,
    progressFraction: Float,
    speedBytesPerSec: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MtrxGold,
            trackColor = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            val percent = (progressFraction * 100).toInt()
            Text(
                text = "$percent% • ${FileSizeFormatter.format(bytesDownloaded)} / ${FileSizeFormatter.format(totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            if (speedBytesPerSec > 0) {
                Text(
                    text = "${FileSizeFormatter.format(speedBytesPerSec)}/s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MtrxGold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
