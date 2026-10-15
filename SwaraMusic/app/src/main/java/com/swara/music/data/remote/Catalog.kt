package com.swara.music.data.remote

/**
 * The demo catalogue. Every entry is a file on Wikimedia Commons that is either in
 * the public domain or under a free licence (CC0 / CC BY / CC BY-SA), checked on
 * 15 Oct 2026. The app re-reads the licence live from Commons before showing a
 * track, so if a file is ever deleted or relicensed it simply disappears.
 */
object Catalog {

    data class CuratedPlaylist(
        val id: String,
        val title: String,
        val subtitle: String,
        val files: List<String>,
    )

    data class CommonsCategory(val category: String, val title: String, val subtitle: String)

    val featured = listOf(
        "File:Zindagi KoSanwarna HogaIndia.ogg",
        "File:Happy sitar (Antti Luode).mp3",
        "File:Binbataye Hindi pop.oga",
        "File:Naraina (ISRC USUAN1400053).mp3",
        "File:Ustad Abdul Karim Khan - Raag Jhinjhoti.ogg",
        "File:Aarti Puja, Ganpati Vandana.ogg",
        "File:Upbeat Sitar (Antti Luode).mp3",
        "File:Gokul Gaav Ke Choriwa (1904).ogg",
        "File:Sohini Qawwali.ogg",
        "File:Living in a Sitar Nation (Antti Luode).mp3",
    )

    val playlists = listOf(
        CuratedPlaylist(
            id = "golden",
            title = "Golden-Era Film & Gramophone",
            subtitle = "Public-domain Indian film and 78-rpm recordings, 1904–1950",
            files = listOf(
                "File:Zindagi KoSanwarna HogaIndia.ogg",
                "File:Bandhan, 1940, film sound track.ogg",
                "File:Babul (1950), sound track.opus",
                "File:Mayameya Jagambe from Telugu movie Harischandra (1935).opus",
                "File:Gokul Gaav Ke Choriwa (1904).ogg",
                "File:Gazal (1910).ogg",
                "File:Jevi Karay Je Karni, Tevi Tarat Faday Chhe (c. 1928).ogg",
                "File:Sohini Qawwali.ogg",
                "File:Art song from Meerut Raga Bhairavi (1931).ogg",
                "File:Art-song from Jummoo Raga Bilaval (1931).ogg",
                "File:Instrumental music by P.S. Mukherjee of Cawanpur(Kanpur) - Side 2.ogg",
            ),
        ),
        CuratedPlaylist(
            id = "grooves",
            title = "Sitar Grooves",
            subtitle = "Modern Bollywood-style instrumentals, CC BY",
            files = listOf(
                "File:Happy sitar (Antti Luode).mp3",
                "File:Upbeat Sitar (Antti Luode).mp3",
                "File:Living in a Sitar Nation (Antti Luode).mp3",
                "File:Veena and Sitar (Antti Luode).mp3",
                "File:Sarod And Roll (Antti Luode).mp3",
                "File:Veena And Shenai (Antti Luode).mp3",
                "File:Sitar and simple beats (Antti Luode).mp3",
                "File:Relax Ay Sitar (Antti Luode).mp3",
                "File:Naraina (ISRC USUAN1400053).mp3",
                "File:Mystery Bazaar (ISRC USUAN1700005).mp3",
                "File:Binbataye Hindi pop.oga",
                "File:12 - Yasuke Mazda Plays Tabla with 8-bit Raga Generator Varanasi.ogg",
            ),
        ),
        CuratedPlaylist(
            id = "ragas",
            title = "Raga Mornings",
            subtitle = "Hindustani & Carnatic classics",
            files = listOf(
                "File:Ustad Abdul Karim Khan - Raag Jhinjhoti.ogg",
                "File:Raga Bahar.ogg",
                "File:Raga Malkounsa.ogg",
                "File:Raga Durga.ogg",
                "File:Raga Tilanga.ogg",
                "File:Raga Bhairava.ogg",
                "File:Sitar sample yaman.ogg",
                "File:RanjitMakkuniRagaKaushiKanra.ogg",
                "File:Shri Nilotpala Nayike, rendered on the Veena by L Ramakishnan.ogg",
                "File:Veena Kinhal - 01 - Haratanaya Sree.ogg",
                "File:Vande Mataram on Mohan Veena.ogg",
            ),
        ),
        CuratedPlaylist(
            id = "bhakti",
            title = "Bhakti & Folk",
            subtitle = "Aarti, bhajan, dhol and harmonium",
            files = listOf(
                "File:Aarti Puja, Ganpati Vandana.ogg",
                "File:Aarti Puja, Lakshmi Laxmi.ogg",
                "File:Aarti Puja, deity Santoshi.ogg",
                "File:Guru tum na mile.ogg",
                "File:Kirtana in Hindi.ogg",
                "File:Nand singing.ogg",
                "File:Niraas Nahi Hone Dega - Purvanchali-Bhojpuri-Awadhi Bhajan.ogg",
                "File:Dhol Thap Punjab Folk.ogg",
                "File:Maharashtrian Dhol Perform 02.ogg",
                "File:Samuel Corwin - 06 - Harmonium and Drum at the Karni Mata Temple Deshnoke.ogg",
                "File:Tabla drums demo.oga",
            ),
        ),
    )

    /** Commons categories shown as "albums" — browsed live, so they grow as people upload. */
    val categories = listOf(
        CommonsCategory("Hindi-language songs", "Hindi Songs", "Freely licensed Hindi recordings"),
        CommonsCategory("Audio files by Antti Luode", "Antti Luode Collection", "Sitar, veena & beats · CC BY"),
        CommonsCategory("Audio files of Hindustani music", "Hindustani Music", "Ragas, sitar and vocals"),
        CommonsCategory("Audio files of Bengali music", "Bengali Songs", "Songs and instrumentals"),
        CommonsCategory("Audio files of Rabindra Sangeet", "Rabindra Sangeet", "Tagore's songs, freely licensed"),
        CommonsCategory("Audio files of Carnatic music", "Carnatic Music", "South Indian classical"),
    )
}
