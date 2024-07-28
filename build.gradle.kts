plugins {
    id("java-library")
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("jacoco")
    id("maven-publish")
}

fun resolveVersion(version: String): String = System.getenv("SNAPSHOT")?.ifBlank { version } ?: version

version = resolveVersion("1.0.0")
group = "com.scottyroges"

val junitVersion: String by project
val kotlinLoggingVersion: String by project
val kluentVersion: String by project

val jitpackRemoteRepo: String by project

repositories {
    mavenCentral()
    maven { url = uri(jitpackRemoteRepo) }
}

dependencies {
    // BOMs
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // json
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")

    // Tests
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:1.10.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
}

val copyNativeDeps by tasks.creating(Copy::class) {
    from(configurations.testRuntimeClasspath) {
        include("*.dylib")
        include("*.so")
        include("*.dll")
    }
    into("${layout.buildDirectory}/native-libs")
}

tasks.withType<Test> {
    dependsOn.add(copyNativeDeps)
    doFirst {
        systemProperty("sqlite4java.library.path", "${layout.buildDirectory}/native-libs")
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.1.1")
    additionalEditorconfig.set(
        mapOf(
            "max_line_length" to "off",
            "ktlint_standard_value-parameter-comment" to "disabled",
            "ktlint_standard_value-argument-comment" to "disabled",
            "ktlint_standard_multiline-expression-wrapping" to "disabled",
            "ktlint_standard_string-template-indent" to "disabled",
            "ktlint_standard_parameter-list-wrapping" to "disabled",
            "ktlint_standard_function-signature" to "disabled",
            "ktlint_standard_if-else-wrapping" to "disabled",
            "ktlint_standard_statement-wrapping" to "disabled",
            "ktlint_standard_try-catch-finally-spacing" to "disabled",
        ),
    )

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
    }
}
