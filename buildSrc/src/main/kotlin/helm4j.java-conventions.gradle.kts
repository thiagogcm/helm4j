import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
    jacoco
    id("com.diffplug.spotless")
}

val libs = the<VersionCatalogsExtension>().named("libs")

val conventions = extensions.create<Helm4jConventionsExtension>("helm4jConventions")
conventions.coverageMinimum.convention("0.85".toBigDecimal())
conventions.coverageExclusions.convention(emptyList())

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    // Compile and publish each source set with a module-info as a real JPMS module, so
    // module-descriptor mistakes surface at build time and the jars are modular. (Whitebox
    // tests still run on the classpath, patched into the module they exercise.)
    modularity.inferModulePath = true
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("junit-jupiter-params").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())

    testRuntimeOnly(platform(libs.findLibrary("log4j-bom").get()))
    testRuntimeOnly(libs.findLibrary("log4j-api").get())
    testRuntimeOnly(libs.findLibrary("log4j-core").get())
    testRuntimeOnly(libs.findLibrary("log4j-slf4j2-impl").get())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

fun coveredClasses() =
    sourceSets["main"].output.asFileTree.matching {
        exclude(conventions.coverageExclusions.get())
    }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(coveredClasses())
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(coveredClasses())
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = conventions.coverageMinimum.get()
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
