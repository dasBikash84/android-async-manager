package com.dasbikash.async_manager

import java.util.concurrent.atomic.AtomicBoolean

internal class OnceSettableBoolean {
    private var status = AtomicBoolean(false)
    fun get():Boolean{
        return status.get()
    }
    fun set(){
        status.getAndSet(true)
    }
}