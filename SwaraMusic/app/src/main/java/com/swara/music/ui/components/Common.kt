package com.swara.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swara.music.data.model.Collection
import com.swara.music.data.model.Track

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: Pair<String, () -> Unit>? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}

@Composable
fun TrackCard(track: Track, onClick: () -> Unit, width: Dp = 148.dp) {
    Column(Modifier.width(width).clickable(onClick = onClick).padding(4.dp)) {
        TrackArt(track, width - 8.dp, corner = 16.dp)
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            track.artist, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CollectionCard(collection: Collection, onClick: () -> Unit, width: Dp = 148.dp) {
    Column(Modifier.width(width).clickable(onClick = onClick).padding(4.dp)) {
        CollectionArt(collection, width - 8.dp, corner = if (collection.kind == Collection.Kind.ARTIST) (width / 2) else 16.dp)
        Spacer(Modifier.height(8.dp))
        Text(collection.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            collection.subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}
