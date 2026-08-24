rootProject.name = "dp-datadeling"

include("openapi")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20260812.281")
        }
    }
}

include("meldekort", "soknad", "dato", "behandling", "datadeling-api", "ktor-client")