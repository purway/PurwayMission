package com.kaisavx.AircraftController.model

import dji.common.flightcontroller.LocationCoordinate3D
import io.reactivex.Observable

/**
 * Created by Abner on 2017/6/3.
 */
interface Flight {
    enum class Type {
        WATER, AIR
    }

    var flightId: String
    val isFlying: Observable<Boolean>

    val connected: Observable<Boolean>
    val location: Observable<LocationCoordinate3D>
    val direction: Observable<Int>

    val name: String
    val type: Type

    //val worker: OnBoardApi

    fun onConnected()
    fun onDisconnected()

    fun destroy()
}