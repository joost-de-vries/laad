package laad

import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

fun interface LoadShape {
   suspend operator fun LoadTestShaper.invoke( )
}

class LoadTest(val userScript: UserScript, val eventProcessor: SendChannel<Event>?, val tick: Duration = Duration.ofSeconds(3), val shape: LoadShape) {
    suspend fun runLoadTest() = coroutineScope {
        red("launching actor")
        val shaper = runConstantActive(userScript, eventProcessor, tick)

        red("actor launched")
        val job = launch {
            with(shape) {
                shaper.invoke()
            }
            shaper.stop()

        }
        job.join()
        cancel()
    }
}