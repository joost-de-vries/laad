package laad

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

interface Scenario {
    fun CoroutineScope.launchScenario(id: Int): Job
}

abstract class AbstractScenario: Scenario {
    protected abstract val events: SendChannel<Event>
    protected abstract val timeout: Duration

    @Suppress("INVISIBLE_REFERENCE")
    suspend fun <A> call(name: String, block: suspend () -> A): A {
        var result: TimedValue<A>? = null
        var outcome: Outcome? = null
        return try {
            result = measureTimedValue {
                withTimeout(timeout) {
                    block()
                }
            }
            outcome = Success
            result.value
        } catch (e: Exception) {
            outcome = toOutcome(e) ?: when(e){
                is TimeoutCancellationException -> TimedOut
                is JobCancellationException -> null
                else -> Unknown(e::class)
            }
            throw e
        }
        finally {
            outcome?.let { Event(name, outcome, result?.duration) }?.let {
                events.send(it)
            }
        }
    }
    open fun toOutcome(e:Exception): Outcome? = null
}

fun CoroutineScope.loggingEventProcessor() = actor<Event> {
    for(event in channel){
        println(event)
    }
}

class ExampleScenario(logins: List<Int>, val duration: Duration): Scenario {
    val iterator = sequence<Int> {
        while(true){
            yieldAll(logins)
        }
    }.iterator()

    override fun CoroutineScope.launchScenario(id: Int): Job =
        launch {
            val login = iterator.next()
            val startTime = TimeSource.Monotonic.markNow()
            do{
                //println("running $id, with login $login")
                delay(1000)
            } while (isActive && startTime.elapsedNow() < duration)
        }

}

private fun main() = runBlocking<Unit> {
    val scenario = ExampleScenario(listOf(1), Duration.seconds(10))
    for(i in 1..10){
        with(scenario){
            launchScenario(i)
        }
    }

    delay(15000)
}

