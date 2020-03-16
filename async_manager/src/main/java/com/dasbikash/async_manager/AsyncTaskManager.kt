package com.dasbikash.async_manager

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * ```
 * Utility class to run back-ground tasks sequentially in allocated thread pool.
 *
 * ```
 * ##Usage
 * ###### Initialization(Mandatory):
 * ```
 * AsyncTaskManager.init(maxParallelTasks:Int)
 * ```
 * ###### Add back-ground task on pending task queue:
 * ```
 *  AsyncTaskManager.addTask(task: AsyncTask<T>)
 *
 *  //or
 *
 *  AsyncTaskManager.addTask(task:()->T?) //Without caller lifecycle-owner hook
 * ```
 * ###### Remove task(Mandatory):
 * ```
 * AsyncTaskManager.removeTask(task: AsyncTask<T>)
 * ```
 *
 * @author Bikash das(das.bikash.dev@gmail.com)
 * */
class AsyncTaskManager private constructor(private val maxParallelTasks:Int)
    :CoroutineScope{

    private val taskQueue:Queue<AsyncTask<*>> = LinkedList()
    private val parallelTaskCount = AtomicInteger(0)

    override val coroutineContext: CoroutineContext
        get() = Job()

    private fun launchTaskIfAny(){
        if (parallelTaskCount.get()< maxParallelTasks && isActive){
            taskQueue.poll()?.let {
                parallelTaskCount.getAndIncrement()
                launchTask(it)
            }
        }
    }

    private fun <T> launchTask(task: AsyncTask<T>){
        launch(Dispatchers.IO) {
            try {
                task.runTask().let {
                    if (isActive) {
                        task.onSuccess(it)
                    }
                }
            }catch (ex:Throwable){
                if (isActive) {
                    task.onFailure(ex)
                }
            }
            parallelTaskCount.getAndDecrement()
            launchTaskIfAny()
        }
    }

    private fun <T> queueTask(task: AsyncTask<T>){
        launch(Dispatchers.IO) {
            while (!taskQueue.offer(task) && isActive){
                delay(50L)
            }
            launchTaskIfAny()
        }
    }

    private fun <T> clearTask(task: AsyncTask<T>): Boolean = taskQueue.remove(task)

    private fun clearResources() {
        cancel()
        taskQueue.clear()
    }

    companion object{
        private const val NOT_INITIALIZED_MESSAGE = "Async Task Manager is not initialized."
        private val DEFAULT_MAX_PARALLEL_RUNNING_TASKS = 2
        private var instance:AsyncTaskManager?=null

        /**
         * Method to initialize(Mandatory) using AppCompatActivity instance.
         * Will clear init clearing the tasks of previous instance.
         *
         * @param maxParallelTasks Maximum parallel running task count. Default DEFAULT_MAX_PARALLEL_RUNNING_TASKS
         * */
        @JvmStatic
        fun init(maxParallelTasks: Int= DEFAULT_MAX_PARALLEL_RUNNING_TASKS){
            instance?.clearResources()
            instance = AsyncTaskManager(maxParallelTasks)
        }

        /**
         * Method to add AsyncTask on pending task queue
         *
         * @param task AsyncTask instance
         * @throws IllegalStateException if 'AsyncTaskManager' not initialized
         * */
        @JvmStatic
        fun <T> addTask(task: AsyncTask<T>){
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            instance!!.queueTask(task)
        }

        /**
         * Method to remove AsyncTask from pending task queue
         *
         * @param task AsyncTask instance
         * @throws IllegalStateException if 'AsyncTaskManager' not initialized
         * @return true if task removed else false
         * */
        @JvmStatic
        fun <T> removeTask(task: AsyncTask<T>):Boolean{
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            return instance!!.clearTask(task)
        }

        /**
         * Method to directly(without lifecycleOwner and doOnSuccess/doOnFailure) add Task on pending task queue
         *
         * @param task Functional parameter
         * @throws IllegalStateException if 'AsyncTaskManager' not initialized
         * */
        @JvmStatic
        fun <T> addTask(task:()->T){
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            instance!!.queueTask(AsyncTask(task=task,lifecycleOwner = null))
        }
    }
}