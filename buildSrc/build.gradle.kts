plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.0.4"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.4")
}
