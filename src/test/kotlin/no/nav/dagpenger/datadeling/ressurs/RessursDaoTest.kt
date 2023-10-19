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

        val ids = sessionOf(database.dataSource).use { session ->
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
        val request = DatadelingRequest(
            personIdent = "EN-IDENT",
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now(),
        )
        val response = DatadelingResponse(
            personIdent = "EN-IDENT",
            perioder = emptyList(),
        )

        sessionOf(database.dataSource).use { session ->
            session.transaction { transaction ->
                transaction.run(
                    asQuery(
                        """
                            insert into request(data) 
                            values (CAST(? as json)), 
                                (CAST(? as json)), 
                                (CAST(? as json)) 
                        """.trimIndent(),
                        objectMapper.writeValueAsString(request),
                        objectMapper.writeValueAsString(request),
                        objectMapper.writeValueAsString(request),
                    ).asUpdate
                )
                transaction.run(
                    asQuery(
                        """
                            insert into ressurs(status, data, requestRef) 
                            values ('ferdig', null, ?), 
                                ('opprettet', null, ?), 
                                ('feilet', CAST(? as json), ?)
                        """.trimIndent(),
                        1L,
                        2L,
                        objectMapper.writeValueAsString(response),
                        3L,
                    ).asExecute
                )
            }
        }

        ressursDao.hent(1L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.FERDIG)
        }

        ressursDao.hent(2L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.OPPRETTET)
        }

        ressursDao.hent(3L).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.FEILET)
        }

        assertNull(ressursDao.hent(4L))
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

}