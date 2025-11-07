plugins {
    id("common")
}

dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":dato"))
    implementation(libs.bundles.ktor.client)

    implementation(libs.ktor.serialization.jackson)
    implementation(libs.bundles.jackson)

    testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
}
