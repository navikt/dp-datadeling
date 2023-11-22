package no.nav.dagpenger.datadeling.teknisk

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.net.URL
import java.util.concurrent.TimeUnit

fun AuthenticationConfig.maskinporten(
    name: String,
    scope: String,
    jwksUri: String,
    issuer: String,
) {
    val maskinportenJwkProvider: JwkProvider = JwkProviderBuilder(URL(jwksUri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    jwt(name) {
        verifier(maskinportenJwkProvider, issuer)
        validate { cred ->
            if (cred.getClaim("scope", String::class) != scope) {
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