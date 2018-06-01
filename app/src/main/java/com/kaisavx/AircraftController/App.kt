package com.kaisavx.AircraftController

import android.content.Context
import android.os.Environment
import android.support.multidex.MultiDexApplication
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import com.kaisavx.AircraftController.util.Log
import com.kaisavx.AircraftController.util.RealmKit
import com.kaisavx.AircraftController.util.log
import com.secneo.sdk.Helper
import java.io.File
import kotlin.properties.Delegates

class App : MultiDexApplication() {

    companion object {
        var context: Context by Delegates.notNull()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Log.m(this)
        Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.m(this)
        val fileDir = File(Environment.getExternalStorageDirectory(), "/AircraftController/xlog").absolutePath
        val filePrinter = FilePrinter.Builder(fileDir)
                .fileNameGenerator(DateFileNameGenerator())
                .build()


        RealmKit.init(this)
        XLog.init(if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE, filePrinter)
        context = this

        log(this,"----------------------- App Start ----------------------------")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.m(this)
        RealmKit.deinit()
    }

}