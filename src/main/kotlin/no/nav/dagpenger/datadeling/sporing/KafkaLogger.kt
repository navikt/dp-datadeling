package no.nav.dagpenger.datadeling.sporing

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.objectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

private val sikkerLogger = KotlinLogging.logger("tjenestekall")
private val logger = KotlinLogging.logger {}

internal class KafkaLogger(
    kafkaProps: Properties = Config.aivenKafkaConfig,
) : SporingOgAudit() {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()
    private val stringSerializer = StringSerializer()
    private val auditTopic: String = Config.auditTopic
    private val sporTopic: String = Config.sporTopic

    private val kafkaProducer = KafkaProducer(kafkaProps, stringSerializer, stringSerializer)

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info { "Closing KafkaAuditLogger Kafka producer" }
                kafkaProducer.flush()
                kafkaProducer.close()
                logger.info { "done! " }
            },
        )
    }

    override fun audit(hendelse: AuditHendelse) {
        aktivitetsloggEventMapper.håndter(hendelse) { aktivitetsloggMelding ->
            val json =
                JsonMessage
                    .newMessage(
                        eventName = aktivitetsloggMelding.eventNavn,
                        map = aktivitetsloggMelding.innhold,
                    ).toJson()

            kafkaProducer
                .send(
                    ProducerRecord(auditTopic, hendelse.ident(), json),
                ).let { recordMetadataFuture ->
                    recordMetadataFuture.get().let { metadata ->
                        sikkerLogger.info { "Audit logget(Topic: ${metadata.topic()}, Offset: ${metadata.offset()} : $json" }
                    }
                }
        }
    }

    override fun spor(hendelse: AuditHendelse) {
        val leverteData: Any? =
            when (hendelse) {
                is DagpengerPeriodeHentetHendelse ->
                    hendelse.ressurs.response?.let {
                        objectMapper.writeValueAsString(it)
                    } ?: ""

                is DagpengerPerioderHentetHendelse ->
                    objectMapper.writeValueAsString(hendelse.response)

                is DagpengerSøknaderHentetHendelse ->
                    objectMapper.writeValueAsString(hendelse.response)

                is DagpengerSisteSøknadHentetHendelse ->
                    objectMapper.writeValueAsString(hendelse.response)

                is DagpengerVedtakHentetHendelse ->
                    objectMapper.writeValueAsString(hendelse.response)

                is DagpengerMeldekortHentetHendelse ->
                    objectMapper.writeValueAsString(hendelse.response)

                is DagpengerPeriodeSpørringHendelse -> null
            }
        val request: String? =
            when (hendelse) {
                is DagpengerPeriodeHentetHendelse ->
                    hendelse.ressurs.request.toString()

                is DagpengerPerioderHentetHendelse ->
                    hendelse.request.toString()

                is DagpengerSøknaderHentetHendelse ->
                    hendelse.request.toString()

                is DagpengerSisteSøknadHentetHendelse ->
                    hendelse.request

                is DagpengerVedtakHentetHendelse ->
                    hendelse.request.toString()

                is DagpengerMeldekortHentetHendelse ->
                    hendelse.request.toString()

                is DagpengerPeriodeSpørringHendelse -> null
            }
        if (leverteData != null && request != null) {
            val sporingHendelse =
                Sporing(
                    personIdent = hendelse.ident(),
                    konsumentOrgNr = hendelse.saksbehandlerNavIdent,
                    dataForespørsel = request,
                    leverteData = leverteData,
                ).sporingHendelse()

            kafkaProducer
                .send(ProducerRecord(sporTopic, sporingHendelse))
                .let { recordMetadataFuture ->
                    recordMetadataFuture.get().let { metadata ->
                        sikkerLogger.info { "Sporing logget(Topic: ${metadata.topic()}, Offset: ${metadata.offset()} : $sporingHendelse" }
                    }
                }
        }
    }
}
