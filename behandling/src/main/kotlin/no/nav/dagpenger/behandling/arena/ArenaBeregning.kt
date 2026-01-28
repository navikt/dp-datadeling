package no.nav.dagpenger.behandling.arena

import java.math.BigDecimal
import java.time.LocalDate

data class ArenaBeregning(
    val meldekortFraDato: LocalDate,
    val meldekortTilDato: LocalDate,
    val innvilgetSats: BigDecimal, // Innvilget dagsats inklusive barnetillegg og samordning til vedtaket som er grunnlag for utbetalingen
    val utbetalingsgrad: BigDecimal, // Angir anvist prosent eller utbetalingsgrad ift dager, der én dag er 20%
    val belop: BigDecimal, // Beregnet totalbeløp for utbetalingen
)
