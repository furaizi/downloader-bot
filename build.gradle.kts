import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

group = "com.download"
version = "0.3.0" // x-release-please-version
description = "downloader-bot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-docker-compose")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.bucket4j:bucket4j_jdk17-core:8.15.0")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.15.0")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.mockk:mockk:1.13.11")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    allRules = false

    // config = files("$rootDir/config/detekt/detekt.yml")
    // baseline = file("$rootDir/config/detekt/baseline.xml")

    source.from("src/main/kotlin", "src/test/kotlin")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

ktlint {
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    verbose.set(true)

    filter {
        exclude("**/build/**")
        include("**/src/**/*.kt")
    }
}

tasks.named("check") {
    dependsOn("detekt", "ktlintCheck")
}

tasks.register("format") {
    group = "verification"
    description = "Runs ktlintFormat and detekt (formatting rules via detekt-formatting are auto-fixed by ktlint)."
    dependsOn("ktlintFormat")
}

kover {
    reports {
        total {
            html { onCheck = true }
            xml { onCheck = true }

            verify {
                rule {
                    bound {
                        minValue = 0
                        coverageUnits = CoverageUnit.LINE
                    }
                    bound {
                        minValue = 0
                        coverageUnits = CoverageUnit.BRANCH
                    }
                }
            }

            filters {
                excludes {
                    classes(
                        "com.download.downloaderbot.DownloaderBotApplicationKt",
                        "com.download.downloaderbot.*.*Config*",
                        "com.download.downloaderbot.*.*Properties*",
                        "com.download.downloaderbot.*.*Media",
                    )
                }
            }
        }
    }
}
