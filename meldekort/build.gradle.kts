plugins {
    id("common")
}

dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":dato"))
    implementation(project(path = ":ktor-client"))
    implementation(libs.kotlin.logging)
    testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
}
