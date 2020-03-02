package com.dasbikash.async_manager

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.*

class AsyncManager private constructor(lifecycleOwner: LifecycleOwner)
    :DefaultLifecycleObserver{

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        clearResources()
    }

    private fun clearResources() {
        clearInstance()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object{
        private var instance:AsyncManager?=null

        private fun clearInstance(){
            instance = null
        }

        @JvmStatic
        fun init(fragment: Fragment):Boolean{
            fragment.activity?.let {
                if (it is AppCompatActivity){
                    return init(it)
                }
            }
            return false
        }
        @JvmStatic
        fun init(activity: AppCompatActivity):Boolean{
            if (instance == null){
                synchronized(AsyncManager::class.java) {
                    if (instance==null) {
                        instance = AsyncManager(activity)
                    }
                }
            }
            return instance!=null
        }
    }
}

class AsyncTask<T:Any>(
    val task:()->T?,
    val doOnSuccess:((T?)->Any?)? = null,
    val doOnFailure:((Throwable?)->Any?)?=null
):DefaultLifecycleObserver{
    internal val id:String = UUID.randomUUID().toString()
    internal var isDestroyed=false
    override fun onDestroy(owner: LifecycleOwner) {
        isDestroyed = true
    }
    constructor(task:()->T?,
                lifecycleOwner: LifecycleOwner?=null,
                doOnSuccess:((T?)->Any?)? = null,
                doOnFailure:((Throwable?)->Any?)?=null):this(task, doOnSuccess, doOnFailure){
        lifecycleOwner?.lifecycle?.addObserver(this)
    }
}