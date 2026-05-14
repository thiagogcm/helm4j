rootProject.name = "helm4j"

include("helm4j-api", "helm4j-native")

buildCache {
    local {
        isEnabled = true
    }
}
