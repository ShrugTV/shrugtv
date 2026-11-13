package com.shrug.shrugtv.net

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shrug.shrugtv.login.TwitchDeviceAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

object Http {

    private val CLIENT = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .authenticator { _, response ->
            val req = response.request
            val auth = req.header("Authorization")
            if (auth == null || req.header("X-Twitch-Retry") != null || !"api.twitch.tv".equals(req.url.host, ignoreCase = true)) {
                return@authenticator null
            }
            val fresh = TwitchDeviceAuth.refreshOn401(auth)
            if (fresh.isNullOrEmpty()) {
                return@authenticator null
            }
            req.newBuilder()
                .header("Authorization", "Bearer $fresh")
                .header("X-Twitch-Retry", "1")
                .build()
        }
        .build()

    private val MAIN = Handler(Looper.getMainLooper())

    @JvmStatic
    fun client(): OkHttpClient = CLIENT

    @JvmStatic
    fun load(url: String): HttpRequest = HttpRequest("GET", url)

    @JvmStatic
    fun load(method: String, url: String): HttpRequest = HttpRequest(method, url)

    class HttpRequest internal constructor(
        private val method: String,
        url: String
    ) {
        private val request = Request.Builder().url(url)
        private var payload: String? = null
        private var payloadType = "text/plain; charset=utf-8"
        private var timeoutMs = 0

        fun setHeader(name: String, value: String): HttpRequest {
            if (name.equals("Content-Type", ignoreCase = true)) {
                payloadType = value
            } else {
                request.header(name, value)
            }
            return this
        }

        fun addHeader(name: String, value: String): HttpRequest {
            request.addHeader(name, value)
            return this
        }

        fun setStringBody(text: String?): HttpRequest {
            payload = text
            return this
        }

        fun setTimeout(ms: Int): HttpRequest {
            timeoutMs = ms
            return this
        }

        /** JSON results fail with [StatusException] on a non-2xx status (docs/bug_review.md B17). */
        fun asJsonObject(): Result<JsonObject> = Result(this, checkStatus = true) { text -> parse(text).asJsonObject }

        fun asJsonArray(): Result<JsonArray> = Result(this, checkStatus = true) { text -> parse(text).asJsonArray }

        /**
         * The raw body whatever the status: the VOD playlist answers 403 with a JSON reason that
         * PlaybackUrlResolver reads (restricted VODs).
         */
        fun asString(): Result<String> = Result(this, checkStatus = false) { text -> text }

        internal fun call(): Call {
            var body: RequestBody? = null
            if (payload != null) {
                body = payload!!.toRequestBody(payloadType.toMediaTypeOrNull())
            } else if (method == "POST" || method == "PUT" || method == "PATCH") {
                body = ByteArray(0).toRequestBody(null)
            }
            request.method(method, body)
            var c = CLIENT
            if (timeoutMs > 0) {
                c = CLIENT.newBuilder()
                    .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                    .build()
            }
            return c.newCall(request.build())
        }

        private fun parse(text: String?): JsonElement {
            if (text.isNullOrEmpty()) {
                throw IOException("empty response")
            }
            return try {
                JsonParser().parse(text)
            } catch (e: RuntimeException) {
                throw IOException("not JSON: ${e.message}", e)
            }
        }
    }

    fun interface Converter<T> {
        @Throws(Exception::class)
        fun convert(text: String): T
    }

    /**
     * Non-2xx answer of a JSON request. Before, error bodies like Helix'
     * `{"error":"Forbidden","status":403}` were handed to callers as a normal result.
     */
    class StatusException(val code: Int, val body: String) : IOException("HTTP $code") {
        companion object {
            /** The StatusException in [e] or its cause chain (get() wraps it in ExecutionException). */
            @JvmStatic
            fun of(e: Throwable?): StatusException? {
                var t = e
                while (t != null) {
                    if (t is StatusException) return t
                    t = t.cause
                }
                return null
            }
        }
    }

    class Result<T> internal constructor(
        private val request: HttpRequest,
        private val checkStatus: Boolean,
        private val converter: Converter<T>
    ) {
        @Throws(ExecutionException::class)
        fun get(): T {
            return try {
                request.call().execute().use { response ->
                    val body = text(response)
                    if (checkStatus && !response.isSuccessful) throw StatusException(response.code, body)
                    converter.convert(body)
                }
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        }

        fun setCallback(callback0: ResultCallback<T>?) {
            val callback = callback0 ?: ResultCallback { _, _ -> }
            request.call().enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    MAIN.post { callback.onCompleted(e, null) }
                }

                override fun onResponse(call: Call, response: Response) {
                    var value: T? = null
                    var error: Exception? = null
                    try {
                        response.use { resp ->
                            val body = text(resp)
                            if (checkStatus && !resp.isSuccessful) throw StatusException(resp.code, body)
                            value = converter.convert(body)
                        }
                    } catch (e: Exception) {
                        value = null
                        error = e
                    }
                    val finalVal = value
                    val finalErr = error
                    MAIN.post { callback.onCompleted(finalErr, finalVal) }
                }
            })
        }

        private fun text(response: Response): String {
            return response.body?.string() ?: ""
        }
    }
}
