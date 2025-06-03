package no.nav.dagpenger.datadeling.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class Vedtaksmelding(
    private val packet: JsonMessage,
) {
    companion object {
        private var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")

        private fun JsonNode.asArenaDato() = asText().let { LocalDateTime.parse(it, formatter) }

        private fun JsonNode.asOptionalArenaDato() =
            takeIf(JsonNode::isTextual)
                ?.asText()
                ?.takeIf(String::isNotEmpty)
                ?.let { LocalDateTime.parse(it, formatter) }
    }

    internal val ident =
        if (packet["tokens"].isMissingOrNull()) packet["FODSELSNR"].asText() else packet["tokens"]["FODSELSNR"].asText()
    internal val vedtakId = packet["after"]["VEDTAK_ID"].asText()
    internal val fagsakId = packet["after"]["SAK_ID"].asText()
    internal val utfall
        get() =
            when (packet["after"]["UTFALLKODE"].asText()) {
                "JA" -> Vedtak.Utfall.INNVILGET
                "NEI" -> Vedtak.Utfall.AVSLÅTT
                else -> throw IllegalArgumentException("Ukjent utfallskode")
            }
    internal val fattet = packet["op_ts"].asArenaDato()
    internal val fraDato = packet["after"]["FRA_DATO"].asArenaDato()
    internal val tilDato = packet["after"]["TIL_DATO"].asOptionalArenaDato()
}
