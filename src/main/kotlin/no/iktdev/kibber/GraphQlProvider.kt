package no.iktdev.kibber

import com.apollographql.apollo3.ApolloClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

abstract class GraphQlProvider() {

    abstract fun start(): Unit
    abstract fun close()

    abstract fun createClient(): ApolloClient
    fun httpClient(oAuthToken: String, userAgent: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(oAuthToken, userAgent))
            .build()
    }
    open fun defaultBuilder(): ApolloClient.Builder {
        return ApolloClient.Builder()
    }



    class AuthorizationInterceptor(private val token: String = "", val userAgent: String): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .addHeader("Authorization", token)
                .addHeader("User-Agent", userAgent)
                .build()

            return chain.proceed(request)
        }
    }


    class InvalidMeasurementData(override val message: String = "Invalid data received from service") : Exception()
}