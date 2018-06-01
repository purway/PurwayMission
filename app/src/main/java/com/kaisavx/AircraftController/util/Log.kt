package com.kaisavx.AircraftController.util

import android.util.Log

/**
 * Created by windless on 2017/11/27.
 */
class Log {
    companion object {
        fun d(o: Any, message: String?, e: Throwable? = null) {
            Log.d(o.javaClass.name, message, e)
        }

        fun w(o: Any, message: String?, e: Throwable? = null) {
            Log.w(o.javaClass.name, message, e)
        }

        fun i(o: Any, message: String?, e: Throwable? = null) {
            Log.i(o.javaClass.name, message, e)
        }

        fun e(o: Any, message: String?, e: Throwable? = null) {
            Log.e(o.javaClass.name, message, e)
        }

        fun m(o:Any ){
            var element = Exception().stackTrace
            if(element!!.size>0){
                Log.d(o.javaClass.name , element[1].methodName)
            }else{
                Log.d(o.javaClass.name , "")
            }
        }
    }

}