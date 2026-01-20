package no.nav.dagpenger.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.dagpenger.behandling.kontrakt.v1.models.Behandlingsresultatv1DTO
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.map

class BehandlingResultatV1Tolker(
    private val dto: Behandlingsresultatv1DTO,
) : BehandlingResultat {
    override val ident: String = dto.ident
    override val behandlingId: UUID = dto.behandlingId

    override val rettighetsperioder: List<Rettighetsperiode> =
        dto.rettighetsperioder
            .filter { it.harRett }
            .map { periode ->
                object : Rettighetsperiode {
                    override val fraOgMed: LocalDate = periode.fraOgMed
                    override val tilOgMed: LocalDate? = periode.tilOgMed
                    override val harRett: Boolean = periode.harRett
                }
            }

    override val rettighetstyper: List<Rettighetstyper> =
        dto.opplysninger
            .filter { opplysning ->
                opplysning.opplysningTypeId in RETTIGHETSTYPE_OPPLYSNINGER.keys
            }.flatMap { opplysning ->
                opplysning.perioder
                    .filter { periode ->
                        (periode.verdi as? no.nav.dagpenger.behandling.kontrakt.v1.models.BoolskVerdiv1DTO)?.verdi == true
                    }.map { periode ->
                        object : Rettighetstyper {
                            override val type: Rettighetstype = RETTIGHETSTYPE_OPPLYSNINGER[opplysning.opplysningTypeId]!!
                            override val fraOgMed: LocalDate = periode.gyldigFraOgMed ?: LocalDate.MIN
                            override val tilOgMed: LocalDate = periode.gyldigTilOgMed ?: LocalDate.MAX
                        }
                    }
            }

    override val beregninger: List<BeregnetDag> =
        dto.utbetalinger.map { utbetalingDto ->
            object : BeregnetDag {
                override val dato: LocalDate = utbetalingDto.dato
                override val sats: Int = utbetalingDto.sats
                override val utbetaling: Int = utbetalingDto.utbetaling
            }
        }

    companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        // Mapping fra opplysningTypeId til rettighetstype
        private val RETTIGHETSTYPE_OPPLYSNINGER =
            mapOf(
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a") to Rettighetstype.ORDINÆR,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86") to Rettighetstype.PERMITTERING,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87") to Rettighetstype.LØNNSGARANTI,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88") to Rettighetstype.FISK,
            )

        fun fra(jsonNode: JsonNode): BehandlingResultatV1Tolker {
            val dto = objectMapper.treeToValue(jsonNode, Behandlingsresultatv1DTO::class.java)
            return BehandlingResultatV1Tolker(dto)
        }
    }
}
