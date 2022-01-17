package laad

import java.util.*

fun WeightedScenario(
    scenarios: List<Pair<Int, Scenario>>): Scenario {
    require(scenarios.isNotEmpty()) { "Expected at least one scenario." }
    val weighted = WeightedIterator.of(scenarios)

    return Scenario { weighted.next().invoke() }
}

fun weighted(vararg scenarios: Pair<Int, Scenario>) = weighted(scenarios.toList())
fun weighted(scenarios: List<Pair<Int, Scenario>>) = WeightedScenario(scenarios)

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
