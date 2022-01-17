package laad.gatling

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingPropertiesBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class ReportConfig(
    val simulationClassName: String,
    val simulationId: String,
    val runId: String,
    val flushLogEvery: Long,
    val gatlingConfiguration: GatlingConfiguration
)

inline fun config(simulationName: String): ReportConfig =
    config(simulationName,
    GatlingConfiguration.loadForTest(GatlingPropertiesBuilder().resultsDirectory("build/simulation").build())
)

inline fun config(simulationName: String, configuration: GatlingConfiguration): ReportConfig {

    val simulationId = "simulationId"
    return ReportConfig(simulationName,simulationId, runId(simulationId), 10000L, configuration)
}
fun runId(simulationId: String): String {
    val instant = Instant.now()
    return simulationId + formatter.format(instant)
}
private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.from(ZoneOffset.UTC))