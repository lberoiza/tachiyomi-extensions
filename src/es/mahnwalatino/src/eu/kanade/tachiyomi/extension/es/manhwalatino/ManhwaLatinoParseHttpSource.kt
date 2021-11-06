package eu.kanade.tachiyomi.extension.es.manhwalatino

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.random.Random

abstract class ManhwaLatinoParseHttpSource : ParsedHttpSource() {

    // Randomaze the userAgent to try to help with cloudflare
    protected open val userAgentRandomizer = " ${Random.nextInt().absoluteValue}"

    /**
     * User Agent for this Website
     */
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/78.0$userAgentRandomizer"

//    /**
//     * User Agent for Android for this Website
//     */
//    private val userAgent = "Mozilla/5.0 (" +
//        "Android ${Build.VERSION.RELEASE}; Mobile) " +
//        "Tachiyomi/${BuildConfig.VERSION_NAME}"

    override fun headersBuilder(): Headers.Builder {
        return Headers.Builder()
            .add("User-Agent", userAgent)
            .add("Referer", "$baseUrl/")
    }

    private val rateLimitInterceptor = RateLimitInterceptor(4)
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()
}
