package com.shrug.shrugtv.net

import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.api.gql.GqlApi
import com.shrug.shrugtv.session.AppState
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coroutine extension and central Twitch headers for the shared [Http.client]
 * (Stage 0.1).
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) cont.resume(response) else response.close()
        }
    })
}

/** Builds a Helix GET request with client ID and the current bearer token. */
fun helixGetRequest(url: String, token: String? = AppState.accessToken): Request =
    Request.Builder()
        .url(url)
        .header("Client-Id", BuildConfig.TWITCH_CLIENT_ID)
        .apply { if (!token.isNullOrEmpty()) header("Authorization", "Bearer $token") }
        .build()

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

/** Builds a GQL POST request with web client ID and optional OAuth token. */
fun gqlPostRequest(bodyJson: String, oauthToken: String? = null): Request =
    Request.Builder()
        .url("https://gql.twitch.tv/gql")
        .header("Client-ID", GqlApi.WEB_CLIENT_ID)
        .apply { if (!oauthToken.isNullOrEmpty()) header("Authorization", "OAuth $oauthToken") }
        .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
        .build()
