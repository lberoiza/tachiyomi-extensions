package eu.kanade.tachiyomi.extension.es.manhwalatino

import android.net.Uri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ManhwaLatino : ParsedHttpSource() {

    /**
     * Name of the source.
     */
    override val name = "Manhwa-Latino"

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    override val baseUrl = "https://manhwa-latino.com/"

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang = "es"

    /**
     * Whether the source has support for latest updates.
     */
    override val supportsLatest = false

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    override fun popularMangaSelector() = "div.page-item-detail.manga"

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    override fun latestUpdatesSelector() = popularMangaSelector()

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    override fun searchMangaSelector() = "div.page-item-detail.manga"

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each chapter.
     */
    override fun chapterListSelector() = throw Exception("Not Used")

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    override fun popularMangaNextPageSelector() = "a.nextpostslink"

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    override fun latestUpdatesNextPageSelector() = "div[role=navigation] a.last"

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()

    /**
     * Returns the request for the popular manga given the page.
     *
     * @param page the page number to retrieve.
     */
    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/page/$page/", headers)

    /**
     * Returns the request for latest manga given the page.
     *
     * @param page the page number to retrieve.
     */
    override fun latestUpdatesRequest(page: Int) =
        GET("$baseUrl/page/$page/", headers)

    /**
     * Returns the request for the search manga given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        println("====searchMangaRequest====")
        println("page: $page")
        println("query: $query")
        println("filters: $filters")
        println("==== ================ ====")

        val uri = if (query.isNotBlank()) {
            println("== QUERY NOT BLANK  ==")
            Uri.parse(baseUrl).buildUpon()
                .appendQueryParameter("s", query)
                .appendQueryParameter("post_type", "wp-manga")
        } else {
            println("== QUERY BLANK  ==")
            val uri = Uri.parse("$baseUrl/?s=&post_type=wp-manga").buildUpon()
            // Append uri filters
            filters.forEach {
                if (it is UriFilter)
                    it.addToUri(uri)
            }
            uri.appendQueryParameter("p", page.toString())
        }

        println("============")
        println("Uri: $uri")

        return GET(uri.toString(), headers)
    }

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    override fun searchMangaParse(response: Response): MangasPage {
        println("== searchMangaParse ==")
        val document = response.asJsoup()
        val mangas = document.select("div.c-tabs-item__content").map { getSmanga(it) }
        val hasNextPages = document.select("#navigation-ajax").hasText()
        return MangasPage(mangas, hasNextPages)
    }

    private fun getSmanga(element: Element): SManga {
        val manga = SManga.create()
        manga.setUrlWithoutDomain(element.select("div.tab-summary .post-title a").attr("abs:href"))
        manga.title = element.select("div.tab-summary .post-title a").text().trim()
        manga.thumbnail_url = element.select("div.tab-thumb img").attr("abs:data-src")
        return manga
    }

//    /**
//     * Returns the request for the details of a manga. Override only if it's needed to change the
//     * url, send different headers or request method like POST.
//     *
//     * @param manga the manga to be updated.
//     */
//    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl + manga.url, headers)

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param manga the manga to look for chapters.
     */
    override fun chapterListRequest(manga: SManga): Request {
        println("=== chapterListRequest ===")
        println("manga.author: ${manga.author}")
        println("manga.artist: ${manga.artist}")
        println("manga.description: ${manga.description}")
        println("manga.genre: ${manga.genre}")
        println("manga.initialized: ${manga.initialized}")
        println("manga.status: ${manga.status}")
        println("manga.thumbnail_url: ${manga.thumbnail_url}")
        println("manga.url: ${manga.url}")
        return GET(baseUrl + manga.url, headers)
    }

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    override fun latestUpdatesParse(response: Response): MangasPage {
        println("latestUpdatesParse")
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector())
            .distinctBy { it.select("a").first().attr("abs:href") }
            .map { latestUpdatesFromElement(it) }
        val hasNextPage = latestUpdatesNextPageSelector().let { selector ->
            document.select(selector).first()
        } != null
        return MangasPage(mangas, hasNextPage)
    }

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [popularMangaSelector].
     */
    override fun popularMangaFromElement(element: Element) = mangaFromElement(element)

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [latestUpdatesSelector].
     */
    override fun latestUpdatesFromElement(element: Element): SManga {
        println("== latestUpdatesFromElement ==")

        val manga = SManga.create()
        manga.setUrlWithoutDomain(element.select("a").first().attr("abs:href"))
        manga.title = element.select("a").first().text().trim()
        return manga
    }

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     */
    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     */
    private fun mangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.setUrlWithoutDomain(element.select("a").first().attr("abs:href"))
        manga.title = element.select("h3").text().trim()
        manga.thumbnail_url = element.select("div.item-thumb.c-image-hover img").attr("abs:data-src")
        println("=======")
        println("mangaFromElement")
        println("Title: ${manga.title}")
        println("thumbnail_url: ${manga.thumbnail_url}")
        println("=======")
        return manga
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    override fun chapterListParse(response: Response): List<SChapter> {
        println("chapterListParse")

        return response.asJsoup().select("li.wp-manga-chapter a").map { element ->
            SChapter.create().apply {
                name = element.text()
                setUrlWithoutDomain(element.attr("abs:href"))
            }
        }
    }

//    private fun parseDate(date: String): Long {
//        return SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.US).parse(date)?.time ?: 0
//    }

    /**
     * Returns a chapter from the given element.
     *
     * @param element an element obtained from [chapterListSelector].
     */
    override fun chapterFromElement(element: Element) = throw Exception("Not used")

    /**
     * Returns the details of the manga from the given [document].
     *
     * @param document the parsed document.
     */
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        val tags = document.select("div.tags-content").text()
        val descriptionList = document.select("div.summary__content.show-more p").map { it.text() }
        val autor = document.select("div.author-content").text()
        val artist = document.select("div.artist-content").text()
        val genrelist = document.select(".genres-content a").map { it.text() }
        val statusQuery = "div.summary_content div.post-status div.post-content_item div.summary-content"

        manga.thumbnail_url = document.select(".summary_image img").attr("abs:data-src")
        manga.description = "Tags: $tags\n\n" + descriptionList.joinToString("\n")
        manga.author = if (autor.isBlank()) "Sin informacion del autor" else autor
        manga.artist = if (artist.isBlank()) "Sin informacion del artista" else artist
        manga.genre = genrelist.joinToString(", ")
        manga.status = when (document.select(statusQuery)?.first()?.text()?.trim()) {
            "Publicandose" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        return manga
    }

    /**
     * Returns the request for getting the page list. Override only if it's needed to override the
     * url, send different headers or request method like POST.
     * (Request to Webseite with comic)
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    override fun pageListRequest(chapter: SChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    /**
     * Parses the response from the site and returns the page list.
     * (Parse the comic pages from the website with the chapter)
     *
     * @param response the response from the site.
     */
    override fun pageListParse(response: Response): List<Page> {
        return response.asJsoup().select("div.page-break.no-gaps img").mapIndexed {
            index, imgElement ->
            Page(index, "", imgElement.attr("abs:data-src"))
        }
    }

    /**
     * Returns a page list from the given document.
     *
     * @param document the parsed document.
     */
    override fun pageListParse(document: Document) = throw Exception("Not Used")

    /**
     * Parse the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    override fun imageUrlParse(document: Document) = throw Exception("Not Used")

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList() = FilterList(
        Filter.Header("NOTA: ¡La búsqueda de títulos no funciona!"), // "Title search not working"
        Filter.Separator(),
        GenreFilter(),
        LetterFilter(),
        StatusFilter(),
        SortFilter()
    )

    class GenreFilter : UriPartFilter(
        "Género",
        "genero",
        arrayOf(
            Pair("all", "All"),
            Pair("1", "Ahegao"),
            Pair("379", "Alien"),
            Pair("2", "Anal"),
            Pair("490", "Android18"),
            Pair("717", "Angel"),
            Pair("633", "Asphyxiation"),
            Pair("237", "Bandages"),
            Pair("77", "Bbw"),
            Pair("143", "Bdsm"),
            Pair("23", "Blackmail"),
            Pair("113", "Blindfold"),
            Pair("24", "Blowjob"),
            Pair("166", "Blowjobface"),
            Pair("25", "Body Writing"),
            Pair("314", "Bodymodification"),
            Pair("806", "Bodystocking"),
            Pair("366", "Bodysuit"),
            Pair("419", "Bodyswap"),
            Pair("325", "Bodywriting"),
            Pair("5", "Bondage"),
            Pair("51", "Bukkake"),
            Pair("410", "Catgirl"),
            Pair("61", "Chastitybelt"),
            Pair("78", "Cheating"),
            Pair("293", "Cheerleader"),
            Pair("62", "Collar"),
            Pair("120", "Compilation"),
            Pair("74", "Condom"),
            Pair("63", "Corruption"),
            Pair("191", "Corset"),
            Pair("234", "Cosplaying"),
            Pair("389", "Cowgirl"),
            Pair("256", "Crossdressing"),
            Pair("179", "Crotchtattoo"),
            Pair("689", "Crown"),
            Pair("733", "Cumflation"),
            Pair("385", "Cumswap"),
            Pair("251", "Cunnilingus"),
            Pair("75", "Darkskin"),
            Pair("180", "Daughter"),
            Pair("52", "Deepthroat"),
            Pair("28", "Defloration"),
            Pair("198", "Demon"),
            Pair("145", "Demongirl"),
            Pair("64", "Drugs"),
            Pair("95", "Drunk"),
            Pair("462", "Femalesonly"),
            Pair("82", "Femdom"),
            Pair("139", "Ffmthreesome"),
            Pair("823", "Fftthreesome"),
            Pair("55", "Full Color"),
            Pair("181", "Fullbodytattoo"),
            Pair("203", "Fullcensorship"),
            Pair("111", "Fullcolor"),
            Pair("114", "Gag"),
            Pair("3", "Glasses"),
            Pair("515", "Gloryhole"),
            Pair("116", "Humanpet"),
            Pair("32", "Humiliation"),
            Pair("147", "Latex"),
            Pair("12", "Maid"),
            Pair("4", "Milf"),
            Pair("245", "Military"),
            Pair("414", "Milking"),
            Pair("34", "Mind Control"),
            Pair("68", "Mindbreak"),
            Pair("124", "Mindcontrol"),
            Pair("645", "Nun"),
            Pair("312", "Nurse"),
            Pair("272", "Robot"),
            Pair("7", "Romance"),
            Pair("761", "Sundress"),
            Pair("412", "Tailplug"),
            Pair("253", "Tutor"),
            Pair("259", "Twins"),
            Pair("207", "Twintails"),
            Pair("840", "Valkyrie"),
            Pair("530", "Vampire"),
            Pair("16", "Yuri"),
            Pair("273", "Zombie")
        )
    )

    class LetterFilter : UriPartFilter(
        "Letra",
        "letra",
        arrayOf(
            Pair("all", "All"),
            Pair("a", "A"),
            Pair("b", "B"),
            Pair("c", "C"),
            Pair("d", "D"),
            Pair("e", "E"),
            Pair("f", "F"),
            Pair("g", "G"),
            Pair("h", "H"),
            Pair("i", "I"),
            Pair("j", "J"),
            Pair("k", "K"),
            Pair("l", "L"),
            Pair("m", "M"),
            Pair("n", "N"),
            Pair("o", "O"),
            Pair("p", "P"),
            Pair("q", "Q"),
            Pair("r", "R"),
            Pair("s", "S"),
            Pair("t", "T"),
            Pair("u", "U"),
            Pair("v", "V"),
            Pair("w", "W"),
            Pair("x", "X"),
            Pair("y", "Y"),
            Pair("z", "Z")
        )
    )

    class StatusFilter : UriPartFilter(
        "Estado",
        "estado",
        arrayOf(
            Pair("all", "All"),
            Pair("1", "En desarrollo"),
            Pair("0", "Finalizado")
        )
    )

    class SortFilter : UriPartFilterreq(
        "Sort",
        "orden",
        arrayOf(
            Pair("visitas", "Visitas"),
            Pair("desc", "Descendente"),
            Pair("asc", "Ascendente"),
            Pair("lanzamiento", "Lanzamiento"),
            Pair("nombre", "Nombre")
        )
    )

    /**
     * Class that creates a select filter. Each entry in the dropdown has a name and a display name.
     * If an entry is selected it is appended as a query parameter onto the end of the URI.
     * If `firstIsUnspecified` is set to true, if the first entry is selected, nothing will be appended on the the URI.
     */
    // vals: <name, display>
    open class UriPartFilter(
        displayName: String,
        private val uriParam: String,
        private val vals: Array<Pair<String, String>>,
        private val firstIsUnspecified: Boolean = true,
        defaultValue: Int = 0
    ) :
        Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray(), defaultValue),
        UriFilter {
        override fun addToUri(uri: Uri.Builder) {
            if (state != 0 || !firstIsUnspecified)
                uri.appendQueryParameter(uriParam, vals[state].first)
        }
    }

    open class UriPartFilterreq(
        displayName: String,
        private val uriParam: String,
        private val vals: Array<Pair<String, String>>
    ) :
        Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray()), UriFilter {
        override fun addToUri(uri: Uri.Builder) {
            uri.appendQueryParameter(uriParam, vals[state].first)
        }
    }

    /**
     * Represents a filter that is able to modify a URI.
     */
    private interface UriFilter {
        fun addToUri(uri: Uri.Builder)
    }
}
