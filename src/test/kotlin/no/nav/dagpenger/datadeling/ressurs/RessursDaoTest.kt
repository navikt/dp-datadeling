package no.nav.dagpenger.datadeling.ressurs

import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.AbstractDatabaseTest
import no.nav.dagpenger.datadeling.teknisk.asQuery
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RessursDaoTest : AbstractDatabaseTest() {
    private val ressursDao = RessursDao(database.dataSource)

    @Test
    fun `opprett ressurs`() {
        ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
        ressursDao.opprett(DatadelingRequest("234", LocalDate.now(), LocalDate.now()))
        ressursDao.opprett(DatadelingRequest("345", LocalDate.now(), LocalDate.now()))

        alleRessurser().let { ressurser ->
            assertEquals(listOf(1L, 2L, 3L), ressurser.map { it.id })
            assertTrue { ressurser.all { it.status == RessursStatus.OPPRETTET } }
        }

    }

    @Test
    fun `henter ressurs`() {
        val request = DatadelingRequest(
            personIdent = "EN-IDENT",
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now(),
        )
        val response = DatadelingResponse(
            personIdent = "EN-IDENT",
            perioder = emptyList(),
        )

        insertRequest(request)
        insertRessurs(RessursStatus.FERDIG, 1L, response)

        ressursDao.hent(1L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.FERDIG)
        }
    }

    @Test
    fun `returnerer null om ressurs ikke finnes`() {
        assertNull(ressursDao.hent(1L))
    }

    @Test
    fun `ferdigstill ressurs`() {
        val opprettet = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
        assertNotNull(opprettet)
        assertEquals(RessursStatus.OPPRETTET, opprettet.status)

        val response = DatadelingResponse(
            personIdent = "EN-IDENT",
            perioder = emptyList(),
        )
        ressursDao.ferdigstill(opprettet.id, response)
        val ferdigstilt = ressursDao.hent(opprettet.id)

        assertNotNull(ferdigstilt)
        assertEquals(RessursStatus.FERDIG, ferdigstilt.status)
        assertEquals(response.personIdent, ferdigstilt.data!!.personIdent)
    }

    @Test
    fun `marker ressurs som feilet`() {
        val ressurs = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
        assertNotNull(ressurs)
        assertEquals(RessursStatus.OPPRETTET, ressurs.status)

        ressursDao.markerSomFeilet(ressurs.id)
        assertEquals(RessursStatus.FEILET, ressursDao.hent(ressurs.id)!!.status)
    }

    private fun alleRessurser() = sessionOf(database.dataSource).use { session ->
        session.run(
            asQuery("select * from ressurs").map { row ->
                Ressurs(
                    id = row.long("id"),
                    status = RessursStatus.valueOf(row.string("status").uppercase()),
                    data = row.stringOrNull("data")?.let { objectMapper.readValue(it, DatadelingResponse::class.java)  }
                )
            }.asList
        )
    }

    private fun insertRequest(request: DatadelingRequest) {
        sessionOf(database.dataSource).use { session ->
            session.run(
                asQuery(
                    "insert into request(data) values (CAST(? as json))",
                    objectMapper.writeValueAsString(request),
                ).asUpdate
            )
        }
    }

    private fun insertRessurs(status: RessursStatus, requestRef: Long, data: DatadelingResponse?) {
        sessionOf(database.dataSource).use { session ->
            session.run(
                asQuery(
                    "insert into ressurs(status, data, requestRef) values(CAST(? as ressurs_status), CAST(? as json), ?)",
                    status.name.lowercase(),
                    if (data != null) objectMapper.writeValueAsString(data) else null,
                    requestRef,
                ).asUpdate
            )
        }
    }
}