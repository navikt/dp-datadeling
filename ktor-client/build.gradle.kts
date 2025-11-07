plugins {
    id("common")
}

dependencies {
    api(libs.bundles.ktor.client)
    api(libs.bundles.jackson)
    api(libs.ktor.serialization.jackson)

    testImplementation(libs.ktor.server.test.host)
}
