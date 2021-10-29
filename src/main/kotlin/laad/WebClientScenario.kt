package laad

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.ResponseCookie
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import kotlin.time.Duration
import kotlin.time.TimeSource

class WebClientScenario(private val duration: Duration, override val timeout: Duration, override val events: SendChannel<Event>): AbstractScenario() {
    private val webclient = createWebClient()

    override fun CoroutineScope.launchScenario(id: Int): Job =
        launch {
            val startTime = TimeSource.Monotonic.markNow()
            do {
                val (response, cookies) = call("login") { webclient.callGetString() }
//                println(response)
                delay(1000)
            } while (isActive && startTime.elapsedNow() < duration)
        }

    override fun toOutcome(e:Exception): Outcome? = when(e) {
        is WebClientResponseException -> HttpStatus(e.rawStatusCode)
        is WebClientRequestException -> Connect(e::class)
        else -> null
    }
}

private fun main() = runBlocking<Unit> {
    val events = actor<Event> {
        for(event in channel){
            println(event)
        }
    }
    val scenario = WebClientScenario(Duration.seconds(5), Duration.seconds(1), events)
    for(i in 1..10){
        with(scenario){
            launchScenario(i)
        }
    }

    delay(15000)
    events.close()
}

suspend fun WebClient.callGetString() =
    get().uri("/get-string")
        .bodyToMonoWithCookies<String>()
        .awaitSingle()

inline fun <reified T: Any> WebClient.RequestHeadersSpec<out WebClient.RequestHeadersSpec<*>>.bodyToMonoWithCookies() =
   exchangeToMono<Pair<T, MultiValueMap<String, ResponseCookie>>> { response ->
    if (response.statusCode().is2xxSuccessful) {
        response.bodyToMono(T::class.java).map { it to response.cookies() }
    } else {
        response.createException().flatMap {
            Mono.error(it)
        }
    }
}

fun createWebClient() =
    WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(HttpClient.create()))
        .baseUrl("http://localhost:9999").build()
