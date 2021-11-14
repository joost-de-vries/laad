package laad

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.ResponseCookie
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient

abstract class WebClientScenario: AbstractScenario() {

    override fun toOutcome(e:Exception): Outcome? = when(e) {
        is WebClientResponseException -> HttpStatus(e.rawStatusCode)
        is WebClientRequestException -> Connect(e::class)
        else -> null
    }
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
