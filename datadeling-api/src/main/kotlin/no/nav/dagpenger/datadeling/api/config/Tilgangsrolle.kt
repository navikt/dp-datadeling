package no.nav.dagpenger.datadeling.api.config

/**
 * Roller som gir tilgang til ulike ressurser i datadeling-api
 */
@Suppress("ktlint:standard:enum-entry-name-case")
enum class Tilgangsrolle {
    rettighetsperioder,
    meldekort,
    utbetaling,
}
