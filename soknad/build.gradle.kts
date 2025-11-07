plugins {
    id("common")
}

dependencies {
    implementation(project(path = ":openapi"))
    implementation(libs.kotlin.logging)
    api(libs.rapids.and.rivers)

    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(project(path = ":dato"))
    testImplementation(libs.mockk)
}
