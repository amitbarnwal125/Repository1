package com.swara.music.data.remote

import com.swara.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Http {
    /** Wikimedia's API etiquette asks every client to send a descriptive User-Agent with contact info. */
    val USER_AGENT = "SwaraMusic/${BuildConfig.VERSION_NAME} (Android; https://github.com/amitbarnwal125/Repository1) okhttp"

    fun client(cacheDir: java.io.File): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .cache(okhttp3.Cache(java.io.File(cacheDir, "http"), 10L * 1024 * 1024))
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()
}

class ApiException(message: String) : IOException(message)

suspend fun OkHttpClient.getJson(url: HttpUrl): JSONObject = withContext(Dispatchers.IO) {
    val call = newCall(Request.Builder().url(url).get().build())
    val body = suspendCancellableCoroutine<String> { cont ->
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        cont.resumeWithException(ApiException("Server returned ${it.code}"))
                    } else {
                        cont.resume(it.body?.string().orEmpty())
                    }
                }
            }
        })
    }
    JSONObject(body)
}
