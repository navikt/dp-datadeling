package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import java.time.LocalDate
import java.util.UUID

/**
 * Tolker som leser behandlingsresultat direkte fra JsonNode uten å gå via
 * fabrikt-genererte DTO-er. Unngår problemer med bakoverkompatibilitet når
 * kontrakten utvides med nye obligatoriske felt — ukjente felt ignoreres stille.
 */
class BehandlingResultatJsonNodeTolker private constructor(
    private val json: JsonNode,
) : BehandlingResultat {
    override val ident: String = json["ident"].asText()
    override val behandlingId: UUID = UUID.fromString(json["behandlingId"].asText())

    override val rettighetsperioder: List<Rettighetsperiode> =
        json["rettighetsperioder"].map { node ->
            object : Rettighetsperiode {
                override val fraOgMed: LocalDate = node["fraOgMed"].asLocalDate()
                override val tilOgMed: LocalDate? = node["tilOgMed"]?.asOptionalLocalDate()
                override val harRett: Boolean = node["harRett"].asBoolean()
            }
        }

    override val rettighetstyper: List<Rettighetstyper> =
        json["opplysninger"]
            .filter { opplysning ->
                UUID.fromString(opplysning["opplysningTypeId"].asText()) in RETTIGHETSTYPE_OPPLYSNINGER.keys
            }.flatMap { opplysning ->
                val type = RETTIGHETSTYPE_OPPLYSNINGER[UUID.fromString(opplysning["opplysningTypeId"].asText())]!!
                opplysning["perioder"]
                    .filter { periode ->
                        periode["verdi"]?.get("verdi")?.asBoolean() == true
                    }.map { periode ->
                        object : Rettighetstyper {
                            override val type: Rettighetstype = type
                            override val fraOgMed: LocalDate =
                                periode["gyldigFraOgMed"]?.asOptionalLocalDate() ?: LocalDate.MIN
                            override val tilOgMed: LocalDate =
                                periode["gyldigTilOgMed"]?.asOptionalLocalDate() ?: LocalDate.MAX
                        }
                    }
            }

    override val beregninger: List<BeregnetDag> by lazy {
        if (json["utbetalinger"] == null) return@lazy emptyList()
        if (json["utbetalinger"].isEmpty) return@lazy emptyList()
        val gjenståendeDagerPerioder =
            json["opplysninger"]
                .find { it["opplysningTypeId"].asText() == GJENSTÅENDE_DAGER_OPPLYSNINGER.toString() }
                ?.get("perioder")
                ?.mapNotNull { periode ->
                    val fraOgMed = periode["gyldigFraOgMed"]?.asOptionalLocalDate() ?: return@mapNotNull null
                    val verdi = periode["verdi"]?.get("verdi")?.asInt() ?: return@mapNotNull null
                    fraOgMed to verdi
                }
                ?: throw IllegalStateException(
                    "Finner ikke gjenstående dager-opplysning (mangler opplysningTypeId $GJENSTÅENDE_DAGER_OPPLYSNINGER)",
                )
        json["utbetalinger"]?.map { utbetaling ->
            object : BeregnetDag {
                override val dato: LocalDate = utbetaling["dato"].asLocalDate()
                override val sats: Int = utbetaling["sats"].asInt()
                override val utbetaling: Int = utbetaling["utbetaling"].asInt()
                override val gjenståendeDager: Int =
                    gjenståendeDagerPerioder
                        .filter { (fraOgMed, _) -> !fraOgMed.isAfter(dato) }
                        .maxByOrNull { (fraOgMed, _) -> fraOgMed }
                        ?.second
                        ?: throw IllegalStateException(
                            "Finner ikke gjenstående dager for dato $dato — ingen perioder starter på eller før denne datoen",
                        )
            }
        } ?: emptyList()
    }

    companion object {
        private val RETTIGHETSTYPE_OPPLYSNINGER =
            mapOf(
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a") to Rettighetstype.ORDINÆR,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86") to Rettighetstype.PERMITTERING,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87") to Rettighetstype.LØNNSGARANTI,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88") to Rettighetstype.FISK,
            )

        private val GJENSTÅENDE_DAGER_OPPLYSNINGER = UUID.fromString("01992956-e349-76b1-8f68-c9d481df3a32")

        fun fra(jsonNode: JsonNode): BehandlingResultatJsonNodeTolker = BehandlingResultatJsonNodeTolker(jsonNode)
    }
}
