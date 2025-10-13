package no.nav.dagpenger.datadeling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.defaultHttpClient

private val logg = KotlinLogging.logger {}

class SakApiClient(
    private val sakApiBaseUrl: String = Config.sakApiBaseUrl,
    private val tokenProvider: () -> String = Config.sakApiTokenProvider,
) {
    suspend fun hentSakId(behandlingId: String): String {
        val url = URLBuilder(sakApiBaseUrl).appendPathSegments("behandling", behandlingId, "sakId").build()

        try {
            val sakId =
                defaultHttpClient
                    .get(url) {
                        header("Nav-Consumer-Id", "dp-datadeling")
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                    }.bodyAsText()

            logg.info { "Hentet sakId=$sakId for behandlingId=$behandlingId" }

            return sakId
        } catch (error: Exception) {
            logg.error(error) { "Uventet feil ved henting av sakId for behandlingId=$behandlingId" }
            throw error
        }
    }
}
