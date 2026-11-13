package com.shrug.shrugtv.events

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * App-internal events, replacement for greenrobot EventBus.
 */
object Events {

    interface Receiver {
        fun onAppEvent(event: AppEvent)
        fun liveHistoryRemoved(event: LiveHistoryChannelRemovedEvent)
    }

    private val RECEIVERS = CopyOnWriteArrayList<Receiver>()
    private val MAIN = Handler(Looper.getMainLooper())

    @JvmStatic
    fun register(e: Receiver) {
        RECEIVERS.addIfAbsent(e)
    }

    @JvmStatic
    fun unregister(e: Receiver) {
        RECEIVERS.remove(e)
    }

    @JvmStatic
    fun send(event: AppEvent) {
        onMainThread {
            for (e in RECEIVERS) {
                e.onAppEvent(event)
            }
        }
    }

    @JvmStatic
    fun send(event: LiveHistoryChannelRemovedEvent) {
        onMainThread {
            for (e in RECEIVERS) {
                e.liveHistoryRemoved(event)
            }
        }
    }

    private inline fun onMainThread(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            MAIN.post { block() }
        }
    }
}
