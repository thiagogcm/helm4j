rootProject.name = "helm4j"

include(
    "helm4j-model",
    "helm4j-spi",
    "helm4j-client",
    "helm4j-runtime-native",
    "helm4j-samples",
)

buildCache {
    local {
        isEnabled = true
    }
}
