package com.swara.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swara.music.BuildConfig
import com.swara.music.container

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun H(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))

@Composable
private fun P(text: String) = Text(
    text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(bottom = 8.dp),
)

@Composable
private fun Link(label: String, url: String) {
    val uri = LocalUriHandler.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable { runCatching { uri.openUri(url) } }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SourceCard(name: String, role: String, rows: List<Pair<Boolean, String>>, note: String, link: Pair<String, String>?) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp)
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        rows.forEach { (ok, text) ->
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Block, null, Modifier.size(18.dp),
                    tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        link?.let { (l, u) -> Link(l, u) }
    }
}

@Composable
fun SourcesScreen(onBack: () -> Unit) = InfoScaffold("Music sources & licences", onBack) {
    P(
        "Swara only plays music that is legally free to stream, and only offers downloads when the licence " +
            "and the provider's terms both allow keeping a copy. Every song shows its source, licence and attribution " +
            "(tap ⋮ → Details & licence)."
    )
    SourceCard(
        "Wikimedia Commons", "Main catalogue · built in, no account needed",
        listOf(true to "Stream", true to "Download & keep offline", true to "Share, with attribution"),
        "Commons only hosts public-domain and freely licensed files (CC0, CC BY, CC BY-SA). Those licences let anyone copy " +
            "and store the files as long as the creator is credited and, for BY-SA, the same licence is kept. That is why Commons " +
            "tracks can be downloaded. Swara reads each file's licence live from Commons and hides anything without one.",
        "commons.wikimedia.org" to "https://commons.wikimedia.org/wiki/Commons:Licensing",
    )
    SourceCard(
        "Jamendo", "Optional · add your own free API key in Settings",
        listOf(true to "Stream (non-commercial use)", false to "Download in this app", true to "Credit + link to the artist page"),
        "Jamendo artists publish under Creative Commons. Jamendo's API terms allow free non-commercial apps but do not allow " +
            "apps that cache content or offer offline access, so Swara streams Jamendo tracks only. Many tracks carry NC/ND terms.",
        "Jamendo API terms" to "https://devportal.jamendo.com/api_terms_of_use",
    )
    SourceCard(
        "Your own files", "On device · imported with the system file picker",
        listOf(true to "Play offline", false to "Upload or share from Swara"),
        "Swara plays files you choose and never uploads them. You're responsible for having the rights to them.",
        null,
    )
    H("Why aren't new Bollywood hits here?")
    P(
        "Recent film songs belong to labels such as T-Series, Saregama, Sony Music and Zee Music. Services like JioSaavn, " +
            "Spotify, Gaana and YouTube license that music for their own apps, and their developer terms forbid third-party " +
            "apps from streaming or downloading it. JioSaavn's API policy, for example, grants no public access and bans " +
            "downloading and stream-ripping. No free, licensed API for mainstream Bollywood catalogues exists, so Swara does not " +
            "include them and does not scrape any service or bypass DRM."
    )
    P(
        "What you do get: Indian film and 78-rpm recordings old enough to be in the public domain, classical and devotional " +
            "recordings, and modern Bollywood-style instrumentals released under free licences."
    )
    Link("JioSaavn API policy", "https://www.jiosaavn.com/corporate/api-policy")
    H("Want real Bollywood hits in your own build?")
    P(
        "You'd need a licensed provider (a commercial music-licensing API or a direct deal with labels). Add it as a new " +
            "provider class next to CommonsProvider and set offlineAllowed only where your contract permits offline copies."
    )
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) = InfoScaffold("Privacy policy", onBack) {
    P("Last updated: 15 October 2026")
    H("Summary")
    P("Swara has no accounts, no ads, no analytics and no trackers. Your library stays on your phone.")
    H("What stays on your phone")
    P(
        "Downloaded songs, playlists, imported-file references, settings (including an optional Jamendo client ID) and a small " +
            "image/API cache. All of it is stored in the app's private storage and is deleted when you uninstall Swara. " +
            "App data is excluded from cloud backup."
    )
    H("What leaves your phone")
    P(
        "To browse, stream and download, Swara connects directly to the music providers. Like any website, they receive your IP " +
            "address, the app's user-agent and the search or file you requested. Swara has no server of its own and never sees this traffic."
    )
    Link("Wikimedia Foundation privacy policy", "https://foundation.wikimedia.org/wiki/Policy:Privacy_policy")
    Link("Jamendo privacy policy (only if you enable Jamendo)", "https://www.jamendo.com/legal/privacy")
    H("Files you import")
    P("Swara only reads the audio files you pick with Android's file picker. It never uploads, copies or shares them.")
    H("Permissions")
    P(
        "Internet and network state (to stream and to detect offline mode), foreground media playback and wake lock (to keep " +
            "music playing with the screen off), and notifications (for player controls). No location, contacts, microphone or " +
            "storage permission is requested."
    )
    H("Children")
    P("Swara collects no personal data from anyone, including children.")
    H("Contact")
    P("Questions: open an issue on the project's GitHub repository.")
    Link("github.com/amitbarnwal125/Repository1", "https://github.com/amitbarnwal125/Repository1")
}

@Composable
fun LicensesScreen(onBack: () -> Unit) = InfoScaffold("Open-source licences", onBack) {
    P("Swara's own code is released under the MIT licence. It is built with these open-source libraries:")
    listOf(
        "Jetpack Compose, Material 3, Navigation, Lifecycle, Room, Core — Apache 2.0 (Google / AOSP)",
        "AndroidX Media3 ExoPlayer & Session — Apache 2.0 (Google)",
        "OkHttp — Apache 2.0 (Square)",
        "Coil — Apache 2.0 (Coil contributors)",
        "Kotlin & kotlinx.coroutines — Apache 2.0 (JetBrains)",
        "Guava — Apache 2.0 (Google)",
    ).forEach { P("• $it") }
    Link("Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
    H("Music")
    P("Each song keeps its own licence and attribution, shown in its Details & licence sheet.")
}

@Composable
fun SettingsScreen(onBack: () -> Unit, openSources: () -> Unit, openPrivacy: () -> Unit, openLicenses: () -> Unit) {
    val c = LocalContext.current.container
    val jamendoId by c.settings.jamendoClientId.collectAsStateWithLifecycle()
    val wifiOnly by c.settings.wifiOnly.collectAsStateWithLifecycle()
    var draft by remember(jamendoId) { mutableStateOf(jamendoId.orEmpty()) }

    InfoScaffold("Settings", onBack) {
        H("Music sources")
        P("Wikimedia Commons is always on. Optionally add Jamendo for more Indian & Bollywood-tagged independent music (streaming only).")
        OutlinedTextField(
            value = draft, onValueChange = { draft = it }, singleLine = true,
            label = { Text("Jamendo client ID (optional)") },
            leadingIcon = { Icon(Icons.Rounded.Key, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            TextButton(onClick = { c.settings.setJamendoClientId(draft); c.player.message(if (draft.isBlank()) "Jamendo turned off" else "Jamendo enabled") }) { Text("Save") }
            if (!jamendoId.isNullOrBlank()) TextButton(onClick = { c.settings.setJamendoClientId(null); c.player.message("Jamendo turned off") }) { Text("Remove") }
        }
        Link("Get a free client ID at devportal.jamendo.com", "https://devportal.jamendo.com/")
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        H("Downloads")
        SettingRow(Icons.Rounded.Wifi, "Download over Wi-Fi only", "Saves mobile data") {
            Switch(checked = wifiOnly, onCheckedChange = { c.settings.setWifiOnly(it) })
        }
        SettingRow(Icons.Rounded.Storage, "Where downloads are stored", "Inside Swara's private folder. Manage them in Library → Downloads.") {}
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        H("About")
        ListItem(headlineContent = { Text("Music sources & licences") }, leadingContent = { Icon(Icons.Rounded.Gavel, null) }, modifier = Modifier.clickable(onClick = openSources))
        ListItem(headlineContent = { Text("Privacy policy") }, leadingContent = { Icon(Icons.Rounded.PrivacyTip, null) }, modifier = Modifier.clickable(onClick = openPrivacy))
        ListItem(headlineContent = { Text("Open-source licences") }, leadingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) }, modifier = Modifier.clickable(onClick = openLicenses))
        P("Swara ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { trailing() },
    )
}
