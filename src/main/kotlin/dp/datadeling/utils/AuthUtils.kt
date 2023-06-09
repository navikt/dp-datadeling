package dp.datadeling.utils

import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import dp.datadeling.defaultAuthProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

enum class Scopes(override val description: String) : Described {
    Profile("Some scope")
}

class JwtProvider : AuthProvider<TokenValidationContextPrincipal?> {
    override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
        listOf(
            listOf(
                AuthProvider.Security(
                    SecuritySchemeModel(
                        SecuritySchemeType.http,
                        scheme = HttpSecurityScheme.bearer,
                        bearerFormat = "JWT",
                        referenceName = "jwtAuth",
                    ),
                    emptyList<Scopes>()
                )
            )
        )

    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): TokenValidationContextPrincipal? {
        return pipeline.context.authentication.principal() // ?: throw RuntimeException("No JWTPrincipal")
    }

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?> {
        val authenticatedKtorRoute = route.ktorRoute.authenticate { }
        return OpenAPIAuthenticatedRoute(authenticatedKtorRoute, route.provider.child(), this)
    }
}

inline fun NormalOpenAPIRoute.auth(route: OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?>.() -> Unit): OpenAPIAuthenticatedRoute<TokenValidationContextPrincipal?> {
    val authenticatedKtorRoute = this.ktorRoute.authenticate { }
    val openAPIAuthenticatedRoute = OpenAPIAuthenticatedRoute(
        authenticatedKtorRoute,
        this.provider.child(),
        authProvider = defaultAuthProvider
    )
    return openAPIAuthenticatedRoute.apply {
        route()
    }
}
