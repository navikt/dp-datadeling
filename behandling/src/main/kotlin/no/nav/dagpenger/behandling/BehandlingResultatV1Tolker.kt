package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.map
import kotlin.text.get

data class BehandlingResultatV1Tolker(
    val jsonNode: JsonNode,
) : BehandlingResultat {
    override val ident: String = jsonNode["ident"].asText()
    override val behandlingId: UUID = UUID.fromString(jsonNode["behandlingId"].asText())
    override val rettighetsperioder: List<Rettighetsperiode> =
        jsonNode["rettighetsperioder"]
            .filter { it["harRett"].asBoolean() }
            .map {
                object : Rettighetsperiode {
                    override val fraOgMed: LocalDate = it["fraOgMed"].asLocalDate()
                    override val tilOgMed: LocalDate? = it["tilOgMed"]?.asOptionalLocalDate()
                    override val harRett: Boolean = it["harRett"].asBoolean()
                }
            }
    override val rettighetstyper: List<Rettighetstyper> =
        jsonNode["opplysninger"]
            .filter {
                UUID.fromString(it["opplysningTypeId"].asText()) in Opplysningstyper.entries.map { type -> type.opplysningTypeId }
            }.flatMap { rettighetstype ->
                rettighetstype["perioder"]
                    ?.filter { it["verdi"]["verdi"].asBoolean() }
                    ?.map { periode ->
                        object : Rettighetstyper {
                            override val type: Rettighetstype =
                                when (
                                    Opplysningstyper.entries.find {
                                        it.opplysningTypeId == UUID.fromString(rettighetstype["opplysningTypeId"].asText())
                                    }!!
                                ) {
                                    Opplysningstyper.ORDINÆRE_DAGPENGER -> Rettighetstype.ORDINÆR
                                    Opplysningstyper.PERMITTERT -> Rettighetstype.PERMITTERING
                                    Opplysningstyper.LØNNSGARANTI -> Rettighetstype.LØNNSGARANTI
                                    Opplysningstyper.FISK -> Rettighetstype.FISK
                                }
                            override val fraOgMed: LocalDate = periode["gyldigFraOgMed"]?.asOptionalLocalDate() ?: LocalDate.MIN
                            override val tilOgMed: LocalDate = periode["gyldigTilOgMed"]?.asOptionalLocalDate() ?: LocalDate.MAX
                        }
                    } ?: emptyList()
            }

    private enum class Opplysningstyper(
        val opplysningTypeId: UUID,
    ) {
        ORDINÆRE_DAGPENGER(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a")),
        PERMITTERT(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86")),
        LØNNSGARANTI(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87")),
        FISK(UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88")),
    }
}
