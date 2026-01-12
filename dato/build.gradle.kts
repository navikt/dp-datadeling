plugins {
    id("common")
    `java-library`
}

dependencies {
    testImplementation(libs.kotest.assertions.core)
}
