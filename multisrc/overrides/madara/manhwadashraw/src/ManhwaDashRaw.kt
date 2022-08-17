package eu.kanade.tachiyomi.extension.all.manhwadashraw

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class ManhwaDashRaw : Madara(
    "Manhwa-raw",
    "https://manhwa-raw.com",
    "all",
    dateFormat = SimpleDateFormat("dd/MM/yyy", Locale.ROOT)
) {
    override val useNewChapterEndpoint = true
}
