package com.kaisavx.AircraftController.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kaisavx.AircraftController.interfaces.DJIWayPointOperator
import com.kaisavx.AircraftController.model.Flight
import com.kaisavx.AircraftController.util.logMethod
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class FlightService : Service(){

    class LocalBinder(val service: FlightService) : Binder()

    var isFakeDevice = false
    private val registerSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    val isRegister: Observable<Boolean> = registerSubject


    var binder: LocalBinder? = null

    val currentFlight: BehaviorSubject<Flight> = BehaviorSubject.create()
    val error: BehaviorSubject<String> = BehaviorSubject.create()

    override fun onBind(intent: Intent): IBinder?{
        return binder
    }

    private val operator by lazy { DJIWayPointOperator() }

    val missionService by lazy {MissionService(operator)}

    override fun onCreate() {
        logMethod(this)
        super.onCreate()
        binder = LocalBinder(this)

    }

    override fun onDestroy() {
        logMethod(this)
        super.onDestroy()
        binder = null

    }


}