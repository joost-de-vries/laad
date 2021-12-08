package laad.gatling

import io.gatling.app.RunResult
import io.gatling.app.RunResultProcessor
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingPropertiesBuilder

fun generateReport(config: ReportConfig) {
    generateReport(config.gatlingConfiguration, config.runId)
}
fun generateReport(gatlingConfiguration: GatlingConfiguration, runId: String) {

    val runResult = RunResult(runId, false)
    RunResultProcessor(gatlingConfiguration).processRunResult(runResult)
//    val singleLogFileReader = LogFileReader(runId, configuration)
//    singleLogFileReader.requestGeneralStats(Option.none<String>().asScala(),Option.none<Group>().asScala(), Option.none<Status>().asScala()).min()
//
//    val reportsGenerationInputs = ReportsGenerationInputs(runId, singleLogFileReader, ScalaList.empty())
//    val indexFile = ReportsGenerator(configuration).generateFor(reportsGenerationInputs)
}
