package com.kaisavx.AircraftController.interfaces

import com.kaisavx.AircraftController.service.Mission
import com.kaisavx.AircraftController.service.MissionState
import dji.common.util.CommonCallbacks
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Created by windless on 2017/12/14.
 */
interface WayPointOperator {

    val errorSubject: PublishSubject<String>
    val error: Observable<String>
    val state: Observable<MissionState>
    val stateSubject: BehaviorSubject<MissionState>

    fun init(): Boolean

    fun upload(mission: Mission , callback: CommonCallbacks.CompletionCallback)
    fun start(callback: CommonCallbacks.CompletionCallback)
    fun stop()
    fun pause()
    fun resume()
}