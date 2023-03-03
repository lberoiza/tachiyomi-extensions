package eu.kanade.tachiyomi.extension.en.manhwaxxl

import eu.kanade.tachiyomi.multisrc.bakamanga.BakaManga

class ManhwaXXL : BakaManga(
    "Manhwa XXL",
    "https://manhwaxxl.com",
    "en",
) {
    override fun getGenreList() = arrayOf(
        Pair("All", ""),
        Pair("Action", "action"),
        Pair("Adaptation", "adaptation"),
        Pair("Adult", "adult"),
        Pair("Adventure", "adventure"),
        Pair("BL", "bl"),
        Pair("Comedy", "comedy"),
        Pair("Cooking", "cooking"),
        Pair("Demons", "demons"),
        Pair("Drama", "drama"),
        Pair("Ecchi", "ecchi"),
        Pair("Fantasy", "fantasy"),
        Pair("Full color", "full-color"),
        Pair("Game", "game"),
        Pair("Gender Bender", "gender-bender"),
        Pair("GL", "gl"),
        Pair("Harem", "harem"),
        Pair("Historical", "historical"),
        Pair("Horror", "horror"),
        Pair("Isekai", "isekai"),
        Pair("Josei", "josei"),
        Pair("Live action", "live-action"),
        Pair("Love & Romance", "love-romance"),
        Pair("Magic", "magic"),
        Pair("Manga", "manga"),
        Pair("Manhua", "manhua"),
        Pair("Manhwa", "manhwa"),
        Pair("Martial Arts", "martial-arts"),
        Pair("Mature", "mature"),
        Pair("Mecha", "mecha"),
        Pair("Mystery", "mystery"),
        Pair("Omegaverse", "omegaverse"),
        Pair("Psychological", "psychological"),
        Pair("Raw", "raw"),
        Pair("Reincarnation", "reincarnation"),
        Pair("Romance", "romance"),
        Pair("RPG", "rpg"),
        Pair("School Life", "school-life"),
        Pair("Sci-fi", "sci-fi"),
        Pair("Seinen", "seinen"),
        Pair("Shoujo", "shoujo"),
        Pair("Shoujo Ai", "shoujo-ai"),
        Pair("Shounen", "shounen"),
        Pair("Slice of Life", "slice-of-life"),
        Pair("Smut", "smut"),
        Pair("Sports", "sports"),
        Pair("Supernatural", "supernatural"),
        Pair("Thriller", "thriller"),
        Pair("Tragedy", "tragedy"),
        Pair("Vampire", "vampire"),
        Pair("Vanilla", "vanilla"),
        Pair("Webtoon", "webtoon"),
        Pair("Webtoons", "webtoons"),
        Pair("Yaoi", "yaoi"),
        Pair("Yuri", "yuri"),
        Pair("Zombie", "zombie"),
    )
}
