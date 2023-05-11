package dp.datadeling.api

import dp.datadeling.module
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
open class TestBase {

    private fun setOidcConfig(): MapApplicationConfig {
        return MapApplicationConfig(

        )
    }

    fun setUpTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            environment {
                config = setOidcConfig()
            }
            application {
                module()
            }

            block()
        }
    }
}
