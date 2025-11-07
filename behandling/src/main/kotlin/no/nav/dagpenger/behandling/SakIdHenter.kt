package no.nav.dagpenger.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import java.util.UUID

class SakIdHenter(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    httpClientEngine: HttpClientEngine = CIO.create {},
) {
    private companion object {
        private val log = KotlinLogging.logger {}
    }

    private val client: HttpClient =
        HttpClient(httpClientEngine) {
            expectSuccess = true
            defaultRequest {
                header("Nav-Consumer-Id", "dp-datadeling")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            }
        }

    suspend fun hentSakId(behandlingId: UUID): UUID {
        val url = URLBuilder(baseUrl).appendPathSegments("behandling", behandlingId.toString(), "sakId").build()
        try {
            val sakId =
                client
                    .get(url)
                    .bodyAsText()
                    .let { body -> UUID.fromString(body) }
            log.info { "Hentet sakId=$sakId for behandlingId=$behandlingId" }
            return sakId
        } catch (error: Exception) {
            log.error(error) { "Uventet feil ved henting av sakId for behandlingId=$behandlingId" }
            throw error
        }
    }
}
