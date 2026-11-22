package com.mtrx.store.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mtrx.store.R

object AppIconHelper {
    @DrawableRes
    fun getBundledIconRes(packageName: String?, iconUrl: String?): Int? {
        val pkg = packageName?.lowercase() ?: ""
        val url = iconUrl?.lowercase() ?: ""

        return when {
            pkg == "com.mtrx.office.text" || url.contains("document_studio") -> R.drawable.ic_app_document_studio
            pkg == "com.mtrx.office.grid" || url.contains("grid_pro") -> R.drawable.ic_app_grid_pro
            pkg == "com.mtrx.office.vision" || url.contains("vision_studio") -> R.drawable.ic_app_vision_studio
            pkg == "com.mtrx.store" || url.contains("store.png") || url.contains("/store") -> R.drawable.ic_app_store
            else -> null
        }
    }
}

@Composable
fun AppIcon(
    packageName: String?,
    iconUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bundledRes = AppIconHelper.getBundledIconRes(packageName, iconUrl)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF161720))
    ) {
        if (bundledRes != null) {
            Image(
                painter = painterResource(id = bundledRes),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        } else {
            AsyncImage(
                model = iconUrl,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        }
    }
}
