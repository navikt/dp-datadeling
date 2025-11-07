package no.nav.dagpenger.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.søknad.modell.LegacySøknadsmelding
import no.nav.dagpenger.søknad.modell.PapirSøknadsMelding
import no.nav.dagpenger.søknad.modell.QuizSøknadMelding
import no.nav.dagpenger.søknad.modell.SøknadMelding
import java.time.Duration
import java.time.LocalDateTime

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")

class SøknadMottak(
    rapidsConnection: RapidsConnection,
    private val søknadRepository: SøknadRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "innsending_ferdigstilt") }
                validate {
                    it.requireKey(
                        "fødselsnummer",
                        "journalpostId",
                        "datoRegistrert",
                        "skjemaKode",
                    )
                    it.requireAny("type", listOf("NySøknad", "Gjenopptak"))
                    it.interestedIn(
                        QuizSøknadMelding.SØKNAD_ID_NØKKEL,
                        LegacySøknadsmelding.SØKNAD_ID_NØKKEL,
                    )
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadMelding: SøknadMelding = packet.tilSøknadMelding()

        withLoggingContext(
            "søknadId" to søknadMelding.søknadId,
            "journalpostId" to søknadMelding.journalpostId,
        ) {
            logg.info { "Mottok ny søknad av typen ${søknadMelding.javaClass.simpleName}." }
            sikkerlogg.info { "Mottok ny søknad for person ${søknadMelding.ident}: ${packet.toJson()}" }

            søknadRepository.lagre(
                søknadMelding.ident,
                søknadMelding.søknadId,
                søknadMelding.journalpostId,
                søknadMelding.skjemaKode,
                søknadMelding.søknadsType,
                søknadMelding.kanal,
                søknadMelding.datoRegistrert,
            )

            // TODO: Konverter til en "ekstern"-hendelse og videresend til RnR
        }.also {
            logg.info {
                val datoRegistrert = packet["datoRegistrert"].asLocalDateTime()
                val forsinkelse = Duration.between(datoRegistrert, LocalDateTime.now()).toMillis()
                "Har lagret en søknad med $forsinkelse millisekunder forsinkelse"
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.debug { problems }
    }
}

private fun JsonMessage.tilSøknadMelding(): SøknadMelding =
    if (this.harSøknadIdFraQuiz()) {
        QuizSøknadMelding(this)
    } else if (this.harSøknadIdFraLegacy()) {
        LegacySøknadsmelding(this)
    } else {
        PapirSøknadsMelding(this)
    }

private fun JsonMessage.harSøknadIdFraQuiz() = !this[QuizSøknadMelding.SØKNAD_ID_NØKKEL].isMissingNode

private fun JsonMessage.harSøknadIdFraLegacy() = !this[LegacySøknadsmelding.SØKNAD_ID_NØKKEL].isMissingNode
