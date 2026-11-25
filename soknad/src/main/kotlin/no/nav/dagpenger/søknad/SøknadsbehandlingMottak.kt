package no.nav.dagpenger.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.søknad.modell.SøknadsbehandlingTilstand
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}

/** søknadsbehandling_opprettet: søknaden er til behandling. @opprettet er søknadens innsendt-tidspunkt i NKS. */
class SøknadsbehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val repository: SøknadsbehandlingRepository,
) : SøknadsbehandlingMottak(rapidsConnection, "søknadsbehandling_opprettet") {
    override fun håndter(hendelse: Søknadsbehandlingshendelse) {
        repository.lagreOpprettet(hendelse.søknadId, hendelse.behandlingId, hendelse.sakId, hendelse.ident, hendelse.tidspunkt)
    }
}

/** søknadsbehandling_ferdig */
class SøknadsbehandlingFerdigMottak(
    rapidsConnection: RapidsConnection,
    private val repository: SøknadsbehandlingRepository,
) : SøknadsbehandlingMottak(rapidsConnection, "søknadsbehandling_ferdig") {
    override fun håndter(hendelse: Søknadsbehandlingshendelse) =
        repository.lagreTilstand(hendelse, SøknadsbehandlingTilstand.Ferdig, hendelse.utfall)
}

/** søknadsbehandling_avbrutt, for eksempel når søknaden er trukket. */
class SøknadsbehandlingAvbruttMottak(
    rapidsConnection: RapidsConnection,
    private val repository: SøknadsbehandlingRepository,
) : SøknadsbehandlingMottak(rapidsConnection, "søknadsbehandling_avbrutt") {
    override fun håndter(hendelse: Søknadsbehandlingshendelse) = repository.lagreTilstand(hendelse, SøknadsbehandlingTilstand.Avbrutt)
}

class Søknadsbehandlingshendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val sakId: UUID,
    val ident: String,
    val tidspunkt: LocalDateTime,
    val utfall: String? = null,
)

private fun SøknadsbehandlingRepository.lagreTilstand(
    hendelse: Søknadsbehandlingshendelse,
    tilstand: SøknadsbehandlingTilstand,
    utfall: String? = null,
) = lagreTilstand(hendelse.søknadId, hendelse.behandlingId, hendelse.sakId, hendelse.ident, tilstand, hendelse.tidspunkt, utfall)

/** Felles felter for søknadsbehandling-eventene. behandlingsskjedeId (med s) er sakId. */
abstract class SøknadsbehandlingMottak(
    rapidsConnection: RapidsConnection,
    eventNavn: String,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", eventNavn) }
                validate {
                    it.requireKey("ident", "behandlingId", "behandlingsskjedeId", "søknadId", "@opprettet")
                    it.interestedIn("førteTil")
                }
            }.register(this)
    }

    protected abstract fun håndter(hendelse: Søknadsbehandlingshendelse)

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelse =
            Søknadsbehandlingshendelse(
                søknadId = UUID.fromString(packet["søknadId"].asString()),
                behandlingId = UUID.fromString(packet["behandlingId"].asString()),
                sakId = UUID.fromString(packet["behandlingsskjedeId"].asString()),
                ident = packet["ident"].asString(),
                tidspunkt = packet["@opprettet"].asLocalDateTime(),
                utfall = packet["førteTil"].takeUnless { it.isMissingNode || it.isNull }?.asText(),
            )

        withLoggingContext("søknadId" to hendelse.søknadId.toString(), "behandlingId" to hendelse.behandlingId.toString()) {
            håndter(hendelse)
            logg.info { "Lagret søknadsbehandling-hendelse." }
        }
    }
}
