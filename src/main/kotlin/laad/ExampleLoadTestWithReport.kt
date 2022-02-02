package laad

import kotlinx.coroutines.runBlocking
import laad.gatling.config
import laad.gatling.gatlingEventProcessor
import laad.gatling.generateReport

private fun main() = runBlocking {
    val config = config<ExampleUserScript>()
    val eventProcessor = gatlingEventProcessor(config)
    val loadTest = runUserScript(ExampleUserScript(),eventProcessor)

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

    red("back to steady for 3 seconds")
    loadTest.goTo(10)
    delay(3.s)

    loadTest.stop()
    generateReport(config)
}
