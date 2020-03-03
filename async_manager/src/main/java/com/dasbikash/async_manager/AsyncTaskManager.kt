package com.dasbikash.async_manager

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * ```
 * Utility class to run back-ground tasks sequentially in allocated thread pool.
 *
 * User have to initialize Async Task Manager singleton instance with a
 * lifecycle-owner(AppCompatActivity/Fragment) instance.
 *
 * Async Task Manager instance will not save lifecycle-owner reference but
 * will observe it's lifecycle and will clear itself(including task Queue)
 * on "lifecycle-owner" destroy. So, user doesn't have worry about resource clearing.
 * ```
 *
 * ##Usage
 * ###### Initialization(Mandatory):
 * ```
 * AsyncTaskManager.init(activity: AppCompatActivity,maxParallelTasks:Int)
 *
 * //or
 *
 * AsyncTaskManager.init(fragment: Fragment,maxParallelTasks:Int)
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
class AsyncTaskManager private constructor(private val maxParallelTasks:Int, lifecycleOwner: LifecycleOwner)
    :DefaultLifecycleObserver,CoroutineScope{

    private val taskQueue:Queue<AsyncTask<*>> = LinkedList()
    private val parallelTaskCount = AtomicInteger(0)

    override val coroutineContext: CoroutineContext
        get() = Job()

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private fun launchTaskIfAny(){
        if (parallelTaskCount.get()< maxParallelTasks && isActive){
            taskQueue.poll()?.let {
                parallelTaskCount.getAndIncrement()
                launchTask(it)
            }
        }
    }

    private fun <T:Any> launchTask(task: AsyncTask<T>){
        launch {
            try {
                withTimeout(task.maxRunTime){
                    try {
                        runSuspended {task.runTask()}
                    }catch (ex:TimeoutCancellationException){
                        throw TimeOutException("Task didn't finish within ${task.maxRunTime} ms.")
                    }
                }.let {
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

    private fun <T:Any> queueTask(task: AsyncTask<T>){
        launch(Dispatchers.IO) {
            while (!taskQueue.offer(task) && isActive){
                delay(50L)
            }
            launchTaskIfAny()
        }
    }

    private fun <T:Any> clearTask(task: AsyncTask<T>): Boolean = taskQueue.remove(task)

    override fun onDestroy(owner: LifecycleOwner) {
        clearResources()
    }

    private fun clearResources() {
        cancel()
        taskQueue.clear()
        clearInstance()
    }

    companion object{
        private const val NOT_INITIALIZED_MESSAGE = "Async Task Manager is not initialized."
        private val DEFAULT_MAX_PARALLEL_RUNNING_TASKS = 2
        private var instance:AsyncTaskManager?=null

        private fun clearInstance(){
            instance = null
        }

        /**
         * Method to initialize(Mandatory) using Fragment instance
         *
         * @param fragment Fragment instance
         * @param maxParallelTasks Maximum parallel running task count. Default DEFAULT_MAX_PARALLEL_RUNNING_TASKS
         * @return true for success else false
         * */
        @JvmStatic
        fun init(fragment: Fragment,maxParallelTasks: Int= DEFAULT_MAX_PARALLEL_RUNNING_TASKS):Boolean{
            fragment.activity?.let {
                if (it is AppCompatActivity){
                    return init(it,maxParallelTasks)
                }
            }
            return false
        }

        /**
         * Method to initialize(Mandatory) using AppCompatActivity instance
         *
         * @param activity AppCompatActivity instance
         * @param maxParallelTasks Maximum parallel running task count. Default DEFAULT_MAX_PARALLEL_RUNNING_TASKS
         * @return true for success else false
         * */
        @JvmStatic
        fun init(activity: AppCompatActivity,maxParallelTasks: Int= DEFAULT_MAX_PARALLEL_RUNNING_TASKS):Boolean{
            if (instance == null){
                synchronized(AsyncTaskManager::class.java) {
                    if (instance==null) {
                        instance = AsyncTaskManager(maxParallelTasks,activity)
                    }
                }
            }
            return instance!=null
        }

        /**
         * Method to add AsyncTask on pending task queue
         *
         * @param task AsyncTask instance
         * @throws IllegalStateException if 'AsyncTaskManager' not initialized
         * */
        @JvmStatic
        fun <T:Any> addTask(task: AsyncTask<T>){
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
        fun <T:Any> removeTask(task: AsyncTask<T>):Boolean{
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
        fun <T:Any> addTask(task:()->T?){
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            instance!!.queueTask(AsyncTask(task=task,lifecycleOwner = null))
        }
    }
}