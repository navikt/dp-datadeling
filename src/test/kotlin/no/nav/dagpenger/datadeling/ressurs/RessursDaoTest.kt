package no.nav.dagpenger.datadeling.ressurs

import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.AbstractDatabaseTest
import no.nav.dagpenger.datadeling.api.perioder.ressurs.Ressurs
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursDao
import no.nav.dagpenger.datadeling.api.perioder.ressurs.RessursStatus
import no.nav.dagpenger.datadeling.api.perioder.ressurs.asQuery
import no.nav.dagpenger.datadeling.enDatadelingRequest
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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
            assertEquals(3, ressurser.size)
            assertTrue { ressurser.all { it.status == RessursStatus.OPPRETTET } }
        }
    }

    @Test
    fun `henter ressurs`() {
        val request = enDatadelingRequest()
        val response = DatadelingResponse(
            personIdent = "EN-IDENT",
            perioder = emptyList(),
        )

        val id = insertRessurs(RessursStatus.FERDIG, request, response)

        ressursDao.hent(id!!).let {
            assertNotNull(it)
            assertEquals(it.status, RessursStatus.FERDIG)
        }
    }

    @Test
    fun `returnerer null om ressurs ikke finnes`() {
        assertNull(ressursDao.hent(UUID.randomUUID()))
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
        ressursDao.ferdigstill(opprettet.uuid, response)
        val ferdigstilt = ressursDao.hent(opprettet.uuid)

        assertNotNull(ferdigstilt)
        assertEquals(RessursStatus.FERDIG, ferdigstilt.status)
        assertEquals(response.personIdent, ferdigstilt.response!!.personIdent)
    }

    @Test
    fun `marker ressurs som feilet`() {
        val ressurs = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
        assertNotNull(ressurs)
        assertEquals(RessursStatus.OPPRETTET, ressurs.status)

        ressursDao.markerSomFeilet(ressurs.uuid)
        assertEquals(RessursStatus.FEILET, ressursDao.hent(ressurs.uuid)!!.status)
    }

    @Test
    fun `marker gamle ressurser som feilet`() {
        val ressurs = ressursDao.opprett(DatadelingRequest("123", LocalDate.now(), LocalDate.now()))
        assertNotNull(ressurs)
        assertEquals(RessursStatus.OPPRETTET, ressurs.status)

        ressursDao.markerSomFeilet(LocalDateTime.now())
        assertEquals(RessursStatus.FEILET, ressursDao.hent(ressurs.uuid)!!.status)
    }

    @Test
    fun `sletter ferdige ressurser`() {
        val now = LocalDateTime.now()
        insertRessurs(RessursStatus.OPPRETTET, opprettet = now.minusMinutes(10))
        insertRessurs(RessursStatus.FERDIG, opprettet = now.minusMinutes(10))
        insertRessurs(RessursStatus.FERDIG, opprettet = now)
        insertRessurs(RessursStatus.FEILET, opprettet = now)

        val antallSlettet = ressursDao.slettFerdigeRessurser(eldreEnn = now.minusMinutes(5))
        assertEquals(1, antallSlettet)
        assertEquals(3, alleRessurser().size)
    }

    private fun alleRessurser() = sessionOf(database.dataSource).use { session ->
        session.run(
            asQuery("select * from ressurs").map { row ->
                Ressurs(
                    uuid = row.uuid("uuid"),
                    status = RessursStatus.valueOf(row.string("status").uppercase()),
                    response = row.stringOrNull("response")
                        ?.let { objectMapper.readValue(it, DatadelingResponse::class.java) }
                )
            }.asList
        )
    }

    private fun insertRessurs(
        status: RessursStatus = RessursStatus.OPPRETTET,
        request: DatadelingRequest = enDatadelingRequest(),
        response: DatadelingResponse? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) =
        sessionOf(database.dataSource).use { session ->
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
                    opprettet
                ).map {
                    it.uuid("uuid")
                }.asSingle
            )
        }
}