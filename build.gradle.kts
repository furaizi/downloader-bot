import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.jmh)
}

group = "com.download"
version = "1.0.6" // x-release-please-version
description = "downloader-bot"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

jmh {
    jmhVersion =
        libs.versions.jmh.core
            .get()
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation(libs.spring.boot.starter.data.redis.reactive)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.docker.compose)
    developmentOnly(libs.spring.boot.devtools)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.kotlin.reflect)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactor)
//    implementation(libs.coroutines.jdk8)

    implementation(libs.kotlin.logging)
    implementation(libs.telegram.bot)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.okhttp)
    implementation(libs.retrofit)

    implementation(libs.bucket4j.core)
    implementation(libs.bucket4j.lettuce)

    testImplementation(kotlin("test"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.reactor.test)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.coroutines.test)

    testImplementation(libs.mockk)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.extensions.spring)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

detekt {
    buildUponDefaultConfig = true

    // config = files("$rootDir/config/detekt/detekt.yml")
    // baseline = file("$rootDir/config/detekt/baseline.xml")
}

tasks {
    test {
        useJUnitPlatform()
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
    verbose = true
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
