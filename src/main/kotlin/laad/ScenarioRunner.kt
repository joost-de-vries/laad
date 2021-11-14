package laad
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.coroutineContext

class ScenarioRunner(private val channel: SendChannel<RunnerMessage>) {
    suspend fun goTo(desired: Int) = channel.send(GoTo(desired))

    suspend fun stop() {
        channel.send(Stop)
        coroutineContext[Job]?.cancel()
    }

    suspend fun getActive(): Int {
        val deferred = CompletableDeferred<Int>()
        channel.send(GetActive(deferred))
        return deferred.await()
    }
}

sealed interface RunnerMessage

object Stop: RunnerMessage

data class GoTo(val concurrent: Int): RunnerMessage

data class GetActive(val active: CompletableDeferred<Int>): RunnerMessage

fun CoroutineScope.runScenario(scenario: EventScenario, tick: Duration = Duration.ofSeconds(3)) = ScenarioRunner(actor {
    val sessions = Sessions(0, mutableListOf(), RunnableScenario(scenario))

    fun checkSessions() {
        val removed = sessions.removeNonActiveSessions()
        if (removed > 0) println("removed $removed finished sessions")

        with(sessions) { adjustSessions() }
    }

    fun processMessages() {
        do {
            val tryMsg = channel.tryReceive()
            if (tryMsg.isFailure || tryMsg.isClosed) {
                break
            }

            when (val msg = tryMsg.getOrThrow()) {
                is GoTo -> {
                    sessions.desired = msg.concurrent
                }
                is Stop -> {
                    println("stop")
                    coroutineContext[Job]?.cancel()
                }
                is GetActive -> {
                    msg.active.complete(sessions.getActiveJobs())
                }
            }

        } while (tryMsg.isSuccess && isActive)
    }

    while(isActive) {
        processMessages()
        checkSessions()
        delay(tick.toMillis())
    }
})

class Sessions(
    var desired: Int,
    private val jobs: MutableList<Job>,
    private val scenario: RunnableScenario
) {
    private var sessionCounter = 0L

    fun removeNonActiveSessions(): Int {
        val before = jobs.size
        jobs.removeIf { !it.isActive }

        return before - jobs.size
    }

    fun CoroutineScope.adjustSessions(): Int {
        println("checking sessions. current: ${jobs.size}, desired: $desired")
        val diff = desired - jobs.size
        if (diff > 0) {
            for (i in 0 until diff) {
                val job = with(scenario) { launchSession(sessionCounter) }
                sessionCounter += 1
                jobs += job
            }
            println("started $diff sessions")
        } else if (diff < 0) {
            for (i in 0 until -diff) {
                val job = jobs.first()
                job.cancel()
                jobs.removeAt(0)
            }
            println("stopped ${-diff} sessions")
        } else {
            println("steady as she goes")
        }

        return diff
    }

    fun getActiveJobs(): Int {
        removeNonActiveSessions()
        return jobs.size
    }
}

@Suppress("INVISIBLE_REFERENCE")
class RunnableScenario(private val scenario: EventScenario) {

    fun CoroutineScope.launchSession(id: Long): Job {
        val session = Session(scenario::class.simpleName!!, id, Instant.now())
        return launch(session) {
            scenario.events.send(StartUser(session))
            try {
                scenario.runSession()
                scenario.events.send(EndUser(session, Instant.now()))
            } catch (e: Exception) {
                if (e !is JobCancellationException) {
                    scenario.events.send(UnhandledError(e::class, Instant.now()))
                }
            }
        }
    }
}

val Int.s
    get() = Duration.ofSeconds(this.toLong())
suspend fun delay(duration: Duration): Unit = delay(duration.toMillis())

fun red(msg: String) = System.err.println(msg)
