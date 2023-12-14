plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.22.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.22.0")
}
