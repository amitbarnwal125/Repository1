package com.swara.music.util

import android.text.Html
import java.util.Locale

object Format {
    fun duration(ms: Long): String {
        if (ms <= 0) return "--:--"
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun bytes(b: Long): String {
        if (b <= 0) return "0 MB"
        val kb = b / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    private val hiddenSpan = Regex("<span[^>]*display:\\s*none[^>]*>.*?</span>", RegexOption.IGNORE_CASE)
    private val linkTag = Regex("<link[^>]*>", RegexOption.IGNORE_CASE)
    private val whitespace = Regex("\\s+")

    /** Turns the HTML snippets Wikimedia returns into plain one-line text. */
    @Suppress("DEPRECATION")
    fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val cleaned = html.replace(hiddenSpan, "").replace(linkTag, "")
        val text = if (android.os.Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY).toString()
        } else Html.fromHtml(cleaned).toString()
        return text.replace(whitespace, " ").trim()
    }
}
