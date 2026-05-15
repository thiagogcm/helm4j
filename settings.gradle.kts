rootProject.name = "helm4j"

include("helm4j-api", "helm4j-spi", "helm4j-native", "helm4j-samples")

buildCache {
    local {
        isEnabled = true
    }
}
