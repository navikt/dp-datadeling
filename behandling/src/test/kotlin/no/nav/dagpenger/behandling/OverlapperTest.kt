package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import org.junit.jupiter.api.Test

class OverlapperTest {
    @Test
    fun `test overlapper funksjon`() {
        val ønsketPeriode = 10.januar(2024)..15.januar(2024)

        // Overlapper ikke
        (5.januar(2024)..9.januar(2024)).overlapper(ønsketPeriode) shouldBe false

        // Overlapper på start
        (5.januar(2024)..10.januar(2024)).overlapper(ønsketPeriode) shouldBe true
        (5.januar(2024)..12.januar(2024)).overlapper(ønsketPeriode) shouldBe true

        // Overlapper hele
        (5.januar(2024)..22.januar(2024)).overlapper(ønsketPeriode) shouldBe true

        // Overlapper halen
        (10.januar(2024)..22.januar(2024)).overlapper(ønsketPeriode) shouldBe true
        (15.januar(2024)..22.januar(2024)).overlapper(ønsketPeriode) shouldBe true

        // Overlapper første/siste dag
        (10.januar(2024)..10.januar(2024)).overlapper(ønsketPeriode) shouldBe true
        (15.januar(2024)..15.januar(2024)).overlapper(ønsketPeriode) shouldBe true
    }
}
