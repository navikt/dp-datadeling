package no.nav.dagpenger.datadeling.e2e

import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.behandling.PerioderService
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Postgres
import no.nav.dagpenger.datadeling.RessursConfig
import no.nav.dagpenger.datadeling.api.TestApplication
import no.nav.dagpenger.datadeling.api.TestApplication.testEndepunkter
import no.nav.dagpenger.datadeling.api.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.ressurs.RessursService
import no.nav.dagpenger.datadeling.api.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.models.DatadelingRequestDTO
import no.nav.dagpenger.datadeling.models.DatadelingResponseDTO
import no.nav.dagpenger.datadeling.models.PeriodeDTO
import no.nav.dagpenger.datadeling.models.YtelseTypeDTO
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testutil.januar
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RessursE2ETest {
    @Test
    fun `opprett ressurs og poll til ressurs har status FERDIG`() =
        Postgres.withMigratedDb {
            val perioderService =
                mockk<PerioderService>().also {
                    every {
                        it.hentDagpengeperioder(
                            any(),
                        )
                    } returns
                        DatadelingResponseDTO(
                            personIdent = "123",
                            perioder =
                                listOf(
                                    PeriodeDTO(
                                        fraOgMedDato = 10.januar(),
                                        tilOgMedDato = 25.januar(),
                                        ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                                        kilde = PeriodeDTO.Kilde.ARENA,
                                    ),
                                ),
                        )
                }

            val response =
                DatadelingResponseDTO(
                    personIdent = "123",
                    perioder =
                        listOf(
                            PeriodeDTO(
                                fraOgMedDato = 10.januar(),
                                tilOgMedDato = 25.januar(),
                                ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                                kilde = PeriodeDTO.Kilde.ARENA,
                            ),
                        ),
                )

            val ressursService =
                RessursService(
                    ressursDao = RessursDao(Config.datasource),
                    leaderElector = mockk(relaxed = true),
                    config =
                        RessursConfig(
                            minutterLevetidOpprettet = 120,
                            minutterLevetidFerdig = 1440,
                            oppryddingsfrekvensMinutter = 60,
                        ),
                )
            testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
                val response =
                    DatadelingResponseDTO(
                        personIdent = "123",
                        perioder =
                            listOf(
                                PeriodeDTO(
                                    fraOgMedDato = 10.januar(),
                                    tilOgMedDato = 25.januar(),
                                    ytelseType = YtelseTypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                                    kilde = PeriodeDTO.Kilde.ARENA,
                                ),
                            ),
                    )

                val request =
                    DatadelingRequestDTO(
                        personIdent = response.personIdent,
                        fraOgMedDato = 1.januar(),
                        tilOgMedDato = 31.januar(),
                    )

                val ressursUrl =
                    client
                        .post("/dagpenger/datadeling/v1/periode") {
                            headers {
                                append(HttpHeaders.Accept, ContentType.Application.Json)
                                append(HttpHeaders.ContentType, ContentType.Application.Json)
                                bearerAuth(TestApplication.issueMaskinportenToken())
                            }
                            setBody(objectMapper.writeValueAsString(request))
                        }.apply { assertEquals(HttpStatusCode.Created, this.status) }
                        .bodyAsText()

                val uuid = UUID.fromString(ressursUrl.split("/").last())
                runBlocking {
                    await.until {
                        ressursService.hent(uuid)?.status == RessursStatus.FERDIG
                    }
                }

                ressursUrl.fetchRessursResponse(client) {
                    this["status"].asText() shouldBe RessursStatus.FERDIG.name
                    this.toString() shouldEqualJson
                        """
                        {
                          "uuid": "$uuid",
                          "status": "FERDIG",
                          "response": {
                            "personIdent": "123",
                            "perioder": [
                              {
                                "fraOgMedDato": "2023-01-10",
                                "tilOgMedDato": "2023-01-25",
                                "ytelseType": "DAGPENGER_ARBEIDSSOKER_ORDINAER"
                              }
                            ]
                          }
                        }
                        """.trimIndent()
                }
            }
        }

    @Test
    fun `opprett ressurs og marker som FEILET ved error fra baksystem`() =
        Postgres.withMigratedDb {
            val perioderService =
                mockk<PerioderService>().also {
                    every {
                        it.hentDagpengeperioder(
                            any(),
                        )
                    } throws RuntimeException("Feil fra baksystem")
                }
            val ressursService =
                RessursService(
                    ressursDao = RessursDao(Config.datasource),
                    leaderElector = mockk(relaxed = true),
                    config =
                        RessursConfig(
                            minutterLevetidOpprettet = 120,
                            minutterLevetidFerdig = 1440,
                            oppryddingsfrekvensMinutter = 60,
                        ),
                )
            testEndepunkter(perioderService = perioderService, ressursService = ressursService) {
                val request =
                    DatadelingRequestDTO(
                        personIdent = "01020312345",
                        fraOgMedDato = 1.januar(),
                        tilOgMedDato = 31.januar(),
                    )

                val ressursUrl =
                    client
                        .post("/dagpenger/datadeling/v1/periode") {
                            headers {
                                append(HttpHeaders.Accept, ContentType.Application.Json)
                                append(HttpHeaders.ContentType, ContentType.Application.Json)
                                bearerAuth(TestApplication.issueMaskinportenToken())
                            }
                            setBody(objectMapper.writeValueAsString(request))
                        }.apply { assertEquals(HttpStatusCode.Created, this.status) }
                        .bodyAsText()

                val uuid = UUID.fromString(ressursUrl.split("/").last())
                runBlocking {
                    await.until {
                        ressursService.hent(uuid)?.status == RessursStatus.FEILET
                    }
                }

                ressursUrl.fetchRessursResponse(client) {
                    this["status"].asText() shouldBe RessursStatus.FEILET.name
                }
            }
        }
}

private suspend fun String.fetchRessursResponse(
    client: HttpClient,
    block: ObjectNode.() -> Unit,
) {
    val path = this.substringAfter("://").substringAfter("/").let { "/$it" }
    client
        .get(path) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
                append(HttpHeaders.Authorization, "Bearer ${TestApplication.issueMaskinportenToken()}")
            }
        }.apply { assertEquals(HttpStatusCode.OK, this.status) }
        .let { objectMapper.readValue(it.bodyAsText(), ObjectNode::class.java) }
        .apply { block() }
}
