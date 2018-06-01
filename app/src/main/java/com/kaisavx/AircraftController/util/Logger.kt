package com.kaisavx.AircraftController.util

import com.elvishew.xlog.XLog

/**
 * Created by windless on 2017/12/15.
 */
fun log(o: Any, message: String) {
    XLog.d("${o.javaClass.name}: $message")
    Log.d(o, message)
}

fun log(o: Any, message: String, e: Throwable) {
    XLog.d("${o.javaClass.name}: $message", e)
    Log.d(o, message, e)
}

fun logMethod(o:Any){
    var element = Exception().stackTrace
    var msg:String=""

    if(element!!.size>0){
        msg = element[1].methodName
    }
    XLog.d("${o.javaClass.name}: $msg")
    Log.d(o,msg)
}
