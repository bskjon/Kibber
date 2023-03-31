package no.iktdev.kibber

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.exception.MissingValueException
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import no.iktdev.tibber.TibberHomesQuery
import no.iktdev.tibber.TibberPulseSubscription
import no.iktdev.tibber.TibberSubscriptionUrlQuery
import java.security.InvalidParameterException


class Tibber(val oAuthToken: String, implementer: String): GraphQlProvider(), TibberCalls {

    val userAgent = "${implementer}/Kibber/${GetProperties().version()}"
    init {
        if (implementer.isBlank() || implementer.length < 2)
            throw InvalidParameterException("A implementer is required (ex: Homey, Hass)")
    }

    private var clientSubscriber: ApolloClient? = null
    override fun start() {
    }

    override fun close() {
        clientSubscriber?.close()
        clientSubscriber = null
    }

    override fun createClient(): ApolloClient {
        val http = httpClient(oAuthToken, userAgent)

        val default = defaultBuilder()
            .httpServerUrl("https://api.tibber.com/v1-beta/gql")
            .okHttpClient(http)
        return default.build()
    }

    fun createSubscriptionClient(socketUrl: String): ApolloClient {
        val headerMaps: Map<String, String> = mapOf(
            "Authorization" to oAuthToken,
            "User-Agent" to userAgent,
            "Sec-WebSocket-Protocol" to "graphql-transport-ws"
        )

        val default = defaultBuilder()
            .httpServerUrl("https://api.tibber.com/v1-beta/gql")
            .subscriptionNetworkTransport(WebSocketNetworkTransport.Builder()
                .addHeader("User-Agent", userAgent)
                .addHeader("Authorization", "Bearer ${oAuthToken}")
                .serverUrl(socketUrl)
                .protocol(GraphQLWsProtocol.Factory(
                    connectionPayload = {
                        mapOf("headers" to headerMaps)
                    }))
                .build())
        return default.build()
    }

    override suspend fun homes(): List<TibberHomesQuery.Home> {
        return createClient().use { it.query(TibberHomesQuery()).execute() }.data?.viewer?.homes?.filterNotNull() ?: emptyList()
    }

    override suspend fun homesWithLiveReader(): List<TibberHomesQuery.Home> {
        return homes().filter { it.features?.realTimeConsumptionEnabled?: false }
    }

    override var registeredLiveListener: TibberListener? = null
    override suspend fun readLiveWattage(homeId: String) {
        if (clientSubscriber != null) {
            throw RuntimeException("Can't open a new subscriber while another is active")
        }
        if (registeredLiveListener == null) {
            throw RuntimeException("Live subscription listener has not been registered!")
        }
        val data = createClient().query(TibberSubscriptionUrlQuery()).execute().data?.viewer?.websocketSubscriptionUrl ?: throw MissingValueException()
        val client = createSubscriptionClient(data)
        clientSubscriber = client
        val subscription = client.subscription(TibberPulseSubscription(homeId))
        subscription.toFlow()
            .retryWhen { e, attempt ->
                if (e is ApolloNetworkException) {
                    if (e.platformCause is ApolloWebSocketClosedException) {
                        val socket = e.platformCause as ApolloWebSocketClosedException
                        val terminate = when(socket.code) {
                            4500 -> {
                                withContext(Dispatchers.Default) {
                                    registeredLiveListener?.onRetryExited(attempt, e, socket.message ?: "")
                                }
                                false
                            }
                            4429 -> {
                                withContext(Dispatchers.Default) {
                                    registeredLiveListener?.onRetryExited(attempt, e, socket.message ?: "")
                                }
                                false
                            }
                            else -> true
                        }
                        if (terminate) return@retryWhen false
                    }
                }
                withContext(Dispatchers.Default) {
                    registeredLiveListener?.onMeasurementRetry(attempt, e)
                }
                Thread.sleep(1000)
                attempt < 5
            }
            .flowOn(Dispatchers.IO)
            .collect {
                val measurement = it.data?.liveMeasurement
                withContext(Dispatchers.Default) {
                    if (measurement != null) {
                        registeredLiveListener?.onMeasurementReceived(measurement)
                    } else
                        registeredLiveListener?.onInvalidMeasurementReceived()
                }

            }
    }


}