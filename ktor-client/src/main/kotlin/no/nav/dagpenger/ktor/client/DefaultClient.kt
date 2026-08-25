package no.nav.dagpenger.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson3.jackson
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature

val defaultHttpClient =
    HttpClient {
        install(ContentNegotiation) {
            jackson {
                disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
        installRetryClient(
            maksRetries = 5,
        )
        install(Logging) {
            level = LogLevel.INFO
        }
    }

fun HttpClientConfig<*>.installRetryClient(
    maksRetries: Int = 5,
    delayFunc: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    install(HttpRequestRetry) {
        delay { delayFunc(it) }
        retryOnServerErrors(maxRetries = maksRetries)
        exponentialDelay()
    }
}
