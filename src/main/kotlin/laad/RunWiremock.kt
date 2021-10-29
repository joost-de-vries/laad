package laad

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

private fun main() {
    val server = startWiremockServer()

    server.stub(
        get(urlPathEqualTo("/get-string"))
            .returnsResponse("ok".toByteArray(), 200, MediaType.TEXT_PLAIN)
    )
}

fun startWiremockServer(port: Int = 9999) = WireMockServer(
    WireMockConfiguration.options()
    .port(port)
    .asynchronousResponseEnabled(true)
).apply {
    this.start()
}

fun WireMockServer.stub(mappingBuilder: MappingBuilder) =
    addStubMapping(stubFor(mappingBuilder))
typealias WithResponse = ResponseDefinitionBuilder.() -> ResponseDefinitionBuilder

fun MappingBuilder.returnsResponse(body: ByteArray?, statusCode: Int, contentType: MediaType, withResponse: WithResponse? = null) =
    willReturn(
        aResponse().apply {
            if (body != null) {
                withBody(
                    body
                )
            }
        }
            .withStatus(statusCode)
            .withLogNormalRandomDelay(750.0, 0.1)
            .withHeader(HttpHeaders.CONTENT_TYPE, contentType.toString())
            .let {
                if (withResponse != null) {
                    it.withResponse()
                } else {
                    it
                }
            }
    )
