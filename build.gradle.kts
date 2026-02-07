plugins {
    java
    application
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.graalvm)
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.versionsPlugin)
    alias(libs.plugins.shadow)
}

group = "dev.nthings.helm4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

application {
    mainClass = "dev.nthings.helm4j.App"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
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
    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.jul.slf4j)
    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j2.impl)

    // Guava
    implementation(libs.guava)

    // Jackson
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)

    // JUnit
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Mockito
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.6".toBigDecimal()
            }
        }
    }
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
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

        googleJavaFormat("1.34.1")
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

// GraalVM Native Image configuration
graalvmNative {
    metadataRepository {
        enabled.set(true)
        version.set("0.3.33")
    }

    binaries {
        all {
            buildArgs.addAll(listOf(
                "-H:ConfigurationFileDirectories=${project.layout.buildDirectory.get().asFile}/native/agent-config",
                "--add-exports=java.base/sun.security.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED"
            ))
        }

        named("main") {
            verbose.set(true)
            imageName.set("${project.name}")
            mainClass.set("dev.nthings.helm4j.App")
        }
    }
}

tasks.cyclonedxDirectBom {
    includeConfigs.set(listOf("runtimeClasspath", "compileClasspath"))
    includeMetadataResolution.set(true)
    includeBuildSystem.set(true)
}

// --- Native image metadata collection via tracing agent ---
val agentSessionDir = layout.buildDirectory.dir("native/agent-output/run")
val agentMergedDir = layout.buildDirectory.dir("native/agent-config")

tasks.register<JavaExec>("runWithAgent") {
    group = "native"
    description = "Run the app with GraalVM native-image agent to collect metadata."

    // Ensure we run with a GraalVM JDK that provides the agent library
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    })

    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = project.projectDir

    // Collect metadata into build/native/agent-output/run (plugin will also accept this layout)
    jvmArgs = listOf(
        "-agentlib:native-image-agent=config-output-dir=${agentSessionDir.get().asFile.absolutePath},builtin-caller-filter=true,builtin-heuristic-filter=true,experimental-unsafe-allocation-support=true,track-reflection-metadata=true"
    )
}

tasks.register<Copy>("copyNativeAgentConfig") {
    group = "native"
    description = "Merge native-agent JSON configs into a single directory for native-image."
    dependsOn("runWithAgent")

    from(fileTree(agentSessionDir) {
        include("**/*.json")
    })
    into(agentMergedDir)
}

// Ensure nativeCompile consumes the collected configuration
tasks.named("nativeCompile") {
    dependsOn("copyNativeAgentConfig")
}
