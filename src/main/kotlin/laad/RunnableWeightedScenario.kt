package laad

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import laad.webclient.*
import java.util.*

class RunnableWeightedScenario(
    scenarios: List<Pair<Int, EventScenario>>): RunnableScenario {
    init {
        require(scenarios.isNotEmpty()) { "Expected at least one scenario." }
    }
    private val runnableScenarios = scenarios.map { (weight, scenario) -> weight to DefaultRunnableScenario(scenario) }
    private val weighted = WeightedIterator.of(runnableScenarios)

    override fun CoroutineScope.launchSession(sessionId: Long): Job = with(weighted.next()) { launchSession(sessionId)}
}

fun weighted(scenarios: List<Pair<Int, EventScenario>>) = RunnableWeightedScenario(scenarios)

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
        fun of(scenarios: List<Pair<Int, RunnableScenario>>): WeightedIterator<RunnableScenario> {
            return WeightedIterator<RunnableScenario>().apply {
                for((weight, scenario) in scenarios){
                    this.add(weight.toDouble(), scenario)
                }
            }
        }
    }
}

class OtherExampleScenario(override val events: SendChannel<Event>): WebClientScenario() {
    private val webclient = createWebClient()

    override suspend fun runSession() {
        var response = call("login") { webclient.login() }
        kotlinx.coroutines.delay(1000)

        response = call("add item") { webclient.addItem() }
        kotlinx.coroutines.delay(1000)

        response = call("to payment") { webclient.toPayment() }
        kotlinx.coroutines.delay(1000)
    }
}
