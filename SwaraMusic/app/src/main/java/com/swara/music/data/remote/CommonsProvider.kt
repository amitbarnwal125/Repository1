package com.swara.music.data.remote

import com.swara.music.data.model.LicenseInfo
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import com.swara.music.util.Format
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.json.JSONObject

/**
 * Wikimedia Commons — https://commons.wikimedia.org
 *
 * Licensing: Commons only accepts freely licensed or public-domain media
 * (CC0, CC BY, CC BY-SA, public domain, …). Those licences allow anyone to copy,
 * stream and store the files, including offline, as long as the attribution and
 * licence travel with the file. The MediaWiki API needs no key; Wikimedia asks
 * clients to send a descriptive User-Agent (see [Http.USER_AGENT]).
 */
class CommonsProvider(private val http: OkHttpClient) {

    private val api = "https://commons.wikimedia.org/w/api.php".toHttpUrl()

    private fun base(): HttpUrl.Builder = api.newBuilder()
        .addQueryParameter("action", "query")
        .addQueryParameter("format", "json")
        .addQueryParameter("formatversion", "2")
        .addQueryParameter("prop", "imageinfo")
        .addQueryParameter("iiprop", "url|size|mime|mediatype|extmetadata")
        .addQueryParameter(
            "iiextmetadatafilter",
            "LicenseShortName|LicenseUrl|Artist|Credit|ObjectName|UsageTerms|AttributionRequired"
        )

    /** Looks up specific files (used for the curated demo catalogue). Order is preserved. */
    suspend fun byTitles(titles: List<String>, album: String?): List<Track> {
        val out = mutableMapOf<String, Track>()
        titles.chunked(40).forEach { chunk ->
            val url = base().addQueryParameter("titles", chunk.joinToString("|")).build()
            val json = http.getJson(url)
            pages(json).forEach { (_, page) -> toTrack(page, album)?.let { out[it.id] = it } }
        }
        return titles.mapNotNull { out[idFor(it)] }
    }

    suspend fun search(query: String, limit: Int = 30): List<Track> {
        val url = base()
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrnamespace", "6")
            .addQueryParameter("gsrsearch", "filetype:audio $query")
            .addQueryParameter("gsrlimit", limit.toString())
            .build()
        val json = http.getJson(url)
        return pages(json).sortedBy { it.first }.mapNotNull { toTrack(it.second, null) }
    }

    suspend fun category(category: String, album: String?): List<Track> {
        val url = base()
            .addQueryParameter("generator", "categorymembers")
            .addQueryParameter("gcmtitle", "Category:$category")
            .addQueryParameter("gcmtype", "file")
            .addQueryParameter("gcmlimit", "100")
            .build()
        val json = http.getJson(url)
        return pages(json).mapNotNull { toTrack(it.second, album) }.sortedBy { it.title.lowercase() }
    }

    /** Returns (rank, page) pairs. */
    private fun pages(json: JSONObject): List<Pair<Int, JSONObject>> {
        json.optJSONObject("error")?.let { throw ApiException(it.optString("info", "Commons API error")) }
        val arr = json.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            p.optInt("index", i) to p
        }
    }

    private fun toTrack(page: JSONObject, album: String?): Track? {
        if (page.optBoolean("missing")) return null
        val title = page.optString("title")
        val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: return null
        if (info.optString("mediatype") != "AUDIO") return null
        val mime = info.optString("mime")
        if (mime.contains("midi")) return null
        val durationSec = info.optDouble("duration", 0.0)
        // Skip pronunciation clips and sound effects; we want music.
        if (durationSec in 0.01..20.0) return null
        val url = info.optString("url").ifBlank { return null }
        val meta = info.optJSONObject("extmetadata") ?: JSONObject()
        fun m(key: String) = Format.stripHtml(meta.optJSONObject(key)?.optString("value"))

        val licenseName = m("LicenseShortName")
        // No licence statement → we do not offer the track at all.
        if (licenseName.isBlank()) return null

        val fileName = title.removePrefix("File:").substringBeforeLast('.')
        val display = m("ObjectName").ifBlank { fileName }
        var artist = m("Artist")
        if (artist.isBlank() || artist.startsWith("Unknown author", ignoreCase = true)) artist = "Unknown artist"
        if (artist.length > 70) artist = artist.take(67) + "…"
        val credit = m("Credit")

        val attribution = buildString {
            append("“").append(display).append("” by ").append(artist)
            if (credit.isNotBlank() && credit.length < 140) append(" (").append(credit).append(")")
            append(", via Wikimedia Commons, licensed ").append(licenseName).append('.')
        }
        return Track(
            id = idFor(title),
            title = display,
            artist = artist,
            album = album ?: "Wikimedia Commons",
            durationMs = (durationSec * 1000).toLong(),
            streamUrl = url,
            artworkUrl = null,
            sourceType = SourceType.COMMONS,
            sourcePageUrl = info.optString("descriptionurl").ifBlank { null },
            license = LicenseInfo(
                name = licenseName,
                url = m("LicenseUrl").ifBlank { null },
                attribution = attribution,
                offlineAllowed = true,
                offlineNote = "Free licence ($licenseName): you may stream, download and keep this file. " +
                    "Keep the attribution with it if you share it.",
            ),
            sizeBytes = info.optLong("size"),
            mimeType = mime,
        )
    }

    companion object {
        fun idFor(fileTitle: String): String =
            "commons:" + fileTitle.removePrefix("File:").replace('_', ' ').trim()
    }
}
