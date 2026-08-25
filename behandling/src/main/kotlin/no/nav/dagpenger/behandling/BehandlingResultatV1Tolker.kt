package no.nav.dagpenger.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.kontrakt.v1.models.Behandlingsresultatv1DTO
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.LocalDate
import java.util.UUID

class BehandlingResultatV1Tolker(
    private val dto: Behandlingsresultatv1DTO,
) : BehandlingResultat {
    override val ident: String = dto.ident
    override val behandlingId: UUID = dto.behandlingId

    override val rettighetsperioder: List<Rettighetsperiode> =
        dto.rettighetsperioder
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
                            override val type: Rettighetstype =
                                RETTIGHETSTYPE_OPPLYSNINGER[opplysning.opplysningTypeId]!!
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
                override val gjenståendeDager: Int =
                    dto.opplysninger
                        .find { it.opplysningTypeId == GJENSTÅENDE_DAGER_OPPLYSNINGER }
                        ?.perioder
                        ?.find {
                            it.gyldigFraOgMed == utbetalingDto.dato
                        }?.verdi
                        ?.let { verdi ->
                            (verdi as? no.nav.dagpenger.behandling.kontrakt.v1.models.HeltallVerdiv1DTO)?.verdi
                        } ?: innvilgetDager().also { logger.warn { "Finner ikke gjenstående dager for dato ${utbetalingDto.dato}" } }
            }
        }

    // Fallback for behandlinger hvor vi ikke har satt gjenstående dager for dager som ikke førte til forbruk
    private fun innvilgetDager(): Int =
        dto.opplysninger
            .firstOrNull { it.opplysningTypeId == INNVILGET_ANTALL_DAGER_OPPLYSNING }
            ?.perioder
            ?.firstOrNull()
            ?.let { (it.verdi as? no.nav.dagpenger.behandling.kontrakt.v1.models.HeltallVerdiv1DTO)?.verdi }
            ?: throw IllegalStateException("Finner ikke antall innvilgede dager")

    companion object {
        private val logger = KotlinLogging.logger { }
        private val objectMapper =
            JsonMapper
                .builder()
                .addModule(
                    KotlinModule
                        .Builder()
                        .build(),
                ).configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .changeDefaultPropertyInclusion { inclusion ->
                    inclusion.withContentInclusion(JsonInclude.Include.NON_NULL)
                    inclusion.withValueInclusion(JsonInclude.Include.NON_NULL)
                }.build()

        // Mapping fra opplysningTypeId til rettighetstype
        private val RETTIGHETSTYPE_OPPLYSNINGER =
            mapOf(
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a") to Rettighetstype.ORDINÆR,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86") to Rettighetstype.PERMITTERING,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87") to Rettighetstype.LØNNSGARANTI,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88") to Rettighetstype.FISK,
            )

        private val GJENSTÅENDE_DAGER_OPPLYSNINGER = UUID.fromString("01992956-e349-76b1-8f68-c9d481df3a32")
        private val INNVILGET_ANTALL_DAGER_OPPLYSNING = UUID.fromString("0194881f-943d-77a7-969c-147999f15457")

        fun fra(jsonNode: JsonNode): BehandlingResultatV1Tolker {
            val dto = objectMapper.treeToValue(jsonNode, Behandlingsresultatv1DTO::class.java)
            return BehandlingResultatV1Tolker(dto)
        }
    }
}
