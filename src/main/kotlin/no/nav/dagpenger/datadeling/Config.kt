package no.nav.dagpenger.datadeling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.datadeling.KafkaAivenCredentials.Companion.producerConfig
import no.nav.dagpenger.datadeling.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.datadeling.sporing.KafkaLogger
import no.nav.dagpenger.datadeling.sporing.NoopLogger
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import java.net.URI
import java.net.URL
import java.util.Properties
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

internal object Config {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "DP_DATADELING_URL" to "http://localhost:8080",
                "DP_PROXY_CLIENT_MAX_RETRIES" to "5",
            ),
        )

    val datasource: DataSource by lazy { dataSource }

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val appConfig: AppConfig by lazy {
        AppConfig(
            isLocal = false,
            maskinporten =
                MaskinportenConfig(
                    discoveryUrl = properties[Key("MASKINPORTEN_WELL_KNOWN_URL", stringType)],
                    scope = "nav:dagpenger:afpprivat.read",
                    jwksUri = URI(properties[Key("MASKINPORTEN_JWKS_URI", stringType)]).toURL(),
                    issuer = properties[Key("MASKINPORTEN_ISSUER", stringType)],
                ),
            ressurs =
                RessursConfig(
                    minutterLevetidOpprettet = 120,
                    minutterLevetidFerdig = 1440,
                    oppryddingsfrekvensMinutter = 60,
                ),
        )
    }

    val dpDatadelingUrl: String by lazy {
        properties[Key("DP_DATADELING_URL", stringType)]
    }

    val dpProxyUrl by lazy {
        properties[Key("DP_PROXY_URL", stringType)]
    }
    val dpProxyClientMaxRetries: Int by lazy {
        properties[Key("DP_PROXY_CLIENT_MAX_RETRIES", intType)]
    }

    val dpProxyTokenProvider by lazy {
        azureAdTokenSupplier(properties[Key("DP_PROXY_SCOPE", stringType)])
    }

    val logger by lazy {
        when (isLocalEnvironment) {
            true -> {
                log.info("Using no-op audit logger")
                NoopLogger
            }

            else -> {
                log.info("Using Kafka audit logger")
                KafkaLogger()
            }
        }
    }

    val sporTopic: String by lazy {
        properties.getOrElse(Key("KAFKA_SPOR_TOPIC", stringType)) { "public-sporingslogg-loggmeldingmottatt" }
    }
    val auditTopic: String = "teamdagpenger.rapid.v1"

    val aivenKafkaConfig by lazy {
        producerConfig(
            appId = "dp-datadeling",
            bootStapServerUrl = properties[Key("KAFKA_BROKERS", stringType)],
            aivenCredentials = KafkaAivenCredentials(),
        )
    }

    val isLocalEnvironment: Boolean by lazy {
        properties.getOrNull(Key("NAIS_CLUSTER_NAME", stringType)) == null
    }

    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    private fun azureAdTokenSupplier(scope: String): () -> String =
        {
            runBlocking { azureAdClient.clientCredentials(scope).access_token }!!
        }
}

data class KafkaAivenCredentials(
    val securityProtocolConfig: String = SecurityProtocol.SSL.name,
    val sslEndpointIdentificationAlgorithmConfig: String = "",
    val sslTruststoreTypeConfig: String = "jks",
    val sslKeystoreTypeConfig: String = "PKCS12",
    val sslTruststoreLocationConfig: String = "/var/run/secrets/nais.io/kafka/client.truststore.jks",
    val sslTruststorePasswordConfig: String = Config.properties[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
    val sslKeystoreLocationConfig: String = "/var/run/secrets/nais.io/kafka/client.keystore.p12",
    val sslKeystorePasswordConfig: String = sslTruststorePasswordConfig,
) {
    companion object {
        private val stringSerializer = StringSerializer()

        internal fun producerConfig(
            appId: String,
            bootStapServerUrl: String,
            aivenCredentials: KafkaAivenCredentials?,
        ): Properties =
            Properties().apply {
                putAll(
                    listOf(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootStapServerUrl,
                        ProducerConfig.CLIENT_ID_CONFIG to appId,
                        ProducerConfig.ACKS_CONFIG to "all",
                        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                        ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE.toString(),
                        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5",
                        ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
                        ProducerConfig.LINGER_MS_CONFIG to "20",
                        ProducerConfig.BATCH_SIZE_CONFIG to 32.times(1024).toString(),
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to stringSerializer,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to stringSerializer,
                    ),
                )

                aivenCredentials?.let {
                    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it.securityProtocolConfig)
                    put(
                        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                        it.sslEndpointIdentificationAlgorithmConfig,
                    )
                    put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it.sslTruststoreTypeConfig)
                    put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, it.sslKeystoreTypeConfig)
                    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.sslTruststoreLocationConfig)
                    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.sslTruststorePasswordConfig)
                    put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it.sslKeystoreLocationConfig)
                    put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it.sslKeystorePasswordConfig)
                }
            }
    }
}

data class AppConfig(
    val isLocal: Boolean = false,
    val maskinporten: MaskinportenConfig,
    val ressurs: RessursConfig,
)

data class MaskinportenConfig(
    val discoveryUrl: String,
    val scope: String,
    val jwksUri: URL,
    val issuer: String,
)

data class RessursConfig(
    val minutterLevetidOpprettet: Long,
    val minutterLevetidFerdig: Long,
    val oppryddingsfrekvensMinutter: Long,
)
