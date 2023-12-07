package no.nav.dagpenger.datadeling

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class AbstractApiTest {
    companion object {
        private val testServer = TestApiServer()

        @JvmStatic
        @BeforeAll
        fun setup() {
            testServer.start()
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            testServer.shutdown()
        }
    }

    protected val server get() = testServer
}

suspend fun HttpClient.testPost(
    path: String,
    body: Any?,
    token: SignedJWT?,
) =
    post(path) {
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.ContentType, ContentType.Application.Json)
            if (token != null) {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }
        if (body != null) {
            setBody(objectMapper.writeValueAsString(body))
        }
    }

suspend fun HttpClient.testGet(path: String, token: SignedJWT?) =
    get(path) {
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json)
            append(HttpHeaders.ContentType, ContentType.Application.Json)
            if (token != null) {
                append(HttpHeaders.Authorization, "Bearer ${token.serialize()}")
            }
        }
    }