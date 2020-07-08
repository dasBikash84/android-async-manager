package com.dasbikash.async_manager

/**
 * Handler Class to operate associated queued Back-ground Task
 *
 * */
class AsyncTaskHandler<T> internal constructor(private val asyncTask: AsyncTask<T>){

    /**
     * Method to cancel associated AsyncTask
     *
     * */
    fun cancelTask() = asyncTask.cancel()
}