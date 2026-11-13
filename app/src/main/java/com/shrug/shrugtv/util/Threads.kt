package com.shrug.shrugtv.util

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.Executor

object Threads {

    fun interface OnBackgroundJob {
        fun doOnBackground()
    }

    fun interface OnMainThreadJob {
        fun doInUIThread()
    }

    private val IO: Executor = Dispatchers.IO.asExecutor()
    private val MAIN = Handler(Looper.getMainLooper())

    @JvmStatic
    fun doInBackground(job: OnBackgroundJob) {
        IO.execute { job.doOnBackground() }
    }

    /** Always through post, also from the main thread (like AsyncJob): the order stays the same. */
    @JvmStatic
    fun doOnMainThread(job: OnMainThreadJob) {
        MAIN.post { job.doInUIThread() }
    }
}
