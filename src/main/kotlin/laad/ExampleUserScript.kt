package laad

import laad.webclient.*

class ExampleUserScript: WebClientUserScript() {
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

class FailingUserScript: WebClientUserScript() {
    private val webclient = createWebClient()

    override suspend fun runSession() {
        try {
            call<String>("login") {
                if(1 > 0){
                    throw Boom()

                }
                ""
            }
            delay(1.s)
            call("add item") { webclient.addItem() }

            call("to payment") { webclient.toPayment() }
            delay(1.s)

        } catch (e: java.lang.Exception){
            System.err.println("after boom")
            call("add item anonymously") { webclient.addItem() }
        }
    }
}
class Boom: Exception("BOOM")