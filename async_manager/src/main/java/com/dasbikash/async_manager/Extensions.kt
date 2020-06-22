package com.dasbikash.async_manager

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Extension function to launch async task
 * suspending any suspension function
 *
 * @param task posted functional parameter
 * @param dispatcher CoroutineDispatcher for running Async task
 * */
suspend fun <T> runSuspended(dispatcher: CoroutineDispatcher=Dispatchers.IO,task:()->T?):T? {
    coroutineContext().let {
        return withContext(it) {
            return@withContext async(dispatcher) { task() }.await()
        }
    }
}

/**
 * Extension function on access CoroutineContext from inside of any suspension function
 *
 * @return subject CoroutineContext
 * */
suspend fun coroutineContext(): CoroutineContext = suspendCoroutine { it.resume(it.context) }

internal fun runOnMainThread(task: () -> Any?) =
    Handler(Looper.getMainLooper()).post( { task() })


/**
 * Extension function on AppCompatActivity to enqueue task.
 * Subject AppCompatActivity is injected as lifecycle hook.
 *
 * @param task Functional parameter
 * @return Enqueued AsyncTask
 * */
fun <T> AppCompatActivity.addAsyncTask(task:()->T?):AsyncTaskHandler<T> =
    AsyncTaskManager.addTask(task=task,lifecycleOwner = this)

/**
 * Extension function on Fragment to enqueue task.
 * Subject Fragment is injected as lifecycle hook.
 *
 * @param task Functional parameter
 * @return Enqueued AsyncTask
 * */
fun <T> Fragment.addAsyncTask(task:()->T?):AsyncTaskHandler<T> =
    AsyncTaskManager.addTask(task=task,lifecycleOwner = this)
