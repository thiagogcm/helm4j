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
    // consumer would, with helm4j-runtime-native discovered via ServiceLoader on the module path.
    modularity.inferModulePath = true
}

dependencies {
    // Consumers compile against helm4j-client (helm4j-model + helm4j-spi come transitively)
    // and put helm4j-runtime-native on the runtime path. See docs/architecture.md.
    implementation(project(":helm4j-client"))
    runtimeOnly(project(":helm4j-runtime-native"))
}

application {
    mainModule = "dev.nthings.helm4j.samples"
    mainClass = "dev.nthings.helm4j.samples.HelloHelm"
    applicationDefaultJvmArgs = listOf("--enable-native-access=dev.nthings.helm4j.runtime.ffm")
}

val helloChart = layout.projectDirectory.dir("src/main/resources/charts/hello-world")

tasks.named<JavaExec>("run") {
    // NativeLibrary resolves libhelm4j.so relative to the working directory; the
    // repo-root layout (libhelm4j/libhelm4j.so) is the development default.
    workingDir = rootProject.projectDir
    systemProperty("helm4j.samples.chart", helloChart.asFile.absolutePath)
}

// helm4j-runtime is runtimeOnly (not in `requires`), so name it in --add-modules
// so jlink resolves it and ServiceLoader can find the FFM provider. Native access
// piggybacks on the `Enable-Native-Access` manifest attribute jlink preserves in
// the jimage; --add-options is a no-op on JDK 25 jlink.
val jlinkModulesDir = layout.buildDirectory.dir("jlink-modules")
val jlinkImageDir = layout.buildDirectory.dir("jlink/hello-helm")

val stageJlinkModules by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Stages modular jars (runtime classpath + own jar) for jlink."
    from(configurations.named("runtimeClasspath"))
    from(tasks.named<Jar>("jar"))
    into(jlinkModulesDir)
}

val jlink by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Builds a self-contained runtime image for the sample app via jlink."
    dependsOn(stageJlinkModules)

    val launcher = javaToolchains.launcherFor(java.toolchain)
    val jlinkExecutable = launcher.map {
        it.metadata.installationPath.file("bin/jlink").asFile.absolutePath
    }
    val jmodsDir = launcher.map {
        it.metadata.installationPath.dir("jmods").asFile.absolutePath
    }
    val outputDir = jlinkImageDir.map { it.asFile }
    val modulesDir = jlinkModulesDir.map { it.asFile }

    inputs.dir(modulesDir)
    outputs.dir(outputDir)

    doFirst {
        // jlink refuses to overwrite an existing output directory.
        outputDir.get().deleteRecursively()
        executable = jlinkExecutable.get()
        val modulePath = buildList {
            add(modulesDir.get().absolutePath)
            // Temurin ships without jmods; jlink falls back to run-time image linking.
            val jmods = File(jmodsDir.get())
            if (jmods.isDirectory) add(jmods.absolutePath)
        }.joinToString(File.pathSeparator)
        args(
            "--module-path",
            modulePath,
            "--add-modules",
            "dev.nthings.helm4j.samples,dev.nthings.helm4j.runtime.ffm",
            "--launcher",
            "hello-helm=dev.nthings.helm4j.samples/dev.nthings.helm4j.samples.HelloHelm",
            "--no-header-files",
            "--no-man-pages",
            "--strip-debug",
            "--compress",
            "zip-6",
            "--output",
            outputDir.get().absolutePath,
        )
    }

    // Placeholder so Exec validation passes; the real path is set in doFirst.
    executable = "jlink"
}

// Invokes bin/java directly rather than bin/hello-helm because the jlink launcher
// splices "$@" after `-m`, turning any args into program arguments instead of JVM
// options — so passing `-Dprop=value` to the launcher does not set a system property.
val runJlink by tasks.registering(Exec::class) {
    group = "application"
    description = "Runs the jlink-produced image against the bundled chart."
    dependsOn(jlink)

    val javaFile = jlinkImageDir.map { it.file("bin/java").asFile }
    inputs.file(javaFile)
    workingDir = rootProject.projectDir

    doFirst {
        executable = javaFile.get().absolutePath
        args(
            "-Dhelm4j.samples.chart=${helloChart.asFile.absolutePath}",
            "-Dhelm4j.library.path=${rootProject.projectDir}/libhelm4j",
            "-m",
            "dev.nthings.helm4j.samples/dev.nthings.helm4j.samples.HelloHelm",
        )
    }

    // Placeholder so Exec validation passes; the real path is set in doFirst.
    executable = "java"
}

// Native image build. The graalvm-buildtools plugin reads the entry point from
// the application{} block above, so the same configuration drives `gradle run`
// and `gradle nativeCompile`. native-image flattens helm4j-runtime-native onto
// the classpath; the HelmEngineProvider is discovered through the
// META-INF/services descriptor in that module (a JPMS `provides` declaration is
// not honoured outside the module layer).
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
