package no.nav.dagpenger.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import tools.jackson.databind.JsonNode

private val logger = KotlinLogging.logger {}

fun interface BehandlingResultatTolkerFactory {
    fun hentTolker(json: JsonNode): BehandlingResultat
}

val standardTolkerFactory =
    BehandlingResultatTolkerFactory { json ->
        logger.debug { "Bruker BehandlingResultatJsonNodeTolker" }
        BehandlingResultatJsonNodeTolker.fra(json)
    }
