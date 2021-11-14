package laad

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

class ExampleScenario(override val events: SendChannel<Event>): WebClientScenario() {
    private val webclient = createWebClient()

    override suspend fun runSession() {
        var response = call("login") { webclient.login() }
        delay(1000)

        response = call("add item") { webclient.addItem() }
        delay(1000)

        response = call("to payment") { webclient.toPayment() }
        delay(1000)
    }
}

private fun main() = runBlocking<Unit> {
    val events = actor<Event> {
        for(event in channel){
            println(event)
        }
    }
    val scenario = ExampleScenario(events)
    for(i in 1..10) {
        launch { scenario.runSession() }
    }

    delay(15000)
    events.close()
}
