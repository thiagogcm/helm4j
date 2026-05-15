plugins {
    application
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
