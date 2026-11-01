package app.tab.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Remote image with a branded placeholder and a graceful failure state. Icons are
 * square with an expressive radius; screenshots keep their aspect ratio.
 */
@Composable
fun AppImage(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    contentDescription: String? = null,
) {
    if (url.isNullOrBlank()) {
        PlaceholderIcon(modifier = modifier.size(size), shape = shape)
        return
    }
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape),
    )
}

@Composable
private fun PlaceholderIcon(modifier: Modifier, shape: RoundedCornerShape) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}
