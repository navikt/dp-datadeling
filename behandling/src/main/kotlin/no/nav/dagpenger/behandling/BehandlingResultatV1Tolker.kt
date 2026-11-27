package no.nav.dagpenger.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.kontrakt.v1.models.Behandlingsresultatv1DTO
import no.nav.dagpenger.behandling.kontrakt.v1.models.DesimaltallVerdiv1DTO
import no.nav.dagpenger.behandling.kontrakt.v1.models.HeltallVerdiv1DTO
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
                        Rettighetstypeperiode(
                            type = RETTIGHETSTYPE_OPPLYSNINGER.getValue(opplysning.opplysningTypeId),
                            fraOgMed = periode.gyldigFraOgMed ?: LocalDate.MIN,
                            tilOgMed = periode.gyldigTilOgMed ?: LocalDate.MAX,
                        )
                    }
            }.let { særtyper ->
                (særtyper + ordinærePerioder(særtyper)).sortedBy { it.fraOgMed }
            }

    private fun ordinærePerioder(særtyper: List<Rettighetstyper>): List<Rettighetstyper> =
        buildList {
            var fraOgMed = LocalDate.MIN
            for (særtype in særtyper.sortedBy { it.fraOgMed }) {
                if (fraOgMed.isBefore(særtype.fraOgMed)) {
                    add(Rettighetstypeperiode(Rettighetstype.ORDINÆR, fraOgMed, særtype.fraOgMed.minusDays(1)))
                }
                if (særtype.tilOgMed == LocalDate.MAX) return@buildList
                fraOgMed = maxOf(fraOgMed, særtype.tilOgMed.plusDays(1))
            }
            add(Rettighetstypeperiode(Rettighetstype.ORDINÆR, fraOgMed, LocalDate.MAX))
        }

    private data class Rettighetstypeperiode(
        override val type: Rettighetstype,
        override val fraOgMed: LocalDate,
        override val tilOgMed: LocalDate,
    ) : Rettighetstyper

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

    override val gjenståendeSanksjon: Int? get() = laveste(GJENSTÅENDE_SANKSJON_OPPLYSNING)
    override val gjenståendeTidsbegrensetBortfall: Int? get() = laveste(GJENSTÅENDE_TIDSBEGRENSET_BORTFALL_OPPLYSNING)

    override val antallBarn: List<Periodisert<Int>> get() = periodisert(ANTALL_BARN_OPPLYSNING) { (it as? HeltallVerdiv1DTO)?.verdi }
    override val dagsats: List<Periodisert<Int>> get() = periodisert(SATS_OPPLYSNING) { (it as? HeltallVerdiv1DTO)?.verdi }
    override val fastsattVanligArbeidstid: List<Periodisert<Double>>
        get() = periodisert(FASTSATT_VANLIG_ARBEIDSTID_OPPLYSNING) { (it as? DesimaltallVerdiv1DTO)?.verdi }

    private fun <T : Any> periodisert(
        opplysningTypeId: UUID,
        tolkVerdi: (Any) -> T?,
    ): List<Periodisert<T>> =
        dto.opplysninger
            .find { it.opplysningTypeId == opplysningTypeId }
            ?.perioder
            ?.mapNotNull { periode ->
                val verdi = periode.verdi?.let(tolkVerdi) ?: return@mapNotNull null
                Periodisert(periode.gyldigFraOgMed, periode.gyldigTilOgMed, verdi)
            }?.sortedBy { it.fraOgMed ?: LocalDate.MIN }
            .orEmpty()

    private fun laveste(opplysningTypeId: UUID): Int? =
        dto.opplysninger
            .find { it.opplysningTypeId == opplysningTypeId }
            ?.perioder
            ?.mapNotNull { (it.verdi as? no.nav.dagpenger.behandling.kontrakt.v1.models.HeltallVerdiv1DTO)?.verdi }
            ?.minOrNull()

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
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86") to Rettighetstype.PERMITTERING,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87") to Rettighetstype.LØNNSGARANTI,
                UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88") to Rettighetstype.FISK,
            )

        private val GJENSTÅENDE_DAGER_OPPLYSNINGER = UUID.fromString("01992956-e349-76b1-8f68-c9d481df3a32")
        private val GJENSTÅENDE_SANKSJON_OPPLYSNING = UUID.fromString("76d9c243-228b-4359-9550-1ebbb7ba5cb5")
        private val GJENSTÅENDE_TIDSBEGRENSET_BORTFALL_OPPLYSNING = UUID.fromString("019df7d6-5159-7290-b8a1-9fc9edc58579")
        private val ANTALL_BARN_OPPLYSNING = UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23c")
        private val SATS_OPPLYSNING = UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a24f")
        private val FASTSATT_VANLIG_ARBEIDSTID_OPPLYSNING = UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76c")
        private val INNVILGET_ANTALL_DAGER_OPPLYSNING = UUID.fromString("0194881f-943d-77a7-969c-147999f15457")

        fun fra(jsonNode: JsonNode): BehandlingResultatV1Tolker {
            val dto = objectMapper.treeToValue(jsonNode, Behandlingsresultatv1DTO::class.java)
            return BehandlingResultatV1Tolker(dto)
        }
    }
}
