plugins {
    application
    alias(libs.plugins.graalvmNative)
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    // Run as a real JPMS module so the samples exercise the SDK exactly as a modular
    // consumer would, with helm4j-native discovered via ServiceLoader on the module path.
    modularity.inferModulePath = true
}

dependencies {
    // Consumers compile against helm4j-spi (helm4j-api comes transitively) and put
    // helm4j-native on the runtime path. See docs/architecture.md.
    implementation(project(":helm4j-spi"))
    runtimeOnly(project(":helm4j-native"))
}

application {
    mainModule = "dev.nthings.helm4j.samples"
    mainClass = "dev.nthings.helm4j.samples.HelloHelm"
    applicationDefaultJvmArgs = listOf("--enable-native-access=dev.nthings.helm4j.runtime")
}

val helloChart = layout.projectDirectory.dir("src/main/resources/charts/hello-world")

tasks.named<JavaExec>("run") {
    // NativeLibrary resolves libhelm4j.so relative to the working directory; the
    // repo-root layout (libhelm4j/libhelm4j.so) is the development default.
    workingDir = rootProject.projectDir
    systemProperty("helm4j.samples.chart", helloChart.asFile.absolutePath)
}

// Native image build. The graalvm-buildtools plugin reads the entry point from
// the application{} block above, so the same configuration drives `gradle run`
// and `gradle nativeCompile`. native-image flattens helm4j-native onto the
// classpath; the HelmGatewayProvider is discovered through the legacy
// META-INF/services file in that module (a JPMS `provides` declaration is not
// honoured outside the module layer).
graalvmNative {
    binaries {
        named("main") {
            imageName = "hello-helm"
            sharedLibrary = false
            fallback = false
            verbose = false
            // The Substrate launcher honours -Dprop=value; pipe the chart path and
            // libhelm4j.so location through the same system properties used on HotSpot.
            runtimeArgs.addAll(
                "-Dhelm4j.samples.chart=${helloChart.asFile.absolutePath}",
                "-Dhelm4j.library.path=${rootProject.projectDir}/libhelm4j",
            )
            buildArgs.addAll(
                // FFM downcall handles are restricted-access. native-image flattens
                // the modular layout to a single classpath, so ALL-UNNAMED is what
                // the substrate launcher checks against at runtime.
                "--enable-native-access=ALL-UNNAMED",
                "-H:+UnlockExperimentalVMOptions",
                "-H:+ReportExceptionStackTraces",
            )
        }
    }

    // The reachability-metadata repository ships pre-validated metadata for popular
    // libraries (jackson, log4j2, slf4j); enabling it lets us avoid bundling those
    // configs ourselves and keeps the project-owned metadata small.
    metadataRepository {
        enabled = true
    }

    // `./gradlew -Pagent run` runs the sample on HotSpot with the tracing agent and
    // captures reflection / resource / foreign-function metadata. `metadataCopy`
    // then moves the captured config into src/main/resources/META-INF/native-image
    // where native-image picks it up automatically.
    agent {
        defaultMode = "standard"
        builtinHeuristicFilter = true
        builtinCallerFilter = true
        trackReflectionMetadata = true

        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("src/main/resources/META-INF/native-image/dev.nthings.helm4j/samples")
            mergeWithExisting = true
        }
    }
}
