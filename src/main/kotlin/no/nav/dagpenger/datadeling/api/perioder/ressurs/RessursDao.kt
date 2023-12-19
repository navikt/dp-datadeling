package no.nav.dagpenger.datadeling.api.perioder.ressurs

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.datadeling.Config
import no.nav.dagpenger.datadeling.objectMapper
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class RessursDao(private val dataSource: DataSource = Config.datasource) {
    fun opprett(request: DatadelingRequest) =
        sessionOf(dataSource).use { session ->
            session.run(
                asQuery(
                    "insert into ressurs(uuid, status, request) values (?, 'opprettet', CAST(? as json)) returning *",
                    UUID.randomUUID(),
                    objectMapper.writeValueAsString(request),
                ).map(::mapRessurs).asSingle,
            )
        }

    fun hent(uuid: UUID): Ressurs? =
        sessionOf(dataSource).use { session ->
            session.run(
                asQuery("select * from ressurs where uuid = ?", uuid).map(::mapRessurs).asSingle,
            )
        }

    fun ferdigstill(
        uuid: UUID,
        data: DatadelingResponse,
    ) = sessionOf(dataSource).use {
        it.run(
            asQuery(
                "update ressurs set status = 'ferdig', response = CAST(? as json) where uuid = ?",
                objectMapper.writeValueAsString(data),
                uuid,
            ).asUpdate,
        )
    }

    fun markerSomFeilet(uuid: UUID) =
        sessionOf(dataSource).use {
            it.run(
                asQuery("update ressurs set status = 'feilet' where uuid = ?", uuid).asUpdate,
            )
        }

    fun markerSomFeilet(eldreEnn: LocalDateTime) =
        sessionOf(dataSource).use {
            it.run(
                asQuery("update ressurs set status = 'feilet' where opprettet < ?", eldreEnn).asUpdate,
            )
        }

    fun slettFerdigeRessurser(eldreEnn: LocalDateTime): Int =
        sessionOf(dataSource).use { session ->
            session.run(
                asQuery(
                    "delete from ressurs where status <> 'opprettet' and opprettet < ?",
                    eldreEnn,
                ).asUpdate,
            )
        }
}

fun asQuery(
    @Language("SQL") sql: String,
    argMap: Map<String, Any?> = emptyMap(),
) = queryOf(sql, argMap)

fun asQuery(
    @Language("SQL") sql: String,
    vararg params: Any?,
) = queryOf(sql, *params)

private fun mapRessurs(row: Row): Ressurs =
    Ressurs(
        uuid = row.uuid("uuid"),
        status = row.string("status").tilRessursStatus(),
        request = row.string("request").let { objectMapper.readValue<DatadelingRequest>(it) },
        response = row.stringOrNull("response")?.let { objectMapper.readValue<DatadelingResponse>(it) },
    )

private fun String.tilRessursStatus(): RessursStatus =
    when (this) {
        "opprettet" -> RessursStatus.OPPRETTET
        "ferdig" -> RessursStatus.FERDIG
        "feilet" -> RessursStatus.FEILET
        else -> {
            throw Exception("Prøvde å deserialisere ukjent ressursstatus: $this")
        }
    }
