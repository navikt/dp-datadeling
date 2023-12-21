package no.nav.dagpenger.datadeling.sporing

import mu.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

private val sikkerLogger = KotlinLogging.logger { "tjenestekall" }
private val logger = KotlinLogging.logger {}

internal class KafkaLogger(kafkaProps: Properties = Config.aivenKafkaConfig) : SporingOgAudit() {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()
    private val stringSerializer = StringSerializer()
    private val auditTopic: String = Config.auditTopic
    private val sporTopic: String = Config.sporTopic

    private val kafkaProducer = KafkaProducer(kafkaProps, stringSerializer, stringSerializer)

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info("Closing KafkaAuditLogger Kafka producer")
                kafkaProducer.flush()
                kafkaProducer.close()
                logger.info("done! ")
            },
        )
    }

    override fun audit(hendelse: AuditHendelse) {
        aktivitetsloggEventMapper.håndter(hendelse) { aktivitetsloggMelding ->
            val json =
                JsonMessage.newMessage(
                    eventName = aktivitetsloggMelding.eventNavn,
                    map = aktivitetsloggMelding.innhold,
                ).toJson()

            kafkaProducer.send(
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

                is DagpengerPeriodeSpørringHendelse -> null
            }
        val request: Any? =
            when (hendelse) {
                is DagpengerPeriodeHentetHendelse ->
                    hendelse.ressurs.request?.let {
                        objectMapper.writeValueAsString(it)
                    } ?: ""

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

            kafkaProducer.send(ProducerRecord(sporTopic, hendelse.ident(), sporingHendelse))
                .let { recordMetadataFuture ->
                    recordMetadataFuture.get().let { metadata ->
                        sikkerLogger.info { "Sporing logget(Topic: ${metadata.topic()}, Offset: ${metadata.offset()} : $sporingHendelse" }
                    }
                }
        }
    }
}
