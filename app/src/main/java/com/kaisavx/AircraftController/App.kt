package com.kaisavx.AircraftController

import android.content.Context
import android.os.Environment
import android.support.multidex.MultiDexApplication
import android.text.TextUtils
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.Log
import com.kaisavx.AircraftController.util.RealmKit
import com.secneo.sdk.Helper
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import io.reactivex.plugins.RxJavaPlugins
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import kotlin.properties.Delegates




class App : MultiDexApplication() {

    companion object {
        var context: Context by Delegates.notNull()
        private val TAG = "App"
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Log.m(this)
        Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        val packageName = applicationContext.packageName
        val processName = getProcessName(android.os.Process.myPid())
        val strategy = UserStrategy(applicationContext)
        strategy.isUploadProcess = processName == null || processName == packageName

        CrashReport.initCrashReport(applicationContext , strategy);

        Log.m(this)
        val fileDir = File(Environment.getExternalStorageDirectory(), "/AircraftController/xlog").absolutePath
        val filePrinter = FilePrinter.Builder(fileDir)
                .fileNameGenerator(DateFileNameGenerator())
                .build()


        RealmKit.init(this)
        XLog.init(if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE, filePrinter)
        context = this

        RxJavaPlugins.setErrorHandler {
            KLog.e(TAG,"RxJavaPlugins error:${it.localizedMessage}")
        }

        KLog.i(TAG ,"App context:$context")
        KLog.i(TAG,"----------------------- App Start ----------------------------")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.m(this)
        RealmKit.deinit()
    }

    private fun getProcessName(pid: Int): String? {
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader("/proc/$pid/cmdline"))
            var processName = reader!!.readLine()
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim({ it <= ' ' })
            }
            return processName
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        } finally {
            try {
                if (reader != null) {
                    reader!!.close()
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
            }

        }
        return null
    }

}