plugins {
    `java-library`
    jacoco
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
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

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
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
