package laad

import kotlinx.coroutines.channels.SendChannel
import laad.webclient.*

class ExampleScenario(override val events: SendChannel<Event>): WebClientScenario() {
    private val webclient = createWebClient()

    override suspend fun runSession() {
        var response = call("login") { webclient.login() }
        delay(1.s)

        response = call("add item") { webclient.addItem() }
        delay(1.s)

        response = call("to payment") { webclient.toPayment() }
        delay(1.s)
    }
}
