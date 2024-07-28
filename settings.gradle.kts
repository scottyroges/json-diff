rootProject.name = "json-diff"

pluginManagement {
    val kotlinVersion: String by settings
    val jitpackRemoteRepo: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("io.spring.dependency-management") version "1.0.11.RELEASE"
        id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri(jitpackRemoteRepo) }
    }
}
