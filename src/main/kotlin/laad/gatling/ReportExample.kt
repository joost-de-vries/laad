package laad.gatling

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingPropertiesBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import laad.*

private fun main() = runBlocking<Unit> {

    val config = config()
    val scenarioRunner = runScenario(ExampleScenario(gatlingLoggingEventProcessor(config)), tick = 1.s)

    red("every second increase by 1")
    for(i in 1 .. 10){
        scenarioRunner.goTo(i)
        delay(1.s)
    }
    red("hold steady for 5 seconds")
    delay(5.s)
    red("steep increase for 3 seconds")
    scenarioRunner.goTo(20)
    delay(3.s)

    red("current active is ${scenarioRunner.getActive()}")

    red("back to steady for 3 seconds")
    scenarioRunner.goTo(10)
    delay(3.s)

    red("cool down for 5 seconds")
    scenarioRunner.goTo(0)
    delay(5.s)

    scenarioRunner.stop()
    coroutineContext[Job]?.cancel()
    generateReport(config)
}

fun config(): Config {
    val configuration: GatlingConfiguration =
        GatlingConfiguration.loadForTest(GatlingPropertiesBuilder().resultsDirectory("build/simulation").build())
    return config(configuration)
}
fun config(configuration: GatlingConfiguration): Config {
    val simulationId = "simulationId"
    return Config("laad.gatling.Example",simulationId, simulationId + System.currentTimeMillis(), configuration)
}
