package no.nav.dagpenger.behandling

object BehandlingsresultatScenarioer {
    // Hentet fra https://github.com/navikt/dp-behandling/blob/bf3f22a6f9f1e88bcbf9fd0657eeca34a3382928/mediator/src/test/kotlin/no/nav/dagpenger/behandling/scenario/ScenarioTest.kt#L291
    val innvilgelse_v1 by lazy {
        requireNotNull(
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultat_v1.json"),
        ) { "Kunne ikke finne testdata/behandlingresultat_v1.json" }
            .reader()
            .readText()
    }

    val endring_v1 by lazy {
        requireNotNull(
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultat_endring_v1.json"),
        ) { "Kunne ikke finne testdata/behandlingresultat_endring_v1.json" }
            .reader()
            .readText()
    }

    // Henter fra https://github.com/navikt/dp-behandling/blob/bf3f22a6f9f1e88bcbf9fd0657eeca34a3382928/mediator/src/test/kotlin/no/nav/dagpenger/behandling/scenario/BeregningTest.kt#L143
    val meldekortBeregning_v1 by lazy {
        requireNotNull(
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultatBasertPaa_v1.json"),
        ) { "Kunne ikke finne testdata/behandlingresultatBasertPaa_v1.json" }
            .reader()
            .readText()
    }

    // Hentet fra https://github.com/navikt/dp-behandling/blob/bf3f22a6f9f1e88bcbf9fd0657eeca34a3382928/mediator/src/test/kotlin/no/nav/dagpenger/behandling/scenario/ScenarioTest.kt#L311

    val stans_v1 by lazy {
        requireNotNull(
            this.javaClass
                .getResourceAsStream("/testdata/behandlingresultatMedStans_v1.json"),
        ) { "Kunne ikke finne testdata/behandlingresultatMedStans_v1.json" }
            .reader()
            .readText()
    }
}
