package com.dasbikash.async_manager

class AsyncTaskHandler<T> internal constructor(private val asyncTask: AsyncTask<T>){
    fun cancelTask() = asyncTask.cancel()
    fun removeTask():Boolean = AsyncTaskManager.removeTask(asyncTask)
}