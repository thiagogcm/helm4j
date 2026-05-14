import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.math.BigDecimal

/**
 * Per-module knobs for the `helm4j.java-conventions` plugin. Each module declares its own coverage
 * threshold and exclusions in one place instead of re-wiring Jacoco tasks by hand.
 */
interface Helm4jConventionsExtension {

    /** Minimum line-coverage ratio enforced by `jacocoTestCoverageVerification`. */
    val coverageMinimum: Property<BigDecimal>

    /** Ant-style class paths excluded from both the coverage report and verification. */
    val coverageExclusions: ListProperty<String>
}
