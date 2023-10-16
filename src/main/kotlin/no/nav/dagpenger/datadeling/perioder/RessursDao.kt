package no.nav.dagpenger.datadeling.perioder

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.teknisk.asQuery
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.felles.objectMapper
import javax.sql.DataSource

class RessursDao(private val dataSource: DataSource) {
    fun opprettRessurs() = sessionOf(dataSource, returnGeneratedKey = true).use {
        it.run(
            asQuery("insert into ressurs(status) values ('opprettet')").asUpdateAndReturnGeneratedKey
        )
    }

    fun hentRessurs(id: Long): Ressurs<DatadelingResponse>? = sessionOf(dataSource).use { session ->
        session.run(
            asQuery("select * from ressurs where id = ?", id).map { row ->
                Ressurs(
                    id = row.long("id"),
                    status = row.string("status").tilRessursStatus(),
                    data = row.stringOrNull("data")?.let { objectMapper.readValue<DatadelingResponse>(it) },
                )
            }.asSingle
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

private fun String.tilRessursStatus(): RessursStatus = when (this) {
    "opprettet" -> RessursStatus.OPPRETTET
    "ferdig" -> RessursStatus.FERDIG
    "feilet" -> RessursStatus.FEILET
    else -> {
        throw Exception("Prøvde å deserialisere ukjent ressursstatus: $this")
    }
}
