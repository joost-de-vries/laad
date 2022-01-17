package laad.webclient

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import laad.*
import org.springframework.http.ResponseCookie
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import kotlin.coroutines.coroutineContext

fun WebClientScenario(scenario: suspend () -> Unit): Scenario {
    return Scenario {
        withContext(coroutineContext + ExceptionToOutcome(::webclientExceptionToOutcome)) {
            scenario.invoke()
        }
    }
}

fun webclientExceptionToOutcome(exception: Exception): Outcome? = when(exception) {
        is WebClientResponseException -> HttpStatus(exception.rawStatusCode)
        is WebClientRequestException -> Connect(exception::class)
        else -> null
    }

suspend fun WebClient.login() =
    post().uri("/login")
        .bodyToMonoWithCookies<String>()
        .awaitSingle()

suspend fun WebClient.addItem() =
    post().uri("/add-item")
        .bodyToMonoWithCookies<String>()
        .awaitSingle()

suspend fun WebClient.toPayment() =
    post().uri("/to-payment")
        .bodyToMonoWithCookies<String>()
        .awaitSingle()

inline fun <reified T: Any> WebClient.RequestHeadersSpec<out WebClient.RequestHeadersSpec<*>>.bodyToMonoWithCookies(): Mono<Pair<T, MultiValueMap<String, ResponseCookie>>> =
   exchangeToMono { response ->
    if (response.statusCode().is2xxSuccessful) {
        response.bodyToMono(T::class.java).map { it to response.cookies() }
    } else {
        response.createException().flatMap {
            Mono.error(it)
        }
    }
}

fun createWebClient(): WebClient =
    WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(HttpClient.create()))
        .baseUrl("http://localhost:9999").build()
