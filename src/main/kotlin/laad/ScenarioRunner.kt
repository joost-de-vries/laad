package laad
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.time.Duration

class ScenarioRunner(private val channel: SendChannel<RunnerMessage>) {
    suspend fun goTo(desired: Int) = channel.send(GoTo(desired))

    suspend fun stop() = channel.send(Stop)

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
    val runner = InternalCoroutineRunner(0, mutableListOf(), RunnableScenario(scenario))

    fun checkSessions() {
        val removed = runner.removeNonActiveSessions()
        if (removed > 0) println("removed $removed finished sessions")

        with(runner) { adjustSessions() }
    }

    fun processMessages() {
        do {
            val tryMsg = channel.tryReceive()
            if (tryMsg.isFailure || tryMsg.isClosed) {
                break
            }

            when (val msg = tryMsg.getOrThrow()) {
                is GoTo -> {
                    runner.desired = msg.concurrent
                }
                is Stop -> {
                    println("stop")
                    coroutineContext[Job]?.cancel()
                }
                is GetActive -> {
                    msg.active.complete(runner.getActiveJobs())
                }
            }

        } while (tryMsg.isSuccess && isActive)
    }

    while(isActive){
        processMessages()
        checkSessions()
        delay(tick.toMillis())
    }
})

class InternalCoroutineRunner(
    var desired: Int,
    private val jobs: MutableList<Job>,
    private val scenario: RunnableScenario
) {
    fun removeNonActiveSessions(): Int {
        val before = jobs.size
        jobs.removeIf { !it.isActive }

        return before - jobs.size
    }

    fun CoroutineScope.adjustSessions(): Int {
        println("checking sessions. current: ${jobs.size}, desired: $desired")
        val toAdd = desired - jobs.size
        if(toAdd > 0) {
            for(i in 0 until toAdd){
                val job = with(scenario){ launchScenario(jobs.size + 1L) }
                jobs += job
            }
            println("started $toAdd sessions")
        } else if(toAdd < 0) {
            for(i in 0 until -toAdd) {
                val job = jobs.first()
                job.cancel()
                jobs.removeAt(0)
            }
            println("stopped ${-toAdd} sessions")
        } else {
            println("steady as she goes")
        }

        return toAdd
    }

    fun getActiveJobs(): Int {
        removeNonActiveSessions()
        return jobs.size
    }
}
val Int.s
    get() = Duration.ofSeconds(this.toLong())
suspend fun delay(duration: Duration): Unit = delay(duration.toMillis())

fun red(msg: String) = System.err.println(msg)
