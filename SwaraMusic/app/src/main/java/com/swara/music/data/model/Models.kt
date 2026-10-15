package com.swara.music.data.model

import org.json.JSONObject

/** Where a track comes from. Each provider has different licence permissions. */
enum class SourceType(val label: String) {
    COMMONS("Wikimedia Commons"),
    JAMENDO("Jamendo"),
    LOCAL("Your device");
}

/**
 * Licence facts we show to the user and use to decide what the app may do.
 * [offlineAllowed] is decided by the provider adapter from the licence AND the
 * provider's own terms — never by the UI.
 */
data class LicenseInfo(
    val name: String,
    val url: String?,
    val attribution: String,
    val offlineAllowed: Boolean,
    val offlineNote: String,
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val streamUrl: String,
    val artworkUrl: String?,
    val sourceType: SourceType,
    val sourcePageUrl: String?,
    val license: LicenseInfo,
    val sizeBytes: Long = 0L,
    val mimeType: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
) {
    val canDownload: Boolean get() = license.offlineAllowed && sourceType != SourceType.LOCAL

    fun toJson(): String = JSONObject().apply {
        put("id", id); put("title", title); put("artist", artist); put("album", album ?: "")
        put("durationMs", durationMs); put("streamUrl", streamUrl); put("artworkUrl", artworkUrl ?: "")
        put("sourceType", sourceType.name); put("sourcePageUrl", sourcePageUrl ?: "")
        put("licName", license.name); put("licUrl", license.url ?: ""); put("licAttr", license.attribution)
        put("licOffline", license.offlineAllowed); put("licNote", license.offlineNote)
        put("sizeBytes", sizeBytes); put("mimeType", mimeType ?: "")
        put("albumId", albumId ?: ""); put("artistId", artistId ?: "")
    }.toString()

    companion object {
        fun fromJson(json: String): Track? = runCatching {
            val o = JSONObject(json)
            fun s(k: String) = o.optString(k, "").ifBlank { null }
            Track(
                id = o.getString("id"),
                title = o.getString("title"),
                artist = o.optString("artist", "Unknown artist"),
                album = s("album"),
                durationMs = o.optLong("durationMs"),
                streamUrl = o.getString("streamUrl"),
                artworkUrl = s("artworkUrl"),
                sourceType = SourceType.valueOf(o.getString("sourceType")),
                sourcePageUrl = s("sourcePageUrl"),
                license = LicenseInfo(
                    name = o.optString("licName"),
                    url = s("licUrl"),
                    attribution = o.optString("licAttr"),
                    offlineAllowed = o.optBoolean("licOffline"),
                    offlineNote = o.optString("licNote"),
                ),
                sizeBytes = o.optLong("sizeBytes"),
                mimeType = s("mimeType"),
                albumId = s("albumId"),
                artistId = s("artistId"),
            )
        }.getOrNull()
    }
}

/** A browsable group of tracks: an album, a curated playlist, a category or an artist. */
data class Collection(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: Kind,
    val artworkUrl: String? = null,
) {
    enum class Kind { ALBUM, PLAYLIST, ARTIST, CATEGORY, USER_PLAYLIST }
}

/** Simple result wrapper used by view models to drive loading / empty / error states. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val data: T) : Loadable<T>
    data class Failed(val message: String, val offline: Boolean = false) : Loadable<Nothing>
}
