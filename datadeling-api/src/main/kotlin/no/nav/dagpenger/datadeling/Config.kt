package no.nav.dagpenger.datadeling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.datadeling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import java.net.URI
import java.net.URL
import javax.sql.DataSource

internal object Config {
    val IDENT_REGEX = Regex("^[0-9]{11}$")

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "KAFKA_CONSUMER_GROUP_ID" to "dp-datadeling-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_EXTRA_TOPIC" to "teamdagpenger.journalforing.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "DP_DATADELING_URL" to "http://localhost",
            ),
        )

    private val prodProperties =
        ConfigurationMap(
            "KAFKA_EXTRA_TOPIC" to "teamdagpenger.journalforing.v1",
        )

    val datasource: DataSource by lazy { dataSource }

    val properties by lazy {
        val envProperties = systemProperties() overriding EnvironmentVariables()
        when (envProperties.getOrNull(Key("NAIS_CLUSTER_NAME", stringType))) {
            "prod-gcp" -> envProperties overriding prodProperties overriding defaultProperties
            else -> envProperties overriding defaultProperties
        }
    }

    val appConfig: AppConfig by lazy {
        AppConfig(
            isLocal = false,
            maskinporten =
                IssuerConfig(
                    discoveryUrl = properties[Key("MASKINPORTEN_WELL_KNOWN_URL", stringType)],
                    scope = "nav:dagpenger:afpprivat.read",
                    jwksUri = URI(properties[Key("MASKINPORTEN_JWKS_URI", stringType)]).toURL(),
                    issuer = properties[Key("MASKINPORTEN_ISSUER", stringType)],
                ),
            azure =
                IssuerConfig(
                    discoveryUrl = properties[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
                    scope = properties[Key("AZURE_APP_CLIENT_ID", stringType)],
                    jwksUri = URI(properties[Key("AZURE_OPENID_CONFIG_JWKS_URI", stringType)]).toURL(),
                    issuer = properties[Key("AZURE_OPENID_CONFIG_ISSUER", stringType)],
                ),
            ressurs =
                RessursConfig(
                    minutterLevetidOpprettet = 120,
                    minutterLevetidFerdig = 1440,
                    oppryddingsfrekvensMinutter = 60,
                ),
        )
    }

    val clusterName: String by lazy {
        properties[Key("NAIS_CLUSTER_NAME", stringType)]
    }

    val obotopic: String by lazy {
        properties[Key("OBO_TOPIC", stringType)]
    }

    val dpDatadelingUrl: String by lazy {
        properties[Key("DP_DATADELING_URL", stringType)]
    }

    val dpMeldekortregisterUrl by lazy {
        properties[Key("DP_MELDEKORTREGISTER_URL", stringType)]
    }
    val dpMeldekortregisterTokenProvider by lazy {
        azureAdTokenSupplier(properties[Key("DP_MELDEKORTREGISTER_SCOPE", stringType)])
    }

    val dpMeldepliktAdapterUrl by lazy {
        properties[Key("DP_MELDEPLIKT_ADAPTER_URL", stringType)]
    }
    val dpMeldepliktAdapterTokenProvider by lazy {
        azureAdTokenSupplier(properties[Key("DP_MELDEPLIKT_ADAPTER_SCOPE", stringType)])
    }

    val dpProxyUrl by lazy {
        properties[Key("DP_PROXY_URL", stringType)]
    }
    val dpProxyTokenProvider by lazy {
        azureAdTokenSupplier(properties[Key("DP_PROXY_SCOPE", stringType)])
    }

    fun electorPathUrl(): String = properties[Key("ELECTOR_GET_URL", stringType)]

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

    fun asMap(): Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }
}

data class AppConfig(
    val isLocal: Boolean = false,
    val maskinporten: IssuerConfig,
    val azure: IssuerConfig,
    val ressurs: RessursConfig,
)

data class IssuerConfig(
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
