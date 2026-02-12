package no.nav.dagpenger.meldekort

import no.nav.dagpenger.datadeling.models.AktivitetDTO
import no.nav.dagpenger.datadeling.models.AktivitetDTOTypeDTO
import no.nav.dagpenger.datadeling.models.DagDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTOStatusDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTOTypeDTO
import no.nav.dagpenger.datadeling.models.MeldekortKildeDTO
import no.nav.dagpenger.datadeling.models.MeldekortKildeDTORolleDTO
import no.nav.dagpenger.datadeling.models.MeldekortPeriodeDTO
import no.nav.dagpenger.datadeling.models.OpprettetAvDTO
import java.time.LocalDate
import java.util.UUID
import kotlin.time.DurationUnit.HOURS
import kotlin.time.toDuration

data class Rapporteringsperiode(
    val id: Long,
    val periode: Periode,
    val dager: List<Dag>,
    val kanSendesFra: LocalDate,
    val kanSendes: Boolean,
    val kanEndres: Boolean,
    val status: RapporteringsperiodeStatus,
    val mottattDato: LocalDate? = null,
    val bruttoBelop: Double? = null,
    val registrertArbeidssoker: Boolean? = null,
    val begrunnelseEndring: String? = null,
) {
    fun toDTO(
        ident: String,
        originalMeldekortId: String?,
    ): MeldekortDTO =
        when (this.status) {
            RapporteringsperiodeStatus.TilUtfylling -> {
                MeldekortDTO(
                    id = id.toString(),
                    ident = ident,
                    status = MeldekortDTOStatusDTO.TIL_UTFYLLING,
                    type = MeldekortDTOTypeDTO.ORDINAERT,
                    periode = periode.toDTO(),
                    dager = dager.map { it.toDTO() },
                    kanSendes = kanSendes,
                    kanEndres = kanEndres,
                    kanSendesFra = kanSendesFra,
                    sisteFristForTrekk = periode.tilOgMed.plusDays(8),
                    opprettetAv = OpprettetAvDTO.ARENA,
                )
            }

            else -> {
                MeldekortDTO(
                    id = id.toString(),
                    ident = ident,
                    status = MeldekortDTOStatusDTO.INNSENDT,
                    type = if (begrunnelseEndring == null) MeldekortDTOTypeDTO.ORDINAERT else MeldekortDTOTypeDTO.KORRIGERT,
                    periode = periode.toDTO(),
                    dager = dager.map { it.toDTO() },
                    kanSendes = kanSendes,
                    kanEndres = kanEndres,
                    kanSendesFra = kanSendesFra,
                    sisteFristForTrekk = periode.tilOgMed.plusDays(8),
                    opprettetAv = OpprettetAvDTO.ARENA,
                    originalMeldekortId = originalMeldekortId,
                    begrunnelse = begrunnelseEndring,
                    kilde =
                        MeldekortKildeDTO(
                            rolle = MeldekortKildeDTORolleDTO.BRUKER,
                            ident = ident,
                        ),
                    innsendtTidspunkt =
                        mottattDato?.atStartOfDay() ?: throw IllegalArgumentException(
                            "Innsendt meldekort må ha en mottatt dato",
                        ),
                    registrertArbeidssoker =
                        registrertArbeidssoker
                            ?: throw IllegalArgumentException("Innsendt meldekort må ha registrertArbeidssoker satt"),
                    meldedato = mottattDato,
                )
            }
        }
}

data class Periode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
) {
    fun toDTO(): MeldekortPeriodeDTO =
        MeldekortPeriodeDTO(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
        )
}

class Dag(
    val dato: LocalDate,
    val aktiviteter: List<Aktivitet> = emptyList(),
    val dagIndex: Int,
) {
    fun toDTO(): DagDTO =
        DagDTO(
            dato = dato,
            aktiviteter = aktiviteter.map { it.toDTO() },
            dagIndex = dagIndex,
        )
}

data class Aktivitet(
    val uuid: UUID,
    val type: AktivitetsType,
    val timer: Double?,
) {
    enum class AktivitetsType {
        Arbeid,
        Syk,
        Utdanning,
        Fravaer,
    }

    fun toDTO(): AktivitetDTO =
        AktivitetDTO(
            id = uuid,
            type =
                when (type) {
                    AktivitetsType.Arbeid -> AktivitetDTOTypeDTO.ARBEID
                    AktivitetsType.Syk -> AktivitetDTOTypeDTO.SYK
                    AktivitetsType.Utdanning -> AktivitetDTOTypeDTO.UTDANNING
                    AktivitetsType.Fravaer -> AktivitetDTOTypeDTO.FRAVAER
                },
            timer = timer?.toDuration(HOURS)?.toIsoString(),
        )
}

enum class RapporteringsperiodeStatus {
    TilUtfylling,
    Endret,
    Innsendt,
    Ferdig,
    Feilet,
}
