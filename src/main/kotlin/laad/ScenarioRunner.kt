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

    suspend fun getRunningSessions(): Int {
        val deferredCount = CompletableDeferred<Int>()
        channel.send(GetRunningSessions(deferredCount))
        return deferredCount.await()
    }
}

sealed interface RunnerMessage

object Stop: RunnerMessage

data class GoTo(val concurrent: Int): RunnerMessage

data class GetRunningSessions(val active: CompletableDeferred<Int>): RunnerMessage

fun CoroutineScope.runScenario(scenario: EventScenario, tick: Duration = Duration.ofSeconds(3)) =
    runScenario(DefaultRunnableScenario(scenario), tick)

fun CoroutineScope.runScenario(scenario: RunnableScenario, tick: Duration = Duration.ofSeconds(3)) = ScenarioRunner(actor {
    val sessions = Sessions(0, mutableListOf(), scenario)

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
                is GetRunningSessions -> {
                    msg.active.complete(sessions.getActiveJobs())
                }
            }

        } while (tryMsg.isSuccess && isActive)
    }

    while(isActive) {
        processMessages()
        with(sessions) { adjustSessions() }
        delay(tick)
    }
})

class Sessions(
    var desired: Int,
    private val jobs: MutableList<Job>,
    private val scenario: RunnableScenario
) {
    private var sessionCounter = 0L

    fun CoroutineScope.adjustSessions(): Int {
        val removed = removeNonActiveSessions()
        if (removed > 0) println("removed $removed finished sessions")

        println("checking sessions. current: ${jobs.size}, desired: $desired")
        val diff = desired - jobs.size
        if (diff > 0) {
            for (i in 0 until diff) {
                val job = with(scenario) { launchSession(sessionId = sessionCounter) }
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

    private fun removeNonActiveSessions(): Int {
        val before = jobs.size
        jobs.removeIf { !it.isActive }

        return before - jobs.size
    }

    fun getActiveJobs(): Int {
        removeNonActiveSessions()
        return jobs.size
    }
}

interface RunnableScenario {
    fun CoroutineScope.launchSession(sessionId: Long): Job
}

@Suppress("INVISIBLE_REFERENCE")
class DefaultRunnableScenario(private val scenario: EventScenario): RunnableScenario {

    override fun CoroutineScope.launchSession(sessionId: Long): Job {
        val session = Session(scenario::class.simpleName!!, sessionId, Instant.now())
        return launch(session) {
            scenario.events.send(StartUser(session))
            try {
                scenario.runSession()
            } catch (e: Exception) {
                if (e !is JobCancellationException) {
                    scenario.events.send(UnhandledError(e::class, Instant.now()))
                }
            } finally {
                scenario.events.send(EndUser(session, Instant.now()))
            }
        }
    }
}

val Int.s
    get() = Duration.ofSeconds(this.toLong())
suspend fun delay(duration: Duration): Unit = delay(duration.toMillis())

fun red(msg: String) = System.err.println(msg)


private fun main() = runBlocking<Unit> {
    val events = actor<Event> {
        for(event in channel){
            println(event)
        }
    }
    val scenario = DefaultRunnableScenario(ExampleScenario(events))
    for(i in 1..10L) {
        with(scenario) { launchSession(i) }
    }

    delay(15000)
    events.close()
}
