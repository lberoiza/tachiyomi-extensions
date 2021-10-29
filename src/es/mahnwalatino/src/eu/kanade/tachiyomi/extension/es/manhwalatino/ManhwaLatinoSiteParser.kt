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

class ManhwaLatinoSiteParser(private val baseUrl: String) {

    /**
     * TODO: ADD SEARCH_TAG
     */
    enum class SearchType {
        SEARCH_FREE, SEARCH_FILTER
    }

    /**
     * Type of search ( FREE, FILTER)
     */
    var searchType = SearchType.SEARCH_FREE

    private val urlHTMLSelector: String = "a"
    private val titleHTMLSelector: String = "h3"
    private val thumbnailUrlMangaListHTMLSelector: String = "div.item-thumb.c-image-hover img"
    private val authorHTMLSelector: String = "div.author-content"
    private val artistHTMLSelector: String = "div.artist-content"
    private val descriptionHTMLSelector: String = "div.summary__content.show-more p"
    private val genreHTMLSelector: String = "div.genres-content a"
    private val statusHTMLSelector: String =
        "div.summary_content div.post-status div.post-content_item div.summary-content"
    private val thumbnailUrlMangaDetailsHTMLSelector: String = "div.summary_image img"
    private val tagsHTMLSelector: String = "div.tags-content a"
    private val searchSiteMangasHTMLSelector = "div.c-tabs-item__content"
    private val genreSiteMangasHTMLSelector = "div.page-item-detail.manga"
    private val latestUpdatesSelectorUrl = "div.slider__thumb_item a"
    private val latestUpdatesSelectorThumbnailUrl = "div.slider__thumb_item a img"
    private val latestUpdatesSelectorTitle = "div.slider__content h4"
    private val chapterListParseSelector = "li.wp-manga-chapter a"
    private val pageListParseSelector = "div.page-break.no-gaps img"

    val searchMangaNextPageSelector = "link[rel=next]"
    val latestUpdatesSelector = "div.slider__item"

    // TODO TO BE DEFINDED
    val popularMangaSelector = "div.page-item-detail.manga"
    val searchMangaSelector = "div.page-item-detail.manga"
    val popularMangaNextPageSelector = "a.nextpostslink"
    val latestUpdatesNextPageSelector = "div[role=navigation] a.last"
    //

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector).map { getMangaFromLastTranslatedSlide(it) }
        return MangasPage(mangas, false)
    }

    fun getMangaFromLastTranslatedSlide(element: Element): SManga {
        val manga = SManga.create()
        manga.url =
            getUrlWithoutDomain(element.select(latestUpdatesSelectorUrl).first().attr("abs:href"))
        manga.title = element.select(latestUpdatesSelectorTitle).text().trim()
        manga.thumbnail_url = element.select(latestUpdatesSelectorThumbnailUrl).attr("abs:data-src")
        return manga
    }

    fun latestUpdatesHasNextPages() = false

    fun getMangasFromSearchSite(document: Document): List<SManga> {
        return document.select(searchSiteMangasHTMLSelector).map { getMangaFromList(it) }
    }

    fun getMangasFromGenreSite(document: Document): List<SManga> {
        return document.select(genreSiteMangasHTMLSelector).map { getMangaFromList(it) }
    }

    fun getMangaFromList(element: Element): SManga {
        val manga = SManga.create()
        manga.url = getUrlWithoutDomain(element.select(urlHTMLSelector).first().attr("abs:href"))
        manga.title = element.select(titleHTMLSelector).text().trim()
        manga.thumbnail_url = element.select(thumbnailUrlMangaListHTMLSelector).attr("abs:data-src")
        return manga
    }

    fun getMangaDetails(document: Document): SManga {
        val manga = SManga.create()

        val descriptionList = document.select(descriptionHTMLSelector).map { it.text() }
        val author = document.select(authorHTMLSelector).text()
        val artist = document.select(artistHTMLSelector).text()

        val genrelist = document.select(genreHTMLSelector).map { it.text() }
        val tagList = document.select(tagsHTMLSelector).map { it.text() }
        val genreTagList = genrelist + tagList

        manga.thumbnail_url = document.select(thumbnailUrlMangaDetailsHTMLSelector).attr("abs:data-src")
        manga.description = descriptionList.joinToString("\n")
        manga.author = if (author.isBlank()) "Autor Desconocido" else author
        manga.artist = artist
        manga.genre = genreTagList.joinToString(", ")
        manga.status = findMangaStatus(tagList, document)
        return manga
    }

    private fun findMangaStatus(tagList: List<String>, document: Document): Int {
        return if (tagList.contains("Fin")) {
            SManga.COMPLETED
        } else {
            when (document.select(statusHTMLSelector)?.first()?.text()?.trim()) {
                "Publicandose" -> SManga.ONGOING
                else -> SManga.UNKNOWN
            }
        }
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    fun getChapterListParse(response: Response): List<SChapter> {
        return response.asJsoup().select(chapterListParseSelector).map { element ->
            SChapter.create().apply {
                name = element.text()
                url = getUrlWithoutDomain(element.attr("abs:href"))
            }
        }
    }

    /**
     * Parses the response from the site and returns the page list.
     * (Parse the comic pages from the website with the chapter)
     *
     * @param response the response from the site.
     */
    fun getPageListParse(response: Response): List<Page> {
        val list = response.asJsoup().select(pageListParseSelector).mapIndexed { index, imgElement ->
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

    fun hasNextPages(document: Document): Boolean {
        return !document.select(searchMangaNextPageSelector).isEmpty()
    }

    protected fun getUrlWithoutDomain(url: String) = url.substringAfter(baseUrl)
}
