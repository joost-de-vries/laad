package laad
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.time.Duration

sealed interface RunnerMessage

object Stop: RunnerMessage

data class GoTo(val concurrent: Int): RunnerMessage

fun CoroutineScope.runScenario(scenario: Scenario, tick: Duration = Duration.seconds(3)) = actor<RunnerMessage> {
    var concurrent = 0
    val jobs = mutableListOf<Job>()

    fun checkJobs() {
        val start = jobs.size
        jobs.removeIf { !it.isActive }
        if(jobs.size < start) println("removed ${start - jobs.size} finished jobs")
        println("checking jobs. current: ${jobs.size}, desired: ${concurrent}")
        val toAdd = concurrent - jobs.size
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
    }
    fun processMessages() {
//        println("process messages")
        do {
            val tryMsg = channel.tryReceive()
            if(tryMsg.isFailure || tryMsg.isClosed){
//                println("done processing messages")
//                println("try receive is $tryMsg")
                break
            }

            val msg = tryMsg.getOrThrow()
            when(msg){
                is Stop -> {
                    println("stop")
                    coroutineContext[Job]?.cancel()
                }
                is GoTo -> {
                    //println("update state to ${msg.concurrent}")
                    concurrent = msg.concurrent
                }
            }

        } while (tryMsg.isSuccess)
    }

    while(isActive){
        checkJobs()
        processMessages()
        delay(tick)
    }
}

val Int.s
    get() = Duration.seconds(this)

fun red(msg: String) = System.err.println(msg)
