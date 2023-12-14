package no.nav.dagpenger.datadeling.sporing

import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

data class PersonOpprettetHendelse(
    private val ident: String,
    private val saksbehandlerNavIdent: String,
    private val aktivitetsLogg: Aktivitetslogg = Aktivitetslogg(),
) : AktivitetsloggHendelse, IAktivitetslogg by aktivitetsLogg {
    private val id = UUID.randomUUID()

    init {
        aktivitetsLogg.info(
            melding = "Personhendelse",
            borgerIdent = ident,
            saksbehandlerNavIdent = saksbehandlerNavIdent,
            operasjon = AuditOperasjon.CREATE,
        )
    }

    override fun meldingsreferanseId(): UUID = id

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst(
            this.javaClass.simpleName,
            mapOf(
                "meldingsreferanseId" to meldingsreferanseId().toString(),
                "ident" to ident,
                "saksbehandlerNavIdent" to saksbehandlerNavIdent,
            ),
        )
    }

    override fun ident() = ident
}

internal class SporingService(private val rapidsConnection: RapidsConnection) {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()

    fun hubba(hendelse: PersonOpprettetHendelse) {
        aktivitetsloggEventMapper.håndter(hendelse) { aktivitetsloggMelding ->
            rapidsConnection.publish(
                JsonMessage.newMessage(
                    eventName = aktivitetsloggMelding.eventNavn,
                    map = aktivitetsloggMelding.innhold,
                ).toJson(),
            )
        }
    }
}
