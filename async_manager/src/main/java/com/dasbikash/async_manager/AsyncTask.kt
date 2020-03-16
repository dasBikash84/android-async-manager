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
 * @param lifecycleOwner optional(but recommended) caller LifecycleOwner.
 * @param task Functial param for back-ground task.
 * @param doOnSuccess optional callback to be called on task success.
 * @param doOnFailure optional callback to be called on task failure.
 * */
class AsyncTask<T>(
    lifecycleOwner: LifecycleOwner?,
    private val task:()->T,
    private val doOnSuccess:((T)->Any?)? = null,
    private val doOnFailure:((Throwable?)->Unit)?=null
): DefaultLifecycleObserver {

    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    private var isActive=true

    override fun onDestroy(owner: LifecycleOwner) {
        isActive = false
    }

    internal fun runTask():T{
        if (isActive){
            return task()
        }
        throw IllegalStateException()
    }

    internal fun onSuccess(result:T){
        if (isActive){
            runOnMainThread {
                doOnSuccess?.invoke(result)
            }
        }
    }

    internal fun onFailure(throwable:Throwable?){
        if (isActive){
            runOnMainThread {
                doOnFailure?.invoke(throwable)
            }
        }
    }
}