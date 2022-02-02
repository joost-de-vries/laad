package laad

import kotlinx.coroutines.runBlocking
import laad.gatling.config
import laad.gatling.gatlingEventProcessor
import laad.gatling.generateReport

private fun main() = runBlocking<Unit> {
    val config = config<ExampleUserScript>()
    val eventProcessor = gatlingEventProcessor(config)
    val loadTest = runUserScript(weighted(
        50 to ExampleUserScript(),
        50 to OtherExampleUserScript()
    ), eventProcessor)

    red("every second increase by 1")
    for (i in 1 .. 10) {
        loadTest.goTo(i)
        delay(1.s)
    }
    red("hold steady for 5 seconds")
    delay(5.s)
    red("steep increase for 3 seconds")
    loadTest.goTo(20)
    delay(3.s)

    red("currently ${loadTest.getRunningSessions()} # sessions")

    red("back to steady for 3 seconds")
    loadTest.goTo(10)
    delay(3.s)

    red("cool down for 5 seconds")
    loadTest.goTo(0)
    delay(5.s)

    loadTest.stop()
    generateReport(config)
}
