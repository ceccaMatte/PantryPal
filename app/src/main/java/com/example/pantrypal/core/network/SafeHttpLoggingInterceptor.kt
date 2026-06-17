package com.example.pantrypal.core.network

import android.util.Log
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

class SafeHttpLoggingInterceptor(
    private val logger: (String) -> Unit = { message -> Log.d("PantryPalNetwork", message) }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = System.nanoTime()
        return try {
            val response = chain.proceed(request)
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            logger(safeHttpLogMessage(request.method, request.url.host, request.url.encodedPath, response.code.toString(), durationMs))
            response
        } catch (error: Exception) {
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            logger(safeHttpLogMessage(request.method, request.url.host, request.url.encodedPath, "failed", durationMs))
            throw error
        }
    }
}

internal fun safeHttpLogMessage(
    method: String,
    host: String,
    encodedPath: String,
    result: String,
    durationMs: Long
): String = "$method $host$encodedPath -> $result in ${durationMs}ms"
