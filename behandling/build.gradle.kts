plugins {
    id("common")
}
dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":dato"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.rapids.and.rivers)
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.bundles.jackson)

    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.rapids.and.rivers.test)
}
