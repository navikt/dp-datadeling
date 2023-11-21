package no.nav.dagpenger.datadeling.teknisk

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
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

enum class Scopes(override val description: String) : Described

fun AuthenticationConfig.jwtScope(realm: String, scope: String) {
    jwt(realm) {
        validate {
            credential ->
            if (scope == credential.getClaim("scope", String::class)) {
                return@validate null
            }
            JWTPrincipal(credential.payload)
        }
    }
}

class JwtProvider(
    referenceName: String,
    discoveryUrl: String? = null,
    private val realm: String? = null,
) : AuthProvider<TokenValidationContextPrincipal?> {
    override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
        listOf(
            listOf(
                AuthProvider.Security(
                    SecuritySchemeModel(
                        SecuritySchemeType.http,
                        scheme = HttpSecurityScheme.bearer,
                        bearerFormat = "JWT",
                        referenceName = referenceName,
                        openIdConnectUrl = discoveryUrl
                    ),
                    emptyList<Scopes>()
                )
            )
        )

    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): TokenValidationContextPrincipal? {
        return pipeline.context.authentication.principal() // ?: throw RuntimeException("No JWTPrincipal")
    }

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?> {
        val authenticatedKtorRoute = route.ktorRoute.authenticate(realm) { }
        return OpenAPIAuthenticatedRoute(authenticatedKtorRoute, route.provider.child(), this)
    }
}

inline fun NormalOpenAPIRoute.authAzureAd(
    realm: String?,
    route: OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?>.() -> Unit
): OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?> {
    val authenticatedKtorRoute = this.ktorRoute.authenticate { }
    val openAPIAuthenticatedRoute = OpenAPIAuthenticatedRoute(
        authenticatedKtorRoute,
        this.provider.child(),
        authProvider = JwtProvider("azureAd", )
    )
    return openAPIAuthenticatedRoute.apply {
        route()
    }
}
inline fun NormalOpenAPIRoute.authMaskinporten(
    realm: String,
    discoveryUrl: String,
    route: OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?>.() -> Unit): OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?> {

    val authenticatedKtorRoute = this.ktorRoute.authenticate(realm) { }

    val openAPIAuthenticatedRoute = OpenAPIAuthenticatedRoute(
        authenticatedKtorRoute,
        this.provider.child(),
        authProvider = JwtProvider(realm, discoveryUrl)
    )
    return openAPIAuthenticatedRoute.apply {
        route()
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