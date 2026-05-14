plugins {
    id("helm4j.java-conventions")
}

dependencies {
    api(project(":helm4j-api"))

    implementation(libs.slf4j.api)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    testRuntimeOnly(platform(libs.log4j.bom))
    testRuntimeOnly(libs.log4j.api)
    testRuntimeOnly(libs.log4j.core)
    testRuntimeOnly(libs.log4j.slf4j2.impl)
}

sourceSets {
    named("main") {
        java {
            srcDir("src/main/generated")
        }
    }
}

// jextract bindings and the FFM bridge/provider/loader require the native library to execute,
// so they cannot be unit-tested without libhelm4j.so; exclude them from coverage like jextract.
// The NativeRuntimeSmokeTest exercises them for real when the library is present.
val jacocoExcludes =
    listOf(
        "dev/nthings/helm4j/jextract/**",
        "dev/nthings/helm4j/internal/runtime/FfmHelmBridge*",
        "dev/nthings/helm4j/internal/runtime/FfmHelmGatewayProvider*",
        "dev/nthings/helm4j/internal/runtime/NativeLibrary*",
    )

tasks.test {
    // Tests run on the classpath (unnamed module); the published jar carries the
    // Enable-Native-Access manifest attribute for the named module.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    workingDir = rootProject.projectDir
}

tasks.jacocoTestReport {
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoExcludes)
        }
    )
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoExcludes)
        }
    )
}

tasks.jar {
    manifest {
        attributes("Enable-Native-Access" to "dev.nthings.helm4j.runtime")
    }
}
