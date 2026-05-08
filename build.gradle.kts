plugins {
    java
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.versionsPlugin)
}

group = "dev.nthings.helm4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets {
    named("main") {
        java {
            srcDir("src/main/generated")
        }
    }
}

dependencies {
    // Logging API
    implementation(libs.slf4j.api)

    // Jackson
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)

    // JUnit
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Test logging backend
    testRuntimeOnly(platform(libs.log4j.bom))
    testRuntimeOnly(libs.log4j.api)
    testRuntimeOnly(libs.log4j.core)
    testRuntimeOnly(libs.log4j.slf4j2.impl)
}

val jacocoExcludes = listOf("dev/nthings/helm4j/jextract/**")

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoExcludes)
        }
    )
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoExcludes)
        }
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

spotless {

    java {
        target(
            "src/main/java/**/*.java",
            "src/main/generated/**/*.java",
            "src/test/java/**/*.java"
        )

        removeUnusedImports()
        forbidWildcardImports()

        googleJavaFormat("1.35.0")
            .reflowLongStrings()
            .formatJavadoc(true)
            .reorderImports(false)

        importOrder(
            "java|javax|jakarta",
            "",
            "com",
            "",
            "dev",
            "",
            "io",
            "",
            "net",
            "",
            "org.apache",
            "",
            "org.slf4j",
            "",
            "org",
            "",
            "tools",
            "",
            "\\#"
        )

        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
