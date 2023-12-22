package no.nav.dagpenger.datadeling.api.config

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class OrgnummerTest {
    @Test
    fun parseISO6523ToOrgnummer() {
        shouldThrow<IllegalArgumentException> {
            "ssdfadsf".parseISO6523ToOrgnummer()
        }
    }
}
