package no.nav.dagpenger.datadeling.perioder

import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.AbstractDatabaseTest
import no.nav.dagpenger.datadeling.teknisk.asQuery
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.security.mock.oauth2.http.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RessursDaoTest : AbstractDatabaseTest() {

    private val ressursDao = RessursDao(dataSource)

    @Test
    fun `opprett ressurs`() {
        ressursDao.opprettRessurs()
        ressursDao.opprettRessurs()
        ressursDao.opprettRessurs()

        val ids = sessionOf(dataSource).use { session ->
            session.run(asQuery("select * from ressurs").map {
                Ressurs(
                    id = it.long("id"),
                    status = RessursStatus.valueOf(it.string("status").uppercase()),
                    data = null,
                )
            }.asList)
        }

        assertEquals(listOf(1L, 2L, 3L), ids.map { it.id })
        assertTrue { ids.all { it.status == RessursStatus.OPPRETTET } }
    }

    @Test
    fun `hent ressurs`() {
        val response = DatadelingResponse(
            personIdent = "EN-IDENT",
            perioder = emptyList(),
        )
        sessionOf(dataSource).use { session ->
            session.run(
                asQuery(
                "insert into ressurs(status, data) values ('ferdig', null), ('opprettet', null), ('feilet', CAST(? as json))",
                objectMapper.writeValueAsString(response),
            ).asExecute)
        }

        ressursDao.hentRessurs(1L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.FERDIG)
        }

        ressursDao.hentRessurs(2L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.OPPRETTET)
        }

        ressursDao.hentRessurs(3L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.FEILET)
        }

        assertNull(ressursDao.hentRessurs(4L))
    }

    @Test
    fun `ferdigstill ressurs`() {
        val id = ressursDao.opprettRessurs()!!
        assertEquals(RessursStatus.OPPRETTET, ressursDao.hentRessurs(id)!!.status)

        val response = DatadelingResponse(
            personIdent = "EN-IDENT",
            perioder = emptyList(),
        )
        ressursDao.ferdigstill(id, response)
        val ressurs = ressursDao.hentRessurs(id)

        assertNotNull(ressurs)
        assertEquals(RessursStatus.FERDIG, ressurs.status)
        assertEquals(response.personIdent, ressurs.data!!.personIdent)
    }

    @Test
    fun `marker ressurs som feilet`() {
        val id = ressursDao.opprettRessurs()!!
        assertEquals(RessursStatus.OPPRETTET, ressursDao.hentRessurs(id)!!.status)

        ressursDao.markerSomFeilet(id)
        assertEquals(RessursStatus.FEILET, ressursDao.hentRessurs(id)!!.status)
    }

}