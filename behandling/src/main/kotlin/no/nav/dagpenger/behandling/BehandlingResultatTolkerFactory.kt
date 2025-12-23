package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

fun interface BehandlingResultatTolkerFactory {
    fun hentTolker(json: JsonNode): BehandlingResultat
}

val standardTolkerFactory =
    BehandlingResultatTolkerFactory { json ->
        val versjon = utledVersjon(json)
        logger.debug { "Bruker BehandlingResultatTolker versjon $versjon (Fabrikt-generert)" }

        when (versjon) {
            TolkerVersjon.V1 -> BehandlingResultatV1Tolker.fra(json)
            // TolkerVersjon.V2 -> BehandlingResultatV2Tolker.fra(json)
        }
    }

private fun utledVersjon(json: JsonNode): TolkerVersjon {
    // Strategi 1: Basert på tidsstempel
    val opprettet = json["@opprettet"]?.asLocalDateTime()
    if (opprettet != null && opprettet.isBefore(TolkerVersjon.V1.gyldigTil)) {
        return TolkerVersjon.V1
    }
    // Strategi 2: Fallback til nyeste versjon
    return TolkerVersjon.entries.last()
}

private enum class TolkerVersjon(
    val gyldigTil: LocalDateTime,
) {
    V1(LocalDateTime.MAX),
    // V2(LocalDateTime.MAX) - når ny kontrakt kommer
}
