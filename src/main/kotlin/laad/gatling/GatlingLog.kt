package laad.gatling

import akka.japi.Option
import io.gatling.commons.stats.Status
import io.gatling.core.stats.writer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor
import laad.*
import scala.Predef
import scala.collection.JavaConverters
import io.gatling.core.session.Session as GatlingSession
import scala.collection.immutable.List as ScalaList

@Suppress("INVISIBLE_REFERENCE")
fun CoroutineScope.gatlingEventProcessor(config: ReportConfig) = actor<Event> {
    val serializer = serializer(config)
    var count = 0L
    var flushed = 0L
    try {
        val runMessage = RunMessage(config.simulationClassName, config.simulationId, System.currentTimeMillis(), config.runId, "3.3.1")
        serializer.serialize(runMessage)
        for (event in channel) {
            serializer.serialize(event.toGatling())
            count += 1
            val div = count / config.flushLogEvery
            if (div > flushed) {
                serializer.writer().flush()
                flushed = div
            }
        }
    } finally {
        serializer.writer().close()
    }
}
private fun Event.toGatling(): LoadEventMessage = when(this) {
    is CallEvent -> {
        val msg = Option.option(outcome.toResponseCode()).asScala()
        ResponseMessage(session.scenario, session.userId, ScalaList.empty(),call, start.toEpochMilli(), end.toEpochMilli(), outcome.toStatus(), msg, msg)
    }
    is EndUser -> UserEndMessage(session.toGatling(), time.toEpochMilli())
    is StartUser -> UserStartMessage(session.toGatling())
    is UnhandledError -> ErrorMessage(exceptionClass.simpleName, time.toEpochMilli())
}

private val ok = Status.apply("OK")
private val ko = Status.apply("KO")

private fun Outcome.toStatus(): Status = when(this) {
    is Success -> ok
    is Failure -> ko
}

private fun Outcome.toResponseCode(): String? = when(this) {
    is Success -> null
    is Connect -> this::class.simpleName + exceptionClass.simpleName
    is HttpStatus -> code.toString()
    TimedOut -> this.toString()
    is ExceptionFailure -> this::class.simpleName + exceptionClass.simpleName
}

private fun serializer(config: ReportConfig): FileData {
    val writer = BufferedFileChannelWriter.apply(config.runId, config.gatlingConfiguration)

    return FileData(
         UserStartMessageSerializer(writer),
         UserEndMessageSerializer(writer),
         ResponseMessageSerializer(writer),
         GroupMessageSerializer(writer),
         ErrorMessageSerializer(writer),
        writer
    )

}
private fun FileData.serialize(msg:LoadEventMessage ) {
    when(msg) {
        is UserStartMessage -> userStartMessageSerializer().serialize(msg)
        is UserEndMessage -> userEndMessageSerializer().serialize(msg)
        is GroupMessage -> groupMessageSerializer().serialize(msg)
        is ResponseMessage -> responseMessageSerializer().serialize(msg)
        is ErrorMessage -> errorMessageSerializer().serialize(msg)
    }
}

private fun FileData.serialize(msg:RunMessage ) = RunMessageSerializer(writer()).serialize(msg)

private fun Session.toGatling() = GatlingSession(scenario, userId, startTime.toEpochMilli(), mapOf<String, Any>().asScala(), 0, ok, scala.collection.immutable.List.empty(), GatlingSession.NothingOnExit())

private fun <A, B> Map<A, B>.asScala() = JavaConverters.mapAsScalaMap(this).toMap(Predef.conforms())
