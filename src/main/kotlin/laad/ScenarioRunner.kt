package laad
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.time.Duration

sealed interface RunnerMessage

object Stop: RunnerMessage

data class GoTo(val concurrent: Int): RunnerMessage

data class GetActive(val active: CompletableDeferred<Int>): RunnerMessage

class ScenarioRunner(private val channel: SendChannel<RunnerMessage>) {
    suspend fun goTo(desired: Int) = channel.send(GoTo(desired))

    suspend fun stop() = channel.send(Stop)

    suspend fun getActive(): Int {
        val deferred = CompletableDeferred<Int>()
        channel.send(GetActive(deferred))
        return deferred.await()
    }
}

fun CoroutineScope.runScenario(scenario: Scenario, tick: Duration = Duration.seconds(3)) = ScenarioRunner(actor<RunnerMessage> {
    val runner = InternalCoroutineRunner(0, mutableListOf(), scenario)

    fun checkJobs() {

        val removed = runner.removeNonActiveJobs()
        if(removed > 0) println("removed $removed finished jobs")

        with(runner) { adjustJobs() }
    }

    fun processMessages() {
        do {
            val tryMsg = channel.tryReceive()
            if(tryMsg.isFailure || tryMsg.isClosed){
                break
            }

            val msg = tryMsg.getOrThrow()

            when(msg){
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

        } while (tryMsg.isSuccess)
    }

    while(isActive){
        processMessages()
        checkJobs()
        delay(tick)
    }
})

class InternalCoroutineRunner(
    var desired: Int,
    val jobs: MutableList<Job>,
    val scenario: Scenario
) {
    fun removeNonActiveJobs(): Int {
        val before = jobs.size
        jobs.removeIf { !it.isActive }

        return before - jobs.size
    }

    fun CoroutineScope.adjustJobs(): Int {
        println("checking jobs. current: ${jobs.size}, desired: $desired")
        val toAdd = desired - jobs.size
        if(toAdd > 0) {
            for(i in 0 until toAdd){
                val job = with(scenario){ launchScenario(jobs.size + 1) }
                jobs += job
            }
            println("started $toAdd scenarios")
        } else if(toAdd < 0) {
            for(i in 0 until -toAdd) {
                val job = jobs.first()
                job.cancel()
                jobs.removeAt(0)
            }
            println("stopped ${-toAdd} scenario's")
        } else {
            println("steady as she goes")
        }

        return toAdd
    }

    fun getActiveJobs(): Int {
        removeNonActiveJobs()
        return jobs.size
    }
}
val Int.s
    get() = Duration.seconds(this)

fun red(msg: String) = System.err.println(msg)
