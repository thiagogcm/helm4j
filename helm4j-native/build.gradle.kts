plugins {
    id("helm4j.java-conventions")
}

dependencies {
    api(project(":helm4j-spi"))

    implementation(libs.slf4j.api)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
}

sourceSets {
    named("main") {
        java {
            srcDir("src/main/generated")
        }
    }
}

// jextract bindings and the FFM bridge/provider/loader require the native library to execute,
// so they cannot be unit-tested without libhelm4j.so. The NativeRuntimeSmokeTest exercises them
// for real when the library is present.
helm4jConventions {
    coverageExclusions =
        listOf(
            "dev/nthings/helm4j/jextract/**",
            "dev/nthings/helm4j/internal/runtime/FfmHelmBridge*",
            "dev/nthings/helm4j/internal/runtime/FfmHelmGatewayProvider*",
            "dev/nthings/helm4j/internal/runtime/NativeLibrary*",
        )
}

tasks.test {
    // Whitebox tests run on the classpath (unnamed module); the published jar carries the
    // Enable-Native-Access manifest attribute for the named module.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    workingDir = rootProject.projectDir
}

tasks.jar {
    manifest {
        attributes("Enable-Native-Access" to "dev.nthings.helm4j.runtime")
    }
}
