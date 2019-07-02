package com.kaisavx.AircraftController.util

import android.util.Log
import com.elvishew.xlog.XLog
import io.reactivex.subjects.ReplaySubject
import java.text.SimpleDateFormat
import java.util.*

object KLog {
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    class LogMessage(val time: Long, val level: LogLevel, val className: String, val message: String, val e: Throwable?) {
        override fun toString(): String {

            val t = SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINESE).format(Date(time))

            var msg = "$t : $className : $message"
            e?.let {
                msg += " error:${it.localizedMessage}"
            }
            return when (level) {
                LogLevel.VERBOSE -> {
                    "<font color='#000000'>V:${msg}</font><br/>"
                }
                LogLevel.DEBUG -> {
                    "<font color='#0000FF'>D:${msg}</font><br/>"
                }
                LogLevel.INFO -> {
                    "<font color='#00FF00'>I:${msg}</font><br/>"
                }
                LogLevel.WARN -> {
                    "<font color='#FF6100'>I:${msg}</font><br/>"
                }
                LogLevel.ERROR -> {
                    "<font color='#FF0000'>E:${msg}</font><br/>"
                }
            }

        }
    }

    val logSubject: ReplaySubject<LogMessage> = ReplaySubject.create()

    fun v(className: String, message: String, e: Throwable? = null) {
        logSubject.onNext(LogMessage(System.currentTimeMillis(), LogLevel.VERBOSE, className, message, e))
        Log.v(className, message, e)
        XLog.v("$className:$message", e)
    }

    fun d(className: String, message: String, e: Throwable? = null) {
        logSubject.onNext(LogMessage(System.currentTimeMillis(), LogLevel.DEBUG, className, message, e))
        Log.v(className, message, e)
        XLog.v("$className:$message", e)
    }

    fun i(className: String, message: String, e: Throwable? = null) {
        logSubject.onNext(LogMessage(System.currentTimeMillis(), LogLevel.INFO, className, message, e))
        Log.i(className, message, e)
        XLog.i("$className:$message", e)
    }

    fun w(className: String, message: String, e: Throwable? = null) {
        logSubject.onNext(LogMessage(System.currentTimeMillis(), LogLevel.WARN, className, message, e))
        Log.w(className, message, e)
        XLog.w("$className:$message", e)
    }

    fun e(className: String, message: String, e: Throwable? = null) {
        logSubject.onNext(LogMessage(System.currentTimeMillis(), LogLevel.ERROR, className, message, e))
        Log.e(className, message, e)
        XLog.e("$className:$message", e)
    }
}