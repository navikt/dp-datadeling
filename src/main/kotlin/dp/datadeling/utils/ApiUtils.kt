package dp.datadeling.utils

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.ktor.http.*


fun isLocal(): Boolean {
    return System.getenv("ENV") == "LOCAL"
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
