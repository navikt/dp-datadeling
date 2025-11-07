package no.nav.dagpenger.ktor.client

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson.jackson

val defaultHttpClient =
    HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
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
