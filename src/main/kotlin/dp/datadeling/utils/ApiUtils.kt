package dp.datadeling.utils

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import dp.datadeling.defaultLogger
import io.ktor.http.*
import io.ktor.util.logging.*


fun isLocal(): Boolean {
    return System.getenv("ENV") == "LOCAL"
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondError(
    message: String,
    throwable: Throwable? = null
) {
    if (throwable != null) defaultLogger.error(throwable)

    responder.respond(
        HttpStatusCode.InternalServerError,
        message,
        this.pipeline
    )
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondConflict(message: String) {
    defaultLogger.info { message }

    responder.respond(
        HttpStatusCode.Conflict,
        message,
        this.pipeline
    )
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondNotFound(message: String) {
    defaultLogger.info { message }

    responder.respond(
        HttpStatusCode.NotFound,
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
