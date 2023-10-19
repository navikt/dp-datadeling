package no.nav.dagpenger.datadeling.ressurs

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.teknisk.asQuery
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import javax.sql.DataSource

class RessursDao(private val dataSource: DataSource) {
    fun opprett(request: DatadelingRequest) = sessionOf(dataSource).use { session ->
        val requestId = session.run(
            asQuery(
                "insert into request(data) values (CAST(? as json)) returning id",
                objectMapper.writeValueAsString(request),
            )
                .map { it.long("id") }
                .asSingle
        )
        session.run(
            asQuery("insert into ressurs(status, requestRef) values ('opprettet', ?) returning *", requestId)
                .map(::mapRessurs)
                .asSingle
        )
    }

    fun hent(id: Long) = sessionOf(dataSource).use { session ->
        session.run(
            asQuery("select * from ressurs where id = ?", id).map(::mapRessurs).asSingle
        )
    }

    fun ferdigstill(id: Long, data: DatadelingResponse) = sessionOf(dataSource).use {
        it.run(
            asQuery(
                "update ressurs set status = 'ferdig', data = CAST(? as json) where id = ?",
                objectMapper.writeValueAsString(data),
                id,
            ).asUpdate
        )
    }

    fun markerSomFeilet(id: Long) = sessionOf(dataSource).use {
        it.run(
            asQuery("update ressurs set status = 'feilet' where id = ?", id).asUpdate
        )
    }
}

private fun mapRessurs(row: Row): Ressurs = Ressurs(
    id = row.long("id"),
    status = row.string("status").tilRessursStatus(),
    data = row.stringOrNull("data")?.let { objectMapper.readValue<DatadelingResponse>(it) },
)

private fun String.tilRessursStatus(): RessursStatus = when (this) {
    "opprettet" -> RessursStatus.OPPRETTET
    "ferdig" -> RessursStatus.FERDIG
    "feilet" -> RessursStatus.FEILET
    else -> {
        throw Exception("Prøvde å deserialisere ukjent ressursstatus: $this")
    }
}
