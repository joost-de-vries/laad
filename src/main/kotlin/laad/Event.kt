package laad

import kotlin.reflect.KClass
import kotlin.time.Duration

data class Event(val call: String, val outcome: Outcome, val time: Duration?)

sealed interface Outcome

object Success: Outcome

sealed interface Failure: Outcome

object TimedOut: Failure

data class HttpStatus(val code: Int): Failure

data class Connect(val exceptionClass: KClass<out Throwable>): Failure

data class Unknown(val exceptionClass: KClass<out Throwable>): Failure

