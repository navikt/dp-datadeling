package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun interface BehandlingResultatTolkerFactory {
    fun hentTolker(json: JsonNode): BehandlingResultat
}

val standardTolkerFactory =
    BehandlingResultatTolkerFactory { json ->
        logger.debug { "Bruker BehandlingResultatJsonNodeTolker" }
        BehandlingResultatJsonNodeTolker.fra(json)
    }
