package no.nav.dagpenger.datadeling.perioder

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import no.nav.dagpenger.datadeling.defaultLogger
import no.nav.dagpenger.datadeling.teknisk.auth
import no.nav.dagpenger.datadeling.utils.respondError
import no.nav.dagpenger.datadeling.utils.respondOk
import no.nav.dagpenger.kontrakter.datadeling.DatadelingRequest
import no.nav.dagpenger.kontrakter.datadeling.DatadelingResponse
import no.nav.dagpenger.kontrakter.datadeling.Periode
import no.nav.dagpenger.kontrakter.felles.StønadType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDate

const val perioderApiPath = "/dagpenger/v1/periode"

fun NormalOpenAPIRoute.perioderApi(perioderService: PerioderService) {
    auth {
        route(perioderApiPath) {
            post<Unit, DatadelingResponse, DatadelingRequest, TokenValidationContextPrincipal?>(
                info("Oppslag"),
                exampleRequest = dataRequestExample,
                exampleResponse = dataResponseExample
            ) { _, request ->
                try {
                    val response = perioderService.hentDagpengeperioder(request)

                    respondOk(response)
                } catch (e: Exception) {
                    defaultLogger.error { e }
                    respondError("Kunne ikke få data")
                }
            }
        }
    }
}

private val dataRequestExample = DatadelingRequest(
    personIdent = "01020312345",
    fraOgMedDato = LocalDate.now(),
    tilOgMedDato = LocalDate.now()
)

private val dataResponseExample = DatadelingResponse(
    personIdent = "01020312345",
    perioder = listOf(
        Periode(
            fraOgMedDato = LocalDate.now(),
            tilOgMedDato = LocalDate.now(),
            ytelseType = StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
            gjenståendeDager = 0
        )
    )
)
