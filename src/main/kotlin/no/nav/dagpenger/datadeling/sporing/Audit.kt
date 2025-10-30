package no.nav.dagpenger.datadeling.sporing

import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.datadeling.api.ressurs.Ressurs
import no.nav.dagpenger.datadeling.model.Søknad
import no.nav.dagpenger.datadeling.model.Vedtak
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.MeldekortDTO
import java.util.UUID

sealed class AuditHendelse(
    private val ident: String,
    val saksbehandlerNavIdent: String,
    auditMelding: String,
    auditOperasjon: AuditOperasjon,
    private val aktivitetsLogg: Aktivitetslogg = Aktivitetslogg(),
) : AktivitetsloggHendelse,
    IAktivitetslogg by aktivitetsLogg {
    init {
        aktivitetsLogg.info(
            melding = auditMelding,
            borgerIdent = ident,
            saksbehandlerNavIdent = saksbehandlerNavIdent,
            operasjon = auditOperasjon,
        )
    }

    override fun meldingsreferanseId(): UUID = UUID.randomUUID()

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(
            this.javaClass.simpleName,
            kontekst() +
                mapOf(
                    "meldingsreferanseId" to meldingsreferanseId().toString(),
                    "ident" to ident,
                    "saksbehandlerNavIdent" to saksbehandlerNavIdent,
                ),
        )

    override fun ident() = ident

    abstract fun kontekst(): Map<String, String>
}

class DagpengerPeriodeHentetHendelse(
    saksbehandlerNavIdent: String,
    val ressurs: Ressurs,
) : AuditHendelse(
        ident = ressurs.request.personIdent,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Henter ut dagpenger periode",
        auditOperasjon = AuditOperasjon.READ,
        aktivitetsLogg = Aktivitetslogg(),
    ) {
    override fun kontekst(): Map<String, String> = mapOf("ressursUuid" to ressurs.uuid.toString())
}

class DagpengerPerioderHentetHendelse(
    saksbehandlerNavIdent: String,
    val request: DatadelingRequestDTO,
    val response: DatadelingResponseDTO,
) : AuditHendelse(
        ident = request.personIdent,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Henter ut dagpenger perioder",
        auditOperasjon = AuditOperasjon.READ,
        aktivitetsLogg = Aktivitetslogg(),
    ) {
    override fun kontekst(): Map<String, String> = emptyMap()
}

class DagpengerMeldekortHentetHendelse(
    saksbehandlerNavIdent: String,
    val request: DatadelingRequestDTO,
    val response: List<MeldekortDTO>,
) : AuditHendelse(
        ident = request.personIdent,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Henter ut dagpenger meldekort",
        auditOperasjon = AuditOperasjon.READ,
        aktivitetsLogg = Aktivitetslogg(),
    ) {
    override fun kontekst(): Map<String, String> = emptyMap()
}

class DagpengerPeriodeSpørringHendelse(
    ident: String,
    saksbehandlerNavIdent: String,
) : AuditHendelse(
        ident = ident,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Spør om dagpenger periode",
        auditOperasjon = AuditOperasjon.CREATE,
    ) {
    override fun kontekst(): Map<String, String> = emptyMap()
}

class DagpengerSøknaderHentetHendelse(
    saksbehandlerNavIdent: String,
    val request: DatadelingRequestDTO,
    val response: List<Søknad>,
) : AuditHendelse(
        ident = request.personIdent,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Henter ut dagpenger søknader",
        auditOperasjon = AuditOperasjon.READ,
        aktivitetsLogg = Aktivitetslogg(),
    ) {
    override fun kontekst(): Map<String, String> = emptyMap()
}

class DagpengerSisteSøknadHentetHendelse(
    saksbehandlerNavIdent: String,
    val request: String,
    val response: Søknad?,
) : AuditHendelse(
        ident = request,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Henter ut siste søknad om dagpenger",
        auditOperasjon = AuditOperasjon.READ,
        aktivitetsLogg = Aktivitetslogg(),
    ) {
    override fun kontekst(): Map<String, String> = emptyMap()
}

class DagpengerVedtakHentetHendelse(
    saksbehandlerNavIdent: String,
    val request: DatadelingRequestDTO,
    val response: List<Vedtak>,
) : AuditHendelse(
        ident = request.personIdent,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        auditMelding = "Henter ut dagpenger vedtak",
        auditOperasjon = AuditOperasjon.READ,
        aktivitetsLogg = Aktivitetslogg(),
    ) {
    override fun kontekst(): Map<String, String> = emptyMap()
}

object NoopLogger : Log {
    override fun log(hendelse: AuditHendelse) {
        // Noop
    }
}
