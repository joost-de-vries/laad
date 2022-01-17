package laad

import kotlinx.coroutines.runBlocking

private fun main() = runBlocking<Unit> {

    val scenarioRunner = runScenario(ExampleScenario(), consoleEventProcessor(), tick = 1.s)

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
}
