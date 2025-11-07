package no.nav.dagpenger.datadeling.api.config

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ISO6523ToOrgnummerTest {
    @Test
    fun parseISO6523ToOrgnummer() {
        shouldThrowWithMessage<IllegalArgumentException>("Feil format på ISO6523-formatted string: 123456") {
            "123456".parseISO6523ToOrgnummer()
        }
        shouldThrowWithMessage<IllegalArgumentException>("Feil lengde på orgnummer. Forventet 9 siffer men var: 456") {
            "123:456".parseISO6523ToOrgnummer()
        }

        shouldThrowWithMessage<IllegalArgumentException>("Orgnummer må bestå av kun siffer men var: 1234567a9") {
            "123:1234567a9".parseISO6523ToOrgnummer()
        }

        "123:123456789".parseISO6523ToOrgnummer() shouldBe "123456789"
    }
}
