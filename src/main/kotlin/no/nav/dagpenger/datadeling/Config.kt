package no.nav.dagpenger.datadeling

import com.ctc.wstx.shaded.msv_core.datatype.xsd.IntType
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
import java.net.URL
import javax.sql.DataSource

internal object Config {

    const val appName = "dp-datadeling"

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "DP_DATADELING_URL" to "http://localhost:8080",
            "DP_PROXY_CLIENT_MAX_RETRIES" to "5",
            //"DP_PROXY_URL" to "",
            //"DP_PROXY_SCOPE" to ""
        ),
    )

    val datasource: DataSource by lazy { dataSource }

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val appConfig: AppConfig by lazy {
        AppConfig(
            isLocal = false,
            maskinporten = MaskinportenConfig(
                discoveryUrl = properties[Key("MASKINPORTEN_WELL_KNOWN_URL", stringType)],
                scope = "nav:dagpenger:afpprivat.read",
                jwks_uri = URL(properties[Key("MASKINPORTEN_JWKS_URI", stringType)]),
                issuer = properties[Key("MASKINPORTEN_ISSUER", stringType)]
            ),
            ressurs = RessursConfig(
                minutterLevetidOpprettet = 120,
                minutterLevetidFerdig = 1440,
                oppryddingsfrekvensMinutter = 60
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

    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret()
        )
    }

    private fun azureAdTokenSupplier(scope: String): () -> String = {
        runBlocking { azureAdClient.clientCredentials(scope).accessToken }
    }
}