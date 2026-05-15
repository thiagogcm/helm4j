plugins {
    id("helm4j.java-conventions")
}

dependencies {
    api(project(":helm4j-model"))
    api(project(":helm4j-spi"))
}
