package laad

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration


private fun main() = runBlocking {
    val scenario = WebClientScenario(duration = Duration.seconds(3), timeout = Duration.seconds(1), loggingEventProcessor())

    val scenarioRunner = runScenario(scenario, 1.s)

    red("every second increase by 1")
    for(i in 1 .. 10){
        scenarioRunner.send(GoTo(i))
        delay(1.s)
    }
    red("hold steady for 5 seconds")
    delay(5.s)
    red("steep increase for 3 seconds")
    scenarioRunner.send(GoTo(20))
    delay(3.s)

    red("back to steady for 3 seconds")
    scenarioRunner.send(GoTo(10))
    delay(3.s)

    red("cool down for 5 seconds")
    scenarioRunner.send(GoTo(0))
    delay(5.s)

    scenarioRunner.send(Stop)
    coroutineContext[Job]?.cancel()
}
