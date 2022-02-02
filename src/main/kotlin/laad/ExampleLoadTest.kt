package laad

import kotlinx.coroutines.runBlocking

private fun main() = runBlocking<Unit> {

    LoadTest(
        userScript = ExampleUserScript(),
        eventProcessor = consoleEventProcessor(),
        shape = {
        red("every second increase by 1")
        for (i in 1 .. 10) {
            goTo(i)
            delay(1.s)
        }
        red("hold steady for 5 seconds")
        delay(5.s)
        red("steep increase for 3 seconds")
        goTo(20)
        delay(3.s)

        red("currently ${getRunningSessions()} # sessions")

        red("back to steady for 3 seconds")
        goTo(10)
        delay(3.s)

        red("cool down for 5 seconds")
        goTo(0)
        delay(5.s)


    }).runLoadTest()

}
