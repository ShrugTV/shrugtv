package com.shrug.shrugtv.net

fun interface ResultCallback<T> {
    fun onCompleted(error: Exception?, result: T?)
}
