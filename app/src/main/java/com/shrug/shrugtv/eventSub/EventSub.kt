package com.shrug.shrugtv.eventSub

import android.content.Context
import com.google.gson.Gson
import com.shrug.shrugtv.api.helix.HelixApi
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class EventSub {
    companion object {
        private val IO = Executors.newCachedThreadPool()

        @JvmStatic
        fun DeleteDisconnectedEvents(context: WeakReference<Context>) {
            IO.execute {
                val ctx = context.get() ?: return@execute
                val subscriptions = HelixApi.getEventSubSubscriptions(ctx) ?: return@execute
                val activeEvents = Gson().fromJson(subscriptions, EventSubActiveEvents::class.java)
                val data = activeEvents?.data ?: return@execute
                val disconnected = data.filter { it.status != "enabled" && !it.id.isNullOrEmpty() }
                for (dto in disconnected) {
                    dto.id?.let { HelixApi.deleteEventSubSubscription(ctx, it) }
                }
            }
        }
    }
}
