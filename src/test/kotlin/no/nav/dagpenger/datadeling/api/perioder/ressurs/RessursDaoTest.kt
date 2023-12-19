package no.nav.dagpenger.datadeling.api.perioder.ressurs

import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.Postgres.withMigratedDb
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus.FEILET
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus.FERDIG
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus.OPPRETTET
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.datadeling.testutil.enDatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RessursDaoTest {
    @Test
    fun `opprett og hent ressurs`() =
        withMigratedDb {
            val ressursDao = RessursDao(Config.datasource)
            val ressurs = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
            assertEquals(OPPRETTET, ressurs!!.status)
            assertEquals(ressurs, ressursDao.hent(ressurs.uuid))
        }

    @Test
    fun `returnerer null om ressurs ikke finnes`() =
        withMigratedDb {
            val ressursDao = RessursDao(Config.datasource)
            assertNull(ressursDao.hent(UUID.randomUUID()))
        }

    @Test
    fun `ferdigstill ressurs`() =
        withMigratedDb {
            val ressursDao = RessursDao(Config.datasource)
            val opprettet = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
            assertNotNull(opprettet)
            assertEquals(OPPRETTET, opprettet!!.status)

            val response =
                DatadelingResponse(
                    personIdent = "EN-IDENT",
                    perioder = emptyList(),
                )
            ressursDao.ferdigstill(opprettet.uuid, response)
            val ferdigstilt = ressursDao.hent(opprettet.uuid)

            requireNotNull(ferdigstilt)
            assertEquals(FERDIG, ferdigstilt.status)
            assertEquals(response.personIdent, ferdigstilt.response!!.personIdent)
        }

    @Test
    fun `marker ressurs som feilet`() =
        withMigratedDb {
            val ressursDao = RessursDao(Config.datasource)
            val ressurs = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
            requireNotNull(ressurs)
            assertEquals(OPPRETTET, ressurs.status)

            ressursDao.markerSomFeilet(ressurs.uuid)
            assertEquals(FEILET, ressursDao.hent(ressurs.uuid)!!.status)
        }

    @Test
    fun `marker gamle ressurser som feilet`() =
        withMigratedDb {
            val ressursDao = RessursDao(Config.datasource)
            val today = LocalDate.now()
            val ressurs = ressursDao.opprett(DatadelingRequest("123", today, today))
            requireNotNull(ressurs)
            assertEquals(OPPRETTET, ressurs.status)

            ressursDao.markerSomFeilet(today.plusDays(1).atStartOfDay())
            assertEquals(FEILET, ressursDao.hent(ressurs.uuid)!!.status)
        }

    @Test
    fun `sletter ferdige ressurser`() =
        withMigratedDb {
            val ressursDao = RessursDao(Config.datasource)
            val now = LocalDateTime.now()
            insertRessurs(OPPRETTET, opprettet = now.minusMinutes(10))
            insertRessurs(FERDIG, opprettet = now.minusMinutes(10))
            insertRessurs(FERDIG, opprettet = now)
            insertRessurs(FEILET, opprettet = now)

            val antallSlettet = ressursDao.slettFerdigeRessurser(eldreEnn = now.minusMinutes(5))
            assertEquals(1, antallSlettet)
            assertEquals(3, alleRessurser().size)
        }

    private fun insertRessurs(
        status: RessursStatus = OPPRETTET,
        request: DatadelingRequest = enDatadelingRequest(),
        response: DatadelingResponse? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = sessionOf(Config.datasource).use { session ->
        session.run(
            asQuery(
                """
                insert into ressurs(uuid, status, response, request, opprettet)
                values(?, CAST(? as ressurs_status), CAST(? as json), CAST(? as json), ?)
                returning uuid
                """.trimIndent(),
                UUID.randomUUID(),
                status.name.lowercase(),
                if (response != null) objectMapper.writeValueAsString(response) else null,
                objectMapper.writeValueAsString(request),
                opprettet,
            ).map {
                it.uuid("uuid")
            }.asSingle,
        )
    }

    private fun alleRessurser() =
        sessionOf(Config.datasource).use { session ->
            session.run(
                asQuery("select * from ressurs").map { row ->
                    Ressurs(
                        uuid = row.uuid("uuid"),
                        status = RessursStatus.valueOf(row.string("status").uppercase()),
                        request =
                            row.string("request")
                                .let { objectMapper.readValue(it, DatadelingRequest::class.java) },
                        response =
                            row.stringOrNull("response")
                                ?.let { objectMapper.readValue(it, DatadelingResponse::class.java) },
                    )
                }.asList,
            )
        }
}
