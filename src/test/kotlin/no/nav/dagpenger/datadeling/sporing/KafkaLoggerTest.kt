package no.nav.dagpenger.datadeling.sporing

import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.KafkaAivenCredentials.Companion.producerConfig
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.datadeling.testutil.enPeriode
import no.nav.dagpenger.datadeling.testutil.enRessurs
import no.nav.dagpenger.datadeling.testutil.januar
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.Properties
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private object Kafka {
    val instance by lazy {
        // See https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility
        KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.3")).apply { this.start() }
    }

    fun consumerProps(): Properties {
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, instance.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-group")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
    }
}

class KafkaLoggerTest {
    @Test
    fun `Sender hendelse på rapid`() {
        KafkaLogger(
            producerConfig(
                appId = "dp-datadeling",
                bootStapServerUrl = Kafka.instance.bootstrapServers,
                aivenCredentials = null,
            ),
        ).let {
            it.log(DagpengerPeriodeSpørringHendelse("01020312342", "999888777"))
            it.log(
                DagpengerPeriodeHentetHendelse(
                    "999888777",
                    ressurs =
                        enRessurs(
                            data =
                                enDatadelingResponse(
                                    enPeriode(periode = 1.januar(2021)..1.januar(2022)),
                                    enPeriode(periode = 1.januar(2022)..1.januar(2023)),
                                ),
                        ),
                ),
            )
        }

        // Consuming the message
        val consumer = KafkaConsumer<String, String>(Kafka.consumerProps())
        consumer.subscribe(listOf(Config.sporTopic, Config.auditTopic))

        runBlocking {
            eventually(10.seconds) {
                consumer.poll(1.seconds.toJavaDuration()).let { records ->
                    records.filter { it.topic() == Config.sporTopic }.let { sporRecords ->
                        sporRecords.count() shouldBe 1
                        //language=JSON
                        sporRecords.first().value() shouldEqualSpecifiedJsonIgnoringOrder
                            """
                            {
                              "person": "01020312342",
                              "mottaker": "999888777",
                              "tema": "DAG",
                              "behandlingsGrunnlag": "GDPR Art. 6(1)e. AFP-tilskottsloven §17 første ledd, §29 andre ledd, første punktum. GDPR Art. 9(2)b",
                              "dataForespoersel":  "IntcbiAgXCJwZXJzb25JZGVudFwiIDogXCIwMTAyMDMxMjM0MlwiLFxuICBcImZyYU9nTWVkRGF0b1wiIDogXCIyMDIzLTAxLTAxXCJcbn0i",
                              "leverteData": "IntcbiAgXCJwZXJzb25JZGVudFwiIDogXCIwMTAyMDMxMjM0MlwiLFxuICBcInBlcmlvZGVyXCIgOiBbIHtcbiAgICBcImZyYU9nTWVkRGF0b1wiIDogXCIyMDIxLTAxLTAxXCIsXG4gICAgXCJ0aWxPZ01lZERhdG9cIiA6IFwiMjAyMi0wMS0wMVwiLFxuICAgIFwieXRlbHNlVHlwZVwiIDogXCJEQUdQRU5HRVJfQVJCRUlEU1NPS0VSX09SRElOQUVSXCJcbiAgfSwge1xuICAgIFwiZnJhT2dNZWREYXRvXCIgOiBcIjIwMjItMDEtMDFcIixcbiAgICBcInRpbE9nTWVkRGF0b1wiIDogXCIyMDIzLTAxLTAxXCIsXG4gICAgXCJ5dGVsc2VUeXBlXCIgOiBcIkRBR1BFTkdFUl9BUkJFSURTU09LRVJfT1JESU5BRVJcIlxuICB9IF1cbn0i"
                            }
                            """.trimIndent()
                    }
                    records.filter { it.topic() == Config.auditTopic }.let { auditRecords ->
                        auditRecords.count() shouldBe 2
                    }
                }
            }
        }
        consumer.close()
    }
}
