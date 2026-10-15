package com.swara.music.data.remote

import com.swara.music.data.model.LicenseInfo
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * Jamendo — https://www.jamendo.com (optional, needs the user's own free client ID).
 *
 * Licensing: tracks are Creative Commons licensed by their artists. Jamendo's API
 * terms allow free *non-commercial* use with attribution and a link back, but they
 * do NOT allow apps that offer offline access to the content. So in Swara, Jamendo
 * tracks are stream-only; downloads are disabled for them regardless of the
 * `audiodownload_allowed` flag.
 */
class JamendoProvider(private val http: OkHttpClient, private val clientId: () -> String?) {

    val isConfigured: Boolean get() = !clientId().isNullOrBlank()

    private fun tracksUrl(): HttpUrl.Builder = "https://api.jamendo.com/v3.0/tracks/".toHttpUrl().newBuilder()
        .addQueryParameter("client_id", clientId().orEmpty())
        .addQueryParameter("format", "json")
        .addQueryParameter("limit", "40")
        .addQueryParameter("audioformat", "mp32")
        .addQueryParameter("imagesize", "300")
        .addQueryParameter("include", "licenses")

    /** Popular tracks tagged with Indian / Bollywood styles. */
    suspend fun popularIndian(): List<Track> = fetch(
        tracksUrl()
            .addEncodedQueryParameter("fuzzytags", "bollywood+indian+hindi+desi+bhangra+sitar")
            .addQueryParameter("order", "popularity_total")
            .build()
    )

    suspend fun search(query: String): List<Track> =
        fetch(tracksUrl().addQueryParameter("search", query).addQueryParameter("order", "relevance").build())

    suspend fun album(albumId: String): List<Track> =
        fetch(tracksUrl().addQueryParameter("album_id", albumId).addQueryParameter("order", "releasedate").build())

    suspend fun artist(artistId: String): List<Track> =
        fetch(tracksUrl().addQueryParameter("artist_id", artistId).addQueryParameter("order", "popularity_total").build())

    private suspend fun fetch(url: HttpUrl): List<Track> {
        if (!isConfigured) return emptyList()
        val json = http.getJson(url)
        val headers = json.optJSONObject("headers")
        if (headers != null && headers.optString("status") != "success") {
            throw ApiException("Jamendo: " + headers.optString("error_message", "request failed"))
        }
        val arr = json.optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val audio = o.optString("audio").ifBlank { return@mapNotNull null }
            val ccUrl = o.optString("license_ccurl").ifBlank { null }
            val licName = ccName(ccUrl)
            val title = o.optString("name")
            val artist = o.optString("artist_name", "Unknown artist")
            Track(
                id = "jamendo:" + o.optString("id"),
                title = title,
                artist = artist,
                album = o.optString("album_name").ifBlank { null },
                durationMs = o.optLong("duration") * 1000,
                streamUrl = audio,
                artworkUrl = o.optString("image").ifBlank { o.optString("album_image").ifBlank { null } },
                sourceType = SourceType.JAMENDO,
                sourcePageUrl = o.optString("shareurl").ifBlank { null },
                license = LicenseInfo(
                    name = licName,
                    url = ccUrl,
                    attribution = "“$title” by $artist, provided by Jamendo (jamendo.com), licensed $licName.",
                    offlineAllowed = false,
                    offlineNote = "Stream only. Jamendo's API terms don't allow apps to offer offline copies. " +
                        "You can get the file from the artist's Jamendo page if they allow it.",
                ),
                albumId = o.optString("album_id").ifBlank { null }?.takeIf { it != "0" },
                artistId = o.optString("artist_id").ifBlank { null },
            )
        }
    }

    private fun ccName(url: String?): String {
        if (url.isNullOrBlank()) return "Creative Commons"
        // e.g. http://creativecommons.org/licenses/by-nc-sa/3.0/
        val parts = url.substringAfter("/licenses/", "").trim('/').split('/')
        if (parts.size >= 2) return "CC " + parts[0].uppercase() + " " + parts[1]
        return "Creative Commons"
    }
}
