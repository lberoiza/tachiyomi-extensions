package eu.kanade.tachiyomi.extension.es.manhwalatino

import android.net.Uri
import eu.kanade.tachiyomi.extension.es.manhwalatino.filters.UriFilter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManhwaLatinoSiteParser(private val baseUrl: String) {

    /**
     * TODO: ADD SEARCH_TAG
     */
    enum class SearchType {
        SEARCH_FREE, SEARCH_FILTER
    }

    val searchMangaNextPageSelector = "link[rel=next]"
    val latestUpdatesSelector = "div.slider__item"
    val searchMangaSelector = "div.page-item-detail.manga"
    val popularMangaNextPageSelector = "a.nextpostslink"
    val latestUpdatesNextPageSelector = "div[role=navigation] a.last"

    val popularMangaSelector = "div.page-item-detail.manga"

    private val popularGenreTitleHTMLSelector: String = "div.item-summary div.post-title h3"
    private val popularGenreUrlHTMLSelector: String = "div.item-summary div.post-title h3 a"
    private val popularGenreThumbnailUrlMangaHTMLSelector: String = "div.item-thumb.c-image-hover img"

    private val searchPageTitleHTMLSelector: String = "div.tab-summary div.post-title h3"
    private val searchPageUrlHTMLSelector: String = "div.tab-summary div.post-title h3 a"
    private val searchPageThumbnailUrlMangaHTMLSelector: String = "div.tab-thumb.c-image-hover img"

    private val mangaDetailsThumbnailUrlHTMLSelector: String = "div.summary_image img"
    private val mangaDetailsAuthorHTMLSelector: String = "div.author-content"
    private val mangaDetailsArtistHTMLSelector: String = "div.artist-content"
    private val mangaDetailsDescriptionHTMLSelector: String = "div.post-content_item > div > p"
    private val mangaDetailsGenreHTMLSelector: String = "div.genres-content a"
    private val mangaDetailsTagsHTMLSelector: String = "div.tags-content a"
    private val mangaDetailsAttributes: String = "div.summary_content div.post-content_item"
    private val searchSiteMangasHTMLSelector = "div.c-tabs-item__content"
    private val genreSiteMangasHTMLSelector = "div.page-item-detail.manga"
    private val latestUpdatesSelectorUrl = "div.slider__thumb_item > a"
    private val latestUpdatesSelectorThumbnailUrl = "div.slider__thumb_item > a > img"
    private val latestUpdatesSelectorTitle = "div.slider__content h4"
    private val chapterListParseSelector = "li.wp-manga-chapter"
    private val chapterLinkParser = "a"
    private val chapterReleaseDateLinkParser = "span.chapter-release-date a"
    private val chapterReleaseDateIParser = "span.chapter-release-date i"
    private val pageListParseSelector = "div.page-break.no-gaps img"

    /**
     * Type of search ( FREE, FILTER)
     */
    private var searchType = SearchType.SEARCH_FREE

    /**
     * The Latest Updates are in a Slider, this Methods get a Manga from the slide
     */
    fun getMangaFromLastTranslatedSlide(element: Element): SManga {
        val manga = SManga.create()
        manga.url =
            getUrlWithoutDomain(element.select(latestUpdatesSelectorUrl).first().attr("abs:href"))
        manga.title = element.select(latestUpdatesSelectorTitle).text().trim()
        manga.thumbnail_url = element.select(latestUpdatesSelectorThumbnailUrl).attr("abs:src").replace("//", "/")
        return manga
    }

    /**
     * The Latest Updates has only one site
     */
    fun latestUpdatesHasNextPages() = false

    /**
     * Get eine Liste mit Mangas from Search Site
     */
    fun getMangasFromSearchSite(document: Document): List<SManga> {
        return document.select(searchSiteMangasHTMLSelector).map {
            val manga = SManga.create()
            manga.url = getUrlWithoutDomain(it.select(searchPageUrlHTMLSelector).attr("abs:href"))
            manga.title = it.select(searchPageTitleHTMLSelector).text().trim()
            manga.thumbnail_url = it.select(searchPageThumbnailUrlMangaHTMLSelector).attr("abs:data-src")
            manga
        }
    }

    /**
     * Get eine Liste mit Mangas from Genre Site
     */
    fun getMangasFromGenreSite(document: Document): List<SManga> {
        return document.select(genreSiteMangasHTMLSelector).map { getMangaFromList(it) }
    }

    /**
     * Parse The Information from Mangas From Popular or Genre Site
     * Title, Address and thumbnail_url
     */
    fun getMangaFromList(element: Element): SManga {
        val manga = SManga.create()
        manga.url = getUrlWithoutDomain(element.select(popularGenreUrlHTMLSelector).attr("abs:href"))
        manga.title = element.select(popularGenreTitleHTMLSelector).text().trim()
        manga.thumbnail_url = element.select(popularGenreThumbnailUrlMangaHTMLSelector).attr("abs:data-src")
        return manga
    }

    /**
     * Get The Details of a Manga Main Website
     * Description, genre, tags, picture (thumbnail_url)
     * status...
     */
    fun getMangaDetails(document: Document): SManga {
        val manga = SManga.create()

        val descriptionList = document.select(mangaDetailsDescriptionHTMLSelector).map { it.text() }
        val author = document.select(mangaDetailsAuthorHTMLSelector).text()
        val artist = document.select(mangaDetailsArtistHTMLSelector).text()

        val genrelist = document.select(mangaDetailsGenreHTMLSelector).map { it.text() }
        val tagList = document.select(mangaDetailsTagsHTMLSelector).map { it.text() }
        val genreTagList = genrelist + tagList

        manga.thumbnail_url =
            document.select(mangaDetailsThumbnailUrlHTMLSelector).attr("abs:data-src")
        manga.description = descriptionList.joinToString("\n")
        manga.author = if (author.isBlank()) "Autor Desconocido" else author
        manga.artist = artist
        manga.genre = genreTagList.joinToString(", ")
        manga.status = findMangaStatus(tagList, document.select(mangaDetailsAttributes))
        return manga
    }

    private fun findMangaStatus(tagList: List<String>, elements: Elements): Int {
        if (tagList.contains("Fin")) {
            return SManga.COMPLETED
        }
        elements?.forEach { element ->
            val key = element.select("summary-heading h5")?.text()?.trim()
            val value = element.select("summary-content")?.text()?.trim()

            if (key == "Estado") {
                return when (value) {
                    "Publicandose" -> SManga.ONGOING
                    else -> SManga.UNKNOWN
                }
            }
        }
        return SManga.UNKNOWN
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    fun getChapterListParse(response: Response): List<SChapter> {
        return response.asJsoup().select(chapterListParseSelector).map { element ->
            // Link to the Chapter with the info (address and chapter title)
            val chapterInfo = element.select(chapterLinkParser)
            // Chaptername
            val chapterName = chapterInfo.text().trim()
            // release date came as text with format dd/mm/yyyy from a link or <i>dd/mm/yyyy</i>
            val chapterReleaseDate = getChapterReleaseDate(element)
            SChapter.create().apply {
                name = chapterName
                chapter_number = getChapterNumber(chapterName)
                url = getUrlWithoutDomain(chapterInfo.attr("abs:href"))
                date_upload = parseChapterReleaseDate(chapterReleaseDate)
            }
        }
    }

    /**
     * Get the number of Chapter from Chaptername
     */
    private fun getChapterNumber(chapterName: String): Float =
        Regex("""\d+""").find(chapterName)?.value.toString().trim().toFloat()

    /**
     * Get The String with the information about the Release date of the Chapter
     */
    private fun getChapterReleaseDate(element: Element): String {
        val chapterReleaseDateLink = element.select(chapterReleaseDateLinkParser).attr("title")
        val chapterReleaseDateI = element.select(chapterReleaseDateIParser).text()
        return when {
            chapterReleaseDateLink.isNotEmpty() -> chapterReleaseDateLink
            chapterReleaseDateI.isNotEmpty() -> chapterReleaseDateI
            else -> ""
        }
    }

    /**
     * Transform String with the Date of Release into Long format
     */
    private fun parseChapterReleaseDate(releaseDateStr: String): Long {
        val regExSecs = Regex("""hace\s+(\d+)\s+segundos?""")
        val regExMins = Regex("""hace\s+(\d+)\s+mins?""")
        val regExHours = Regex("""hace\s+(\d+)\s+horas?""")
        val regExDays = Regex("""hace\s+(\d+)\s+días?""")
        val regExDate = Regex("""\d+/\d+/\d+""")

        return when {
            regExSecs.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.SECOND)

            regExMins.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.MINUTE)

            regExHours.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.HOUR)

            regExDays.containsMatchIn(releaseDateStr) ->
                getReleaseTime(releaseDateStr, Calendar.DAY_OF_YEAR)

            regExDate.containsMatchIn(releaseDateStr) ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(releaseDateStr).time

            else -> 0
        }
    }

    /**
     * Extract the Release time from a Text String
     * Format of the String "hace\s+\d+\s(segundo|minuto|hora|dia)s?"
     */
    private fun getReleaseTime(releaseDateStr: String, timeType: Int): Long {
        val releaseTimeAgo = Regex("""\d+""").find(releaseDateStr)?.value.toString().toInt()
        val calendar = Calendar.getInstance()
        calendar.add(timeType, -releaseTimeAgo)
        return calendar.timeInMillis
    }

    /**
     * Parses the response from the site and returns the page list.
     * (Parse the comic pages from the website with the chapter)
     *
     * @param response the response from the site.
     */
    fun getPageListParse(response: Response): List<Page> {
        val list =
            response.asJsoup().select(pageListParseSelector).mapIndexed { index, imgElement ->
                Page(index, "", imgElement.attr("abs:data-src"))
            }
        return list
    }

    /**
     * Returns the request for the search manga given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    fun searchMangaRequest(page: Int, query: String, filters: FilterList): Uri.Builder {
        val uri = Uri.parse(baseUrl).buildUpon()
        if (query.isNotBlank()) {
            searchType = SearchType.SEARCH_FREE
            uri.appendQueryParameter("s", query)
                .appendQueryParameter("post_type", "wp-manga")
        } else {
            searchType = SearchType.SEARCH_FILTER
            // Append uri filters
            filters.forEach {
                if (it is UriFilter)
                    it.addToUri(uri)
            }
            uri.appendPath("page").appendPath(page.toString())
        }
        return uri
    }

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val hasNextPages = hasNextPages(document)
        val mangas: List<SManga>

        when (searchType) {
            SearchType.SEARCH_FREE ->
                mangas = getMangasFromSearchSite(document)
            SearchType.SEARCH_FILTER ->
                mangas = getMangasFromGenreSite(document)
        }

        return MangasPage(mangas, hasNextPages)
    }

    /**
     * Check if there ir another page to show
     */
    fun hasNextPages(document: Document): Boolean {
        return !document.select(searchMangaNextPageSelector).isEmpty()
    }

    /**
     * Create a Address url without the base url.
     */
    protected fun getUrlWithoutDomain(url: String) = url.substringAfter(baseUrl)
}
