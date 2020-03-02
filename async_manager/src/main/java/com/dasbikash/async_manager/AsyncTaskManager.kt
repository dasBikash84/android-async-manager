package com.dasbikash.async_manager

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AsyncTaskManager private constructor(private val maxParallelTasks:Int, lifecycleOwner: LifecycleOwner)
    :DefaultLifecycleObserver{

    private val taskQueue:Queue<AsyncTask<*>> = LinkedList()
    private val parallelTaskCount = AtomicInteger(0)
    private val jobs = mutableListOf<Job>()

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private fun launchTaskIfAny(){
        if (parallelTaskCount.get()< maxParallelTasks){
            taskQueue.poll()?.let {
                parallelTaskCount.getAndIncrement()
                launchTask(it)
            }
        }
    }

    private fun <T:Any> launchTask(task: AsyncTask<T>){
        var job:Job?=null
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                task.runTask().let {
                    task.onSuccess(it)
                }
            }catch (ex:Throwable){
                task.onFailure(ex)
            }
            parallelTaskCount.getAndDecrement()
            jobs.remove(job)
            launchTaskIfAny()
        }
        jobs.add(job)
    }

    private fun <T:Any> queueTask(task: AsyncTask<T>){
        GlobalScope.launch(Dispatchers.IO) {
            while (!taskQueue.offer(task)){
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
        jobs.asSequence().forEach { it.cancel() }
        jobs.clear()
        taskQueue.clear()
        clearInstance()
    }

    companion object{
        private const val NOT_INITIALIZED_MESSAGE = "AsyncManager is not initialized."
        private val DEFAULT_MAX_PARALLEL_RUNNING_TASKS = 2
        private var instance:AsyncTaskManager?=null

        private fun clearInstance(){
            instance = null
        }

        @JvmStatic
        fun init(fragment: Fragment,maxParallelTasks: Int= DEFAULT_MAX_PARALLEL_RUNNING_TASKS):Boolean{
            fragment.activity?.let {
                if (it is AppCompatActivity){
                    return init(it,maxParallelTasks)
                }
            }
            return false
        }
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

        @JvmStatic
        fun <T:Any> addTask(task: AsyncTask<T>){
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            instance!!.queueTask(task)
        }

        @JvmStatic
        fun <T:Any> removeTask(task: AsyncTask<T>):Boolean{
            if (instance == null){
                throw IllegalStateException(NOT_INITIALIZED_MESSAGE)
            }
            return instance!!.clearTask(task)
        }
    }
}

class AsyncTask<T:Any>(
    private val task:()->T?,
    private val doOnSuccess:((T?)->Any?)? = null,
    private val doOnFailure:((Throwable?)->Any?)?=null
):DefaultLifecycleObserver{
    constructor(
                task:()->T?,
                lifecycleOwner: LifecycleOwner?=null,
                doOnSuccess:((T?)->Any?)? = null,
                doOnFailure:((Throwable?)->Any?)?=null):this(task, doOnSuccess, doOnFailure){
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    private var isActive=true

    override fun onDestroy(owner: LifecycleOwner) {
        isActive = false
    }

    internal fun runTask():T?{
        if (isActive){
            return task()
        }
        return null
    }

    internal fun onSuccess(result:T?){
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