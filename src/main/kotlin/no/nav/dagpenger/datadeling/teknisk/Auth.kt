package no.nav.dagpenger.datadeling.teknisk

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.dagpenger.datadeling.MaskinportenConfig
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import java.util.concurrent.TimeUnit

fun AuthenticationConfig.maskinporten(
    name: String,
    maskinportenConfig: MaskinportenConfig,
) {
    val maskinportenJwkProvider: JwkProvider = JwkProviderBuilder(maskinportenConfig.jwks_uri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    jwt(name) {
        verifier(maskinportenJwkProvider, maskinportenConfig.issuer)
        validate { cred ->
            if (cred.getClaim("scope", String::class) != maskinportenConfig.scope) {
                defaultLogger.warn("Wrong scope in claim")
                return@validate null
            }

            JWTPrincipal(cred.payload)
        }
    }
}

private val properties: Configuration by lazy {
    ConfigurationProperties.systemProperties() overriding EnvironmentVariables()
}
val cachedTokenProvider by lazy {
    val azureAd = OAuth2Config.AzureAd(properties)
    CachedOauth2Client(
        tokenEndpointUrl = azureAd.tokenEndpointUrl,
        authType = azureAd.clientSecret(),
    )
}