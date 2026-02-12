package no.nav.dagpenger.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.ktor.client.defaultHttpClient
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

class MeldepliktAdapterClient(
    private val dpMeldepliktAdapterUrl: String,
    private val tokenProvider: () -> String,
) {
    private val logger = KotlinLogging.logger {}
    private val sikkerlogger = KotlinLogging.logger("tjenestekall")

    suspend fun hentMeldekort(request: DatadelingRequestDTO): List<MeldekortDTO> {
        val ident = request.personIdent
        val fraOgMed = request.fraOgMedDato

        val meldekort = mutableListOf<MeldekortDTO>()

        coroutineScope {
            val hentInnsendte = async { hentInnsendteArenaMeldekort(ident, fraOgMed) }
            val hentTilUtfylling = async { hentArenaMeldekortTilUtfylling(ident) }

            meldekort.addAll(hentInnsendte.await())
            meldekort.addAll(hentTilUtfylling.await())
        }

        return meldekort
            .filter { taMed(it, request) }
            .sortedBy { it.periode.fraOgMed }
    }

    private suspend fun hentInnsendteArenaMeldekort(
        ident: String,
        fraOgMed: LocalDate,
    ): List<MeldekortDTO> =
        withContext(Dispatchers.IO) {
            if (fraOgMed > LocalDate.now()) return@withContext emptyList()

            val ukerSidenStart = ChronoUnit.WEEKS.between(fraOgMed, LocalDate.now())
            val antallMeldeperioder = ceil(ukerSidenStart.toDouble() / 2).toInt()

            logger.info { "Henter $antallMeldeperioder innsendte meldekort fra Arena" }
            sikkerlogger.info { "Henter $antallMeldeperioder innsendte meldekort fra Arena for ident $ident" }
            val response =
                defaultHttpClient
                    .get("$dpMeldepliktAdapterUrl/sendterapporteringsperioder") {
                        bearerAuth(tokenProvider.invoke())
                        header(HttpHeaders.Accept, ContentType.Application.Json)
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        header("ident", ident)
                        parameter("antallMeldeperioder", antallMeldeperioder)
                    }.also {
                        logger.info { "Kall til adapter for å hente innsendte meldekort ga status ${it.status}" }
                    }

            if (response.status == HttpStatusCode.NoContent) {
                return@withContext emptyList()
            }

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                logger.error { "Klarte ikke å hente innsendte meldekort fra adapter, status: ${response.status}, melding: $body" }
                sikkerlogger.error {
                    "Klarte ikke å hente innsendte meldekort for ident $ident, status: ${response.status}, melding: $body"
                }
                throw RuntimeException("Klarte ikke å hente innsendte meldekort")
            }

            response
                .body<List<Rapporteringsperiode>>()
                .also {
                    logger.info { "Hentet ${it.size} innsendte meldekort" }
                    sikkerlogger.info { "Hentet ${it.size} innsendte meldekort for ident $ident" }
                }.toMeldekortListe(ident)
        }

    private suspend fun hentArenaMeldekortTilUtfylling(ident: String): List<MeldekortDTO> =
        withContext(Dispatchers.IO) {
            logger.info { "Henter meldekort fra Arena som ikke er sendt inn" }
            sikkerlogger.info { "Henter meldekort fra Arena som ikke er sendt inn for ident $ident" }
            val response =
                defaultHttpClient
                    .get("$dpMeldepliktAdapterUrl/rapporteringsperioder") {
                        bearerAuth(tokenProvider.invoke())
                        header(HttpHeaders.Accept, ContentType.Application.Json)
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        header("ident", ident)
                    }.also {
                        logger.info { "Kall til adapter for å hente brukers meldekort ga status ${it.status}" }
                    }

            if (response.status == HttpStatusCode.NoContent) {
                return@withContext emptyList()
            }

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                logger.error { "Klarte ikke å hente meldekort fra adapter, status: ${response.status}, melding: $body" }
                sikkerlogger.error { "Klarte ikke å hente meldekort for ident $ident, status: ${response.status}, melding: $body" }
                throw RuntimeException("Klarte ikke å hente meldekort")
            }

            response
                .body<List<Rapporteringsperiode>>()
                .also {
                    logger.info { "Hentet ${it.size} meldekort" }
                    sikkerlogger.info { "Hentet ${it.size} meldekort for ident $ident" }
                }.toMeldekortListe(ident)
        }

    private fun List<Rapporteringsperiode>.toMeldekortListe(ident: String): List<MeldekortDTO> =
        map { rapporteringsperiode ->
            val originalMeldekortId =
                if (rapporteringsperiode.begrunnelseEndring != null) {
                    this
                        .find {
                            it.periode.fraOgMed.isEqual(rapporteringsperiode.periode.fraOgMed) &&
                                it.periode.tilOgMed.isEqual(rapporteringsperiode.periode.tilOgMed) &&
                                it.id != rapporteringsperiode.id
                        }?.id
                        ?.toString()
                } else {
                    null
                }
            rapporteringsperiode.toDTO(ident, originalMeldekortId)
        }

    private fun taMed(
        it: MeldekortDTO,
        request: DatadelingRequestDTO,
    ): Boolean =
        it.periode.fraOgMed >= request.fraOgMedDato && (request.tilOgMedDato == null || it.periode.tilOgMed <= request.tilOgMedDato)
}
