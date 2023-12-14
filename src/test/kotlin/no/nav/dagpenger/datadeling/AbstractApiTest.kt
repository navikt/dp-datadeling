package no.nav.dagpenger.datadeling

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class AbstractApiTest {
    companion object {
        private lateinit var mockOAuth2Server: MockOAuth2Server

        @JvmStatic
        @BeforeAll
        fun setup() {
            mockOAuth2Server = MockOAuth2Server()
            mockOAuth2Server.start(8091)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            mockOAuth2Server.shutdown()
        }
    }

    protected val server get() = mockOAuth2Server
}

suspend fun HttpClient.testPost(
    path: String,
    body: Any?,
    token: String?,
) = post(path) {
    headers {
        append(HttpHeaders.Accept, ContentType.Application.Json)
        append(HttpHeaders.ContentType, ContentType.Application.Json)
        token?.let {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
    body?.let {
        setBody(objectMapper.writeValueAsString(body))
    }
}

suspend fun HttpClient.testGet(
    path: String,
    token: String?,
) = get(path) {
    headers {
        append(HttpHeaders.Accept, ContentType.Application.Json)
        append(HttpHeaders.ContentType, ContentType.Application.Json)
        token?.let {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
