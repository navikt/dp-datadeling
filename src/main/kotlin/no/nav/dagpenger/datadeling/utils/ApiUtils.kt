package no.nav.dagpenger.datadeling.utils

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.ktor.http.*
import io.ktor.server.config.*


fun isLocal(config: ApplicationConfig): Boolean {
    return config.property("ENV").getString() == "LOCAL"
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondError(message: String) {
    responder.respond(
        HttpStatusCode.InternalServerError,
        message,
        this.pipeline
    )
}

suspend inline fun <TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondOk(body: Any) {
    responder.respond(
        HttpStatusCode.OK,
        body,
        this.pipeline
    )
}
