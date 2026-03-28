import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("kapt") version "1.9.23"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("me.champeau.jmh") version "0.7.2"
}

group = "com.download"
version = "0.17.11" // x-release-please-version
description = "downloader-bot"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

jmh {
    jmhVersion = "1.37"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    implementation("com.bucket4j:bucket4j_jdk17-core:8.15.0")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.15.0")


    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.projectreactor:reactor-test")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    testImplementation("io.mockk:mockk:1.13.11")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.1")
    testImplementation("io.kotest:kotest-property-jvm:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.21.3"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


detekt {
    buildUponDefaultConfig = true
    allRules = false

    // config = files("$rootDir/config/detekt/detekt.yml")
    // baseline = file("$rootDir/config/detekt/baseline.xml")

    source.from("src/main/kotlin", "src/test/kotlin")
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<Detekt> {
        reports {
            html.required = true
            xml.required = true
            txt.required = false
            sarif.required = false
            md.required = false
        }
    }

    check {
        dependsOn(detekt, ktlintCheck)
    }

    jmhJar {
        isZip64 = true
    }

    register("format") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(ktlintFormat)
    }
}


ktlint {
    android = false
    outputToConsole = true
    ignoreFailures = false
    verbose = true

    filter {
        exclude("**/build/**")
        include("**/src/**/*.kt")
    }
}

kover {
    reports {
        total {
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }

            verify {
                rule {
                    bound {
                        minValue = 70
                        coverageUnits = CoverageUnit.LINE
                    }
                    bound {
                        minValue = 50
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
                        // drop compiler-generated nested/lambda classes from coverage
                        "*\$*",
                    )
                }
            }
        }
    }
}
