package laad

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.coroutineContext

interface Scenario {
    suspend fun runSession()
}

interface EventScenario: Scenario {
    val events: SendChannel<Event>
}

abstract class AbstractScenario: EventScenario {

    @Suppress("INVISIBLE_REFERENCE")
    suspend fun <A> call(name: String, timeout: Duration = Duration.ofSeconds(1), block: suspend () -> A): A? {
        val result: A
        val start = Instant.now()
        var outcome: Outcome? = null
        return try {
            result = withTimeout(timeout.toMillis()) {
                block()
            }
            outcome = Success
            result
        } catch (e: Exception) {
            outcome = toOutcome(e) ?: when(e) {
                is TimeoutCancellationException -> TimedOut
                is JobCancellationException -> null
                else -> Unknown(e::class)
            }
            null
        }
        finally {
            val end = Instant.now()
            val session = coroutineContext[Session] ?: throw IllegalArgumentException("Session not found in coroutine context")
            outcome?.let { CallEvent(session, name, outcome, start, end) }?.let {
                events.send(it)
            }
        }
    }
    open fun toOutcome(e:Exception): Outcome? = null
}

fun CoroutineScope.consoleEventProcessor() = actor<Event> {
    for(event in channel){
        println(event)
    }
}

class SimpleExampleScenario(logins: List<Int>, val seconds: Int): Scenario {
    val iterator = sequence<Int> {
        while(true){
            yieldAll(logins)
        }
    }.iterator()

    override suspend fun runSession() {
        val login = iterator.next()
        for(i in 0..seconds){
            //println("running $id, with login $login")
            delay(1000)
        }
    }
}

private fun main() = runBlocking<Unit> {
    val scenario = SimpleExampleScenario(listOf(1), 4)
    for(i in 1..10){
        with(scenario){
            runSession()
        }
    }

    delay(15000)
}
