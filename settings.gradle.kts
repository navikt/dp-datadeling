rootProject.name = "dp-datadeling"

include("openapi")

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20251205.234.05353f")
        }
    }
}

include("meldekort", "soknad", "dato", "behandling", "datadeling-api", "ktor-client")