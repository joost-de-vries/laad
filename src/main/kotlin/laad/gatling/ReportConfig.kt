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

fun config(): ReportConfig = config(
    GatlingConfiguration.loadForTest(GatlingPropertiesBuilder().resultsDirectory("build/simulation").build())
)

fun config(configuration: GatlingConfiguration): ReportConfig {
    fun runId(simulationId: String): String {
        val instant = Instant.now()
        return simulationId + formatter.format(instant)
    }

    val simulationId = "simulationId"
    return ReportConfig("laad.gatling.ReportExample",simulationId, runId(simulationId), 10000L, configuration)
}
private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.from(ZoneOffset.UTC))