package laad

import java.time.Instant
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

sealed interface Event

data class CallEvent(val session: Session, val call: String, val outcome: Outcome, val start: Instant, val end: Instant): Event

data class Session(val scenario: String, val userId: Long, val startTime: Instant): AbstractCoroutineContextElement(Session) {
    companion object Key : CoroutineContext.Key<Session>
}

data class StartUser(val session: Session): Event

data class EndUser(val session: Session, val time: Instant): Event

data class UnhandledError(val exceptionClass: KClass<out Throwable>, val time: Instant): Event

sealed interface Outcome

object Success: Outcome {
    override fun toString(): String = this::class.simpleName!!
}

sealed interface Failure: Outcome

object TimedOut: Failure {
    override fun toString(): String = this::class.simpleName!!
}

data class HttpStatus(val code: Int): Failure

data class Connect(val exceptionClass: KClass<out Throwable>): Failure

data class ExceptionFailure(val exceptionClass: KClass<out Throwable>): Failure

