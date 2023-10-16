package no.nav.dagpenger.datadeling.teknisk

import io.ktor.client.*
import io.ktor.client.plugins.*

fun HttpClientConfig<*>.installRetryClient(
    maxRetries: Int = 5,
    delayFunc: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }, // Brukes for å mocke ut delay i enhetstester,
) {
    install(HttpRequestRetry) {
        delay { delayFunc(it) }
        retryOnServerErrors(maxRetries = maxRetries)
        exponentialDelay()
    }
}
