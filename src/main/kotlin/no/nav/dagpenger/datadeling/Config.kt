package no.nav.dagpenger.datadeling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.KafkaRapid
import java.net.URL
import javax.sql.DataSource

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
                    jwksUri = URL(properties[Key("MASKINPORTEN_JWKS_URI", stringType)]),
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

    val rapidsConnection by lazy {
        KafkaRapid.create(
            KafkaConfig(
                bootstrapServers = properties[Key("KAFKA_BROKERS", stringType)],
                consumerGroupId = "dp-datadeling-v1",
                clientId = "dp-datadeling",
                truststorePassword = properties[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
                autoOffsetResetConfig = "latest",
            ),
            topic = "",
            extraTopics = listOf(),
        )
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
            runBlocking { azureAdClient.clientCredentials(scope).accessToken }
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
