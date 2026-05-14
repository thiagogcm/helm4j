plugins {
    id("helm4j.java-conventions")
}

dependencies {
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    testRuntimeOnly(platform(libs.log4j.bom))
    testRuntimeOnly(libs.log4j.api)
    testRuntimeOnly(libs.log4j.core)
    testRuntimeOnly(libs.log4j.slf4j2.impl)
}
