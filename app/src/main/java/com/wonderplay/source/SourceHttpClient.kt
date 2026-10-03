package com.wonderplay.source

import com.wonderplay.domain.SourceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class SourceHttpClient(private val client: OkHttpClient = sharedClient) {
    suspend fun json(url: HttpUrl): JSONObject = withContext(Dispatchers.IO) {
        execute(Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", USER_AGENT).build()).use { response ->
            requireSuccess(response)
            val body = response.body ?: throw SourceException("The music service returned an empty response.")
            if (body.contentLength() > MAX_JSON_BYTES) throw SourceException("The music service returned too much data.")
            val source = body.source()
            source.request(MAX_JSON_BYTES + 1)
            if (source.buffer.size > MAX_JSON_BYTES) throw SourceException("The music service returned too much data.")
            try { JSONObject(source.readUtf8()) } catch (error: org.json.JSONException) {
                throw SourceException("The music service returned an unreadable response. Try again shortly.", error)
            }
        }
    }
    suspend fun text(url: HttpUrl, body: JSONObject? = null): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", USER_AGENT)
        if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
        execute(request.build()).use { response ->
            if (response.code == 404) return@withContext null
            requireSuccess(response)
            val source = response.body?.source() ?: throw IOException("Empty response")
            source.request(MAX_JSON_BYTES + 1)
            if (source.buffer.size > MAX_JSON_BYTES) throw IOException("Response too large")
            source.readUtf8()
        }
    }
    suspend fun jsonOrNull(url: HttpUrl): JSONObject? = text(url)?.let(::JSONObject)
    suspend fun imageBytes(url: HttpUrl): ByteArray = withContext(Dispatchers.IO) {
        execute(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).use { response ->
            requireSuccess(response)
            val body = response.body ?: throw IOException("Empty image")
            val source = body.source(); source.request(MAX_IMAGE_BYTES + 1)
            if (source.buffer.size > MAX_IMAGE_BYTES) throw IOException("Artwork is too large")
            source.readByteArray()
        }
    }
    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(SourceException("Couldn't reach the music service. Check your connection and retry.", e))
            }
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ -> value.close() }
            }
        })
    }
    private fun requireSuccess(response: Response) {
        if (response.isSuccessful) return
        val message = when (response.code) {
            401, 403 -> "This track requires access that wonderPlay does not support."
            404, 410 -> "This music is no longer available from the source."
            429 -> "The music service is busy. Please retry in a moment."
            else -> "The music service is temporarily unavailable. Please retry."
        }
        throw SourceException(message)
    }
    companion object {
        private const val MAX_JSON_BYTES = 8L * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 5L * 1024 * 1024
        const val USER_AGENT = "wonderPlay/1.0 (https://github.com/johanjosesaju3608/wonderPlay)"
        private val sharedClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
    }
}
