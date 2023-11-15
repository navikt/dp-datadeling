package no.nav.dagpenger.datadeling.ressurs

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.teknisk.asQuery
import no.nav.dagpenger.datadeling.teknisk.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class RessursDao(private val dataSource: DataSource) {
    fun opprett(request: DatadelingRequest) = sessionOf(dataSource).use { session ->
        session.run(
            asQuery(
                "insert into ressurs(uuid, status, request) values (?, 'opprettet', CAST(? as json)) returning *",
                UUID.randomUUID(),
                objectMapper.writeValueAsString(request),
            ).map(::mapRessurs).asSingle
        )
    }

    fun hent(uuid: UUID) = sessionOf(dataSource).use { session ->
        session.run(
            asQuery("select * from ressurs where uuid = ?", uuid).map(::mapRessurs).asSingle
        )
    }

    fun ferdigstill(uuid: UUID, data: DatadelingResponse) = sessionOf(dataSource).use {
        it.run(
            asQuery(
                "update ressurs set status = 'ferdig', response = CAST(? as json) where uuid = ?",
                objectMapper.writeValueAsString(data),
                uuid,
            ).asUpdate
        )
    }

    fun markerSomFeilet(uuid: UUID) = sessionOf(dataSource).use {
        it.run(
            asQuery("update ressurs set status = 'feilet' where uuid = ?", uuid).asUpdate
        )
    }

    fun slettFerdigeRessurser(eldreEnn: LocalDateTime): Int = sessionOf(dataSource).use { session ->
        session.run(
            asQuery(
                "delete from ressurs where status = 'ferdig' and opprettet < ?",
                eldreEnn
            ).asUpdate
        )
    }
}

private fun mapRessurs(row: Row): Ressurs = Ressurs(
    uuid = row.uuid("uuid"),
    status = row.string("status").tilRessursStatus(),
    response = row.stringOrNull("response")?.let { objectMapper.readValue<DatadelingResponse>(it) },
)

private fun String.tilRessursStatus(): RessursStatus = when (this) {
    "opprettet" -> RessursStatus.OPPRETTET
    "ferdig" -> RessursStatus.FERDIG
    "feilet" -> RessursStatus.FEILET
    else -> {
        throw Exception("Prøvde å deserialisere ukjent ressursstatus: $this")
    }
}
