package laad

import kotlinx.coroutines.runBlocking
import laad.gatling.config
import laad.gatling.gatlingEventProcessor
import laad.gatling.generateReport

private fun main() = runBlocking<Unit> {
    val config = config(::ExampleScenario.name)
    val eventProcessor = gatlingEventProcessor(config)
    val scenarioRunner = runScenario(weighted(
        50 to ExampleScenario(),
        50 to OtherExampleScenario()
    ), eventProcessor, tick = 1.s)

    red("every second increase by 1")
    for (i in 1 .. 10) {
        scenarioRunner.goTo(i)
        delay(1.s)
    }
    red("hold steady for 5 seconds")
    delay(5.s)
    red("steep increase for 3 seconds")
    scenarioRunner.goTo(20)
    delay(3.s)

    red("currently ${scenarioRunner.getRunningSessions()} # sessions")

    red("back to steady for 3 seconds")
    scenarioRunner.goTo(10)
    delay(3.s)

    red("cool down for 5 seconds")
    scenarioRunner.goTo(0)
    delay(5.s)

    scenarioRunner.stop()
    generateReport(config)
}
