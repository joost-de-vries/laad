package laad.gatling

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

    red("back to steady for 3 seconds")
    scenarioRunner.goTo(10)
    delay(3.s)

    coroutineContext[Job]?.cancel()
    generateReport(config)
}

