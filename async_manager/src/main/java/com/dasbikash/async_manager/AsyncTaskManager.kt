package com.dasbikash.async_manager

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * ```
 * Utility class to run back-ground tasks sequentially in allocated thread pool.
 *
 * ```
 * ##Usage
 *
 * ###### Initializer (optional)
 * ```
 * AsyncTaskManager.init(maxParallelTasks:Int)
 * ```
 *
 * ###### Add back-ground task on pending task queue:
 * ```
 *  AsyncTaskManager.addTask(task: AsyncTask<T>)
 *
 *  //or
 *
 *  AsyncTaskManager.addTask(task:()->T?) //Without completion callbacks
 * ```
 *
 * ###### Remove task:
 * ```
 * AsyncTaskManager.removeTask(task: AsyncTask<T>)
 * ```
 *
 *#### Cancellation of entire task queue
 *```
 * AsyncTaskManager.clear()
 * ```
 *
 * @author Bikash das(das.bikash.dev@gmail.com)
 * */
class AsyncTaskManager private constructor(maxRunningTasks:Int){

    private val taskQueue:Queue<AsyncTask<*>> = LinkedList()
    private val parallelTaskCount = AtomicInteger(0)
    private val maxParallelTasks:Int

    private val dispatcher: ExecutorCoroutineDispatcher
    private var cancelled = OnceSettableBoolean()

    init {
        getNumberOfCores().apply {
            maxParallelTasks =
                if (maxRunningTasks > this) {
                    this
                }else{
                    maxRunningTasks
                }.let {
                    if (it == 0){
                        1
                    }else{
                        it
                    }
                }
        }
        dispatcher = Executors.newFixedThreadPool(maxParallelTasks).asCoroutineDispatcher()
    }

    private fun getNumberOfCores():Int = Runtime.getRuntime().availableProcessors()

    private fun launchTaskIfAny(){
        if (parallelTaskCount.get()< maxParallelTasks && !cancelled.get()){
            taskQueue.poll()?.let {
                parallelTaskCount.getAndIncrement()
                launchTask(it)
            }
        }
    }

    private fun <T> launchTask(task: AsyncTask<T>){
        GlobalScope.launch {
            try {
                runSuspended(dispatcher) {
                    task.runTask()
                }.let {
                    runIfActive {
                        task.onSuccess(it)
                    }
                }
            }catch (ex:Throwable){
                runIfActive {
                    task.onFailure(ex)
                }
            }
            parallelTaskCount.getAndDecrement()
            launchTaskIfAny()
        }
    }

    private fun runIfActive(work:()->Unit){
        if (!cancelled.get()){
            work()
        }
    }

    private fun <T> queueTask(task: AsyncTask<T>){
        GlobalScope.launch {
            while (!taskQueue.offer(task)){
                delay(50L)
            }
            launchTaskIfAny()
        }
    }

    private fun <T> clearTask(task: AsyncTask<T>): Boolean = taskQueue.remove(task)

    private fun clearResources() {
        cancelled.set()
        taskQueue.asSequence().forEach {
            it.cancel()
        }
        taskQueue.clear()
    }

    companion object{
        private const val NOT_INITIALIZED_MESSAGE = "Async Task Manager is not initialized."
        private val DEFAULT_MAX_PARALLEL_RUNNING_TASKS = 2
        private var instance:AsyncTaskManager?=null

        /**
         * Initializer(mandatory) method.
         *
         * Initialization will be done clearing the tasks of previous instance.
         *
         * @param maxParallelTasks:Int (Maximum parallel running task count. Will get truncated to device core count if maxParallelTasks > core count)
         *
         * */
        @JvmStatic
        fun init(maxParallelTasks: Int= DEFAULT_MAX_PARALLEL_RUNNING_TASKS){
            instance?.clearResources()
            instance = AsyncTaskManager(maxParallelTasks)
        }

        /**
         *
         * Clears current task queue.
         *
         * */
        @JvmStatic
        fun clear(){
            instance?.clearResources()
            instance = null
        }

        /**
         *
         * Method to add AsyncTask on pending task queue
         *
         * @param task AsyncTask instance
         * @return Enqueued AsyncTask
         * */
        @JvmStatic
        fun <T> addTask(task:()->T?,
                        doOnSuccess:((T?)->Unit)? = null,
                        doOnFailure:((Throwable?)->Unit)?=null,
                        lifecycleOwner: LifecycleOwner?=null):AsyncTaskHandler<T>{
            if (instance == null){
                init()
            }
            return AsyncTask(task,doOnSuccess, doOnFailure, lifecycleOwner).let {
                instance!!.queueTask(it)
                AsyncTaskHandler(it)
            }
        }

        /**
         *
         * Method to remove AsyncTask from pending task queue
         *
         * @param task AsyncTask instance
         * @throws IllegalStateException if 'AsyncTaskManager' not initialized
         * @return true if task removed else false
         * */
        @JvmStatic
        internal fun <T> removeTask(task: AsyncTask<T>):Boolean{
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            return instance!!.clearTask(task)
        }
    }
}

