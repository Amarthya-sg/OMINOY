package com.swordfish.libretrodroid

import android.content.Context
import androidx.annotation.Keep
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** HTTP and UI-event bridge for the native rcheevos client. */
@Keep
object RetroAchievementsNativeBridge {
    data class Event(
        val type: Int,
        val title: String,
        val detail: String,
        val points: Int,
        val badgeUrl: String,
        val gameHash: String = "",
        val achievementId: Long = 0L,
        val unlockTimeMillis: Long = 0L,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val mutableEvents = MutableSharedFlow<Event>(extraBufferCapacity = 32)
    private var spikeStore: RetroAchievementsOfflineSpikeStore? = null
    val events = mutableEvents.asSharedFlow()

    /** Enables the feasibility-spike persistence only for an explicit test build/session. */
    @JvmStatic
    @Keep
    fun initializeOfflineSpike(context: Context, enabled: Boolean) {
        spikeStore = if (enabled) RetroAchievementsOfflineSpikeStore(context.applicationContext) else null
    }

    @JvmStatic
    @Keep
    fun request(requestId: Long, url: String, postData: String?, contentType: String?) {
        val method = if (postData == null) "GET" else "POST"
        val key = url + "\n" + (postData ?: "")
        spikeStore?.recordRequest(method, url, postData)
        val builder = Request.Builder().url(url)
        if (postData != null) {
            val mediaType = (contentType ?: "application/x-www-form-urlencoded").toMediaTypeOrNull()
            builder.post(postData.toRequestBody(mediaType))
        }
        builder.header("User-Agent", "OMINOY/1.18.0 (rcheevos)")
        val call = client.newCall(builder.build())
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, error: IOException) {
                val cached = spikeStore?.cachedResponse(key)
                if (cached != null) {
                    spikeStore?.recordRequest(method, url, postData, cached.body.length)
                    LibretroDroid.completeRaServerRequest(requestId, cached.statusCode, cached.body)
                } else {
                    // rcheevos treats -1 as a non-retryable client error. -2 keeps
                    // achievement awards pending and lets rc_client retry later.
                    LibretroDroid.completeRaServerRequest(requestId, -2, "")
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    spikeStore?.recordResponse(key, it.code, body)
                    spikeStore?.recordRequest(method, url, postData, body.length)
                    LibretroDroid.completeRaServerRequest(requestId, it.code, body)
                }
            }
        })
    }

    @JvmStatic
    @Keep
    fun onNativeEvent(
        type: Int,
        title: String?,
        detail: String?,
        points: Int,
        badgeUrl: String?,
        gameHash: String?,
        achievementId: Long,
        unlockTimeMillis: Long,
    ) {
        if (type == 1 && achievementId != 0L) {
            spikeStore?.recordUnlock(achievementId, gameHash.orEmpty(), unlockTimeMillis)
        }
        mutableEvents.tryEmit(
            Event(type, title.orEmpty(), detail.orEmpty(), points, badgeUrl.orEmpty(), gameHash.orEmpty(), achievementId, unlockTimeMillis),
        )
    }
}
