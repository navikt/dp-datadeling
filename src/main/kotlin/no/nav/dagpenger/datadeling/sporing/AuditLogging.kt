package no.nav.dagpenger.datadeling.sporing

import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import java.util.UUID

sealed class AuditHendelse(
    private val ident: String,
    private val saksbehandlerNavIdent: String,
    auditMelding: String,
    auditOperasjon: AuditOperasjon,
    private val aktivitetsLogg: Aktivitetslogg = Aktivitetslogg(),
) : AktivitetsloggHendelse, IAktivitetslogg by aktivitetsLogg {
    init {
        aktivitetsLogg.info(
            melding = auditMelding,
            borgerIdent = ident,
            saksbehandlerNavIdent = saksbehandlerNavIdent,
            operasjon = auditOperasjon,
        )
    }

    override fun meldingsreferanseId(): UUID = UUID.randomUUID()

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst(
            this.javaClass.simpleName,
            kontekst() +
                mapOf(
                    "meldingsreferanseId" to meldingsreferanseId().toString(),
                    "ident" to ident,
                    "saksbehandlerNavIdent" to saksbehandlerNavIdent,
                ),
        )
    }

    override fun ident() = ident

    abstract fun kontekst(): Map<String, String>
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

interface AuditLogger {
    fun log(hendelse: AuditHendelse) {}
}
