package laad

import laad.webclient.*
import java.util.*

class WeightedUserScripts(
    private val scenarios: List<Pair<Int, UserScript>>): UserScript {
    init {
        require(scenarios.isNotEmpty()) { "Expected at least one scenario." }
    }
    private val weighted = WeightedIterator.of(this.scenarios)

    override suspend fun runSession() {
        weighted.next().runSession()
    }
}

fun weighted(vararg scenarios: Pair<Int, UserScript>) = weighted(scenarios.toList())
fun weighted(scenarios: List<Pair<Int, UserScript>>) = WeightedUserScripts(scenarios)

private class WeightedIterator<E>(private val random: Random = Random()) {
    private val map = TreeMap<Double, E>()

    private var total = 0.0

    private fun add(weight: Double, result: E): WeightedIterator<E> {
        if (weight <= 0) return this
        total += weight
        map[total] = result
        return this
    }

    operator fun next(): E {
        val value = random.nextDouble() * total
        return map.higherEntry(value).value
    }

    companion object {
        fun <E> of(scenarios: List<Pair<Int, E>>): WeightedIterator<E> {
            return WeightedIterator<E>().apply {
                for((weight, scenario) in scenarios){
                    this.add(weight.toDouble(), scenario)
                }
            }
        }
    }
}

class OtherExampleUserScript(): WebClientUserScript() {
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
