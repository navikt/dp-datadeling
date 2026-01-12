package no.nav.dagpenger.dato

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DatoperiodeTest {
    @Test
    fun `overlapper med periode som er helt innenfor`() {
        val periode1 = Datoperiode(1.januar(2023), 31.januar(2023))
        val periode2 = Datoperiode(10.januar(2023), 20.januar(2023))

        periode1.overlapperMed(periode2) shouldBe true
        periode2.overlapperMed(periode1) shouldBe true
    }

    @Test
    fun `overlapper med periode som starter før og slutter inne i perioden`() {
        val periode1 = Datoperiode(10.januar(2023), 31.januar(2023))
        val periode2 = Datoperiode(1.januar(2023), 15.januar(2023))

        periode1.overlapperMed(periode2) shouldBe true
    }

    @Test
    fun `overlapper med periode som starter inne i perioden og slutter etter`() {
        val periode1 = Datoperiode(1.januar(2023), 20.januar(2023))
        val periode2 = Datoperiode(15.januar(2023), 31.januar(2023))

        periode1.overlapperMed(periode2) shouldBe true
    }

    @Test
    fun `overlapper nøyaktig på startdato`() {
        val periode1 = Datoperiode(1.januar(2023), 10.januar(2023))
        val periode2 = Datoperiode(10.januar(2023), 20.januar(2023))

        periode1.overlapperMed(periode2) shouldBe true
    }

    @Test
    fun `overlapper ikke med periode før`() {
        val periode1 = Datoperiode(1.februar(2023), 28.februar(2023))
        val periode2 = Datoperiode(1.januar(2023), 31.januar(2023))

        periode1.overlapperMed(periode2) shouldBe false
    }

    @Test
    fun `overlapper ikke med periode etter`() {
        val periode1 = Datoperiode(1.januar(2023), 31.januar(2023))
        val periode2 = Datoperiode(1.februar(2023), 28.februar(2023))

        periode1.overlapperMed(periode2) shouldBe false
    }

    @Test
    fun `default tilOgMed er LocalDate MAX`() {
        val periode = Datoperiode(1.januar(2023))
        val langtFremITid = Datoperiode(1.januar(2099), 31.desember(2099))

        periode.overlapperMed(langtFremITid) shouldBe true
    }
}
