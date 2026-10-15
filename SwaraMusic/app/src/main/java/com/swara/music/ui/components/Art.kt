package com.swara.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.swara.music.data.model.Collection
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import com.swara.music.ui.theme.ArtPalettes
import kotlin.math.abs

@Composable
fun GradientArt(seed: String, icon: ImageVector, size: Dp, corner: Dp, modifier: Modifier = Modifier) {
    val colors = ArtPalettes[abs(seed.hashCode()) % ArtPalettes.size]
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
fun TrackArt(track: Track?, size: Dp, corner: Dp = 10.dp, modifier: Modifier = Modifier) {
    val icon = when (track?.sourceType) {
        SourceType.LOCAL -> Icons.Rounded.PhoneAndroid
        null -> Icons.Rounded.GraphicEq
        else -> Icons.Rounded.MusicNote
    }
    val seed = track?.album ?: track?.title ?: "swara"
    val url = track?.artworkUrl
    if (url == null) {
        GradientArt(seed, icon, size, corner, modifier)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(RoundedCornerShape(corner)),
            loading = { GradientArt(seed, icon, size, corner) },
            error = { GradientArt(seed, icon, size, corner) },
        )
    }
}

@Composable
fun CollectionArt(collection: Collection, size: Dp, corner: Dp = 14.dp, modifier: Modifier = Modifier) {
    val icon = when (collection.kind) {
        Collection.Kind.ARTIST -> Icons.Rounded.Person
        Collection.Kind.ALBUM, Collection.Kind.CATEGORY -> Icons.Rounded.Album
        else -> Icons.Rounded.LibraryMusic
    }
    if (collection.artworkUrl == null) {
        GradientArt(collection.title, icon, size, corner, modifier)
    } else {
        SubcomposeAsyncImage(
            model = collection.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(RoundedCornerShape(corner)),
            loading = { Box(Modifier.fillMaxSize()) { GradientArt(collection.title, icon, size, corner) } },
            error = { GradientArt(collection.title, icon, size, corner) },
        )
    }
}
