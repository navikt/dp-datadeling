plugins {
    id("common")
}
dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":dato"))
    implementation(project(path = ":ktor-client"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.kotlin.logging)

    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.rapids.and.rivers.test)
}
