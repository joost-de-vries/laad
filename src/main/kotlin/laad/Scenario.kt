package laad

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

//interface Scenario {
//    suspend fun runSession()
//}
fun interface Scenario {
    suspend operator fun invoke()
}

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
        outcome = coroutineContext[ExceptionToOutcome]?.toOutcome(e) ?: when(e) {
            is TimeoutCancellationException -> TimedOut
            is JobCancellationException -> null
            else -> ExceptionFailure(e::class)
        }
        null
    }
    finally {
        val end = Instant.now()
        val session = coroutineContext[Session] ?: throw IllegalArgumentException("Session not found in coroutine context")
        outcome?.let { CallEvent(session, name, outcome, start, end) }?.let {
            coroutineContext[EventChannel]?.events?.send(it)
        }
    }
}
typealias ToOutcome = (Exception) -> Outcome?
data class ExceptionToOutcome(private val _toOutcome: ToOutcome): AbstractCoroutineContextElement(ExceptionToOutcome) {
    fun toOutcome(e:Exception) = _toOutcome(e)
    companion object Key : CoroutineContext.Key<ExceptionToOutcome>
}

fun CoroutineScope.consoleEventProcessor(): SendChannel<Event> = actor {
    for(event in channel) {
        println(event)
    }
}
fun SimpleExampleScenario(logins: List<Int>, seconds: Int): Scenario {
    val iterator = sequence<Int> {
        while(true){
            yieldAll(logins)
        }
    }.iterator()

    return Scenario {
        val login = iterator.next()
        for(i in 0..seconds){
            println("running with login $login")
            delay(1000)
        }
    }
}

private fun main() = runBlocking<Unit> {

    val scenario = SimpleExampleScenario((0..10).toList(), 4)
    for(i in 1..10){
        scenario()
    }

    delay(15000)
}
