package com.dasbikash.async_manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * ```
 * Wrapper class on back-ground task with optional "doOnSuccess" and "doOnFailure" callbacks.
 * With optional(but recommended) "LifecycleOwner" it can be ensured that "task"/"doOnSuccess"/"doOnFailure"
 * doesn't execute if caller LifecycleOwner is destroyed.
 *
 * Registered doOnSuccess/doOnFailure will run on main thread.
 * ```
 *
 * @param task Functional param for back-ground task.
 * @param doOnSuccess optional callback to be called on task success.
 * @param doOnFailure optional callback to be called on task failure.
 * @param lifecycleOwner optional(but recommended) Lifecycle hook for task cancellation.
 * */

internal class AsyncTask<T>(
    private val task:()->T?,
    private val doOnSuccess:((T?)->Unit)? = null,
    private val doOnFailure:((Throwable?)->Unit)?=null,
    lifecycleOwner: LifecycleOwner?=null
): DefaultLifecycleObserver {

    private var cancelled = OnceSettableBoolean()

    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancelled.set()
    }

    internal fun runTask():T?{
        if (!cancelled.get()){
            return task()
        }
        throw IllegalStateException()
    }

    internal fun onSuccess(result:T?){
        if (!cancelled.get()){
            runOnMainThread {
                doOnSuccess?.invoke(result)
            }
        }
    }

    internal fun onFailure(throwable:Throwable?){
        if (!cancelled.get()){
            runOnMainThread {
                doOnFailure?.invoke(throwable)
            }
        }
    }

    /**
     * Cancels current task.
     *
     * */
    fun cancel(){
        cancelled.set()
    }
}