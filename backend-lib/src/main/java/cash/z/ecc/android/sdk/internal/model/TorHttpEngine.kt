package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.isInUIntRange
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI

class TorHttpEngine(override val config: TorHttpConfig) : HttpClientEngineBase("TorEngine") {
    init {
        require(config.tor != null) {
            "TorHttp requires a TorClient"
        }
        require(config.retryLimit in 0..255) {
            "retryLimit must be a valid 8-bit unsigned integer"
        }
    }

    private fun torClient(): TorClient {
        // Checked on construction.
        return config.tor!!
    }

    @OptIn(InternalAPI::class)
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        val requestTime = GMTDate()

        val response = when (data.method.value) {
            "GET" -> when (data.body) {
                is OutgoingContent.NoContent -> {
                    torClient().httpGet(
                        data.url.toString(),
                        data.headers.entries().flatMap { it.value.map { value -> JniHttpHeader(it.key, value) } },
                        config.retryLimit
                    )
                }

                else -> throw RuntimeException("HTTP GET does not support body")
            }

            "POST" -> when (data.body) {
                is OutgoingContent.NoContent -> {
                    torClient().httpPost(
                        data.url.toString(),
                        data.headers.entries().flatMap { it.value.map { value -> JniHttpHeader(it.key, value) } },
                        ByteArray(0),
                        config.retryLimit
                    )
                }

                is OutgoingContent.ByteArrayContent -> {
                    // TODO: Confirm why `data.body.contentType` and `data.body.headers`
                    //       are not included in `data.headers`.
                    val headers = HeadersBuilder()
                    headers.appendAll(data.headers)
                    headers.appendAll(data.body.headers)
                    data.body.contentType?.let { headers.append(HttpHeaders.ContentType, it.toString()) }
                    torClient().httpPost(
                        data.url.toString(),
                        headers.build().entries().flatMap { it.value.map { value -> JniHttpHeader(it.key, value) } },
                        (data.body as OutgoingContent.ByteArrayContent).bytes(),
                        config.retryLimit
                    )
                }

                else -> throw RuntimeException("HTTP POST requires ByteArray body")
            }

            else -> throw RuntimeException("Unsupported HTTP method " + data.method)
        }

        val headers = HeadersBuilder()
        response.headers.iterator().forEach { headers.append(it.key, it.value) }

        return HttpResponseData(
            statusCode = HttpStatusCode(value = response.status, description = ""),
            requestTime = requestTime,
            headers = headers.build(),
            version = HttpProtocolVersion.parse(response.version),
            body = ByteReadChannel(response.body),
            callContext = callContext
        )
    }
}