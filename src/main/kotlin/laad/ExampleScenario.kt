package laad

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import laad.webclient.*

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
