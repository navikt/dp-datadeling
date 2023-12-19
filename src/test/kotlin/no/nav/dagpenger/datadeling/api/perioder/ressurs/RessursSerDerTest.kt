package no.nav.dagpenger.datadeling.api.perioder.ressurs

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.datadeling.testutil.enDatadelingResponse
import no.nav.dagpenger.datadeling.testutil.enRessurs
import org.junit.jupiter.api.Test
import java.util.UUID

class RessursSerDerTest {
    @Test
    fun serialize() {
        objectMapper.writeValueAsString(
            enRessurs(
                request = enDatadelingRequest(),
                uuid = UUID.fromString("db5338cd-fbf6-44b7-bca8-0312868c2b32"),
                data = enDatadelingResponse(),
            ),
        ) shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "uuid": "db5338cd-fbf6-44b7-bca8-0312868c2b32",
              "status": "OPPRETTET",
              "response": {
                "personIdent": "01020312342",
                "perioder": []
              }
            }
            """.trimIndent()
    }

    @Test
    fun deserialize() {
        objectMapper.readValue<Ressurs>(
            """
            {
              "uuid": "db5338cd-fbf6-44b7-bca8-0312868c2b32",
              "status": "OPPRETTET",
              "request": {
                "personIdent": "01020312342",
                "fraOgMedDato": "2023-01-01"
              },
              "response": {
                "personIdent": "01020312342",
                "perioder": []
              }
            }
            """.trimIndent(),
        ) shouldBe
            enRessurs(
                uuid = UUID.fromString("db5338cd-fbf6-44b7-bca8-0312868c2b32"),
                request = enDatadelingRequest(),
                data = enDatadelingResponse(),
            )
    }
}
