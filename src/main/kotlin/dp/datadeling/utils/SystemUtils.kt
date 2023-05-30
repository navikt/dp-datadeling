package dp.datadeling.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector

fun getProperty(name: String): String? {
    var value = System.getenv(name)
    if (value == null || value.isEmpty()) value = System.getProperty(name)

    return value
}


val defaultObjectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
    .registerModule(ParameterNamesModule())
    .enable(SerializationFeature.INDENT_OUTPUT)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

lateinit var httpClient: HttpClient
fun defaultHttpClient(): HttpClient {
    if (!::httpClient.isInitialized) {
        httpClient = HttpClient(Apache) {
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(
                        defaultObjectMapper
                    )
                )
            }
            install(HttpTimeout) {
                // max time periods
                connectTimeoutMillis = 5000 // required to establish a connection with a server
                requestTimeoutMillis = 10000 // from sending a request to receiving a response
                socketTimeoutMillis = 10000 // inactivity between two data packets when exchanging data with a server
            }
            /*
            install("OutgoingCallInterceptor") {
                OutgoingCallLoggingPlugin().intercept(this)
            }
            */
            expectSuccess = false
            engine {
                customizeClient { setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault())) }
            }
        }
    }

    return httpClient
}
