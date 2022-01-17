package laad

import laad.webclient.*
import org.springframework.web.reactive.function.client.WebClient

fun ExampleScenario(): Scenario {
    val webclient = createWebClient()

    return WebClientScenario {
        var response = call("login") { webclient.login() }
        delay(1.s)

        response = call("add item") { webclient.addItem() }
        delay(1.s)

        response = call("to payment") { webclient.toPayment() }
        delay(1.s)
    }
}

fun OtherExampleScenario(): Scenario {
    val webclient: WebClient = createWebClient()

    return WebClientScenario {
        var response = call("login") { webclient.login() }
        delay(1.s)

        response = call("add item") { webclient.addItem() }
        delay(1.s)

        response = call("to payment") { webclient.toPayment() }
        delay(1.s)
    }
}

fun FailingScenario(): Scenario {
    val webclient = createWebClient()

    return WebClientScenario {
        try {
            call<String>("login") {
                if(1 > 0) {
                    throw Boom()
                }
                ""
            }
            delay(1.s)
            call("add item") { webclient.addItem() }

            call("to payment") { webclient.toPayment() }
            delay(1.s)

        } catch (e: Exception){
            System.err.println("after boom")
            call("add item anonymously") { webclient.addItem() }
        }
    }
}
class Boom: Exception("BOOM")