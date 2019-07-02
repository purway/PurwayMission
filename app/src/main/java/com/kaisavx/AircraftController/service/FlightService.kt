package com.kaisavx.AircraftController.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kaisavx.AircraftController.module.dji.DJIManager
import com.kaisavx.AircraftController.util.KLog

class FlightService : Service(){
    companion object {
        private val TAG = "FlightService"
    }

    class LocalBinder(val service: FlightService) : Binder()

    val djiManager = DJIManager()

    var binder: LocalBinder? = null

    override fun onBind(intent: Intent): IBinder?{
        return binder
    }

    override fun onCreate() {
        KLog.i(TAG,"onCreate")
        super.onCreate()
        binder = LocalBinder(this)
    }

    override fun onDestroy() {
        KLog.i(TAG,"onDestroy")
        super.onDestroy()
        djiManager.destory()
        binder = null
    }


}