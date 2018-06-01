package com.kaisavx.AircraftController.interfaces

import com.amap.api.maps.model.LatLng
import com.kaisavx.AircraftController.service.Mission
import com.kaisavx.AircraftController.service.MissionState
import com.kaisavx.AircraftController.util.gcj02towgs84
import com.kaisavx.AircraftController.util.log
import dji.common.error.DJIError
import dji.common.mission.waypoint.*
import dji.common.util.CommonCallbacks
import dji.sdk.mission.waypoint.WaypointMissionOperator
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import dji.sdk.sdkmanager.DJISDKManager
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Created by windless on 2017/12/14.
 */
class DJIWayPointOperator : WayPointOperator {
    override val errorSubject: PublishSubject<String> = PublishSubject.create()
    override val stateSubject: BehaviorSubject<MissionState> = BehaviorSubject.createDefault(MissionState.None)
    private var djiOperator: WaypointMissionOperator? = null

    override val error: Observable<String> = errorSubject
    override val state: Observable<MissionState> = stateSubject

    private var lastState: WaypointMissionState? = null

    override fun init(): Boolean {
        val operator = getDJIOperator()
        return operator != null
    }

    override fun upload(mission: Mission, callback: CommonCallbacks.CompletionCallback) {
        log(this, "start upload mission")

        val op = getDJIOperator() ?: return

        log(this, "stop mission first for uploading mission")
        op.stopMission {
            log(this, "load mission")

            val loadError = op.loadMission(toDJIMission(mission))
            if (loadError != null) {
                log(this, "load mission failed")
                errorSubject.onNext("加载飞行任务失败: $loadError")
            } else {
                log(this, "uploading mission")
                op.uploadMission(callback)
            }
        }
    }

    override fun start(callback: CommonCallbacks.CompletionCallback) {
        log(this, "starting mission")
        getDJIOperator()?.startMission(callback)
    }

    override fun stop() {
        log(this, "stopping mission")
        getDJIOperator()?.stopMission { error ->
            if (error != null) {
                log(this, "stop mission error: ${error.description}")
                errorSubject.onNext("停止飞行任务失败: $error")
            }
        }
    }

    override fun pause() {
        log(this, "pausing mission")
        getDJIOperator()?.pauseMission { error ->
            if (error != null) {
                log(this, "pause mission error: ${error.description}")
                errorSubject.onNext("暂停飞行任务失败: $error")
            }
        }
    }

    override fun resume() {
        log(this, "resuming mission")
        getDJIOperator()?.resumeMission { error ->
            if (error != null) {
                log(this, "resume mission error: ${error.description}")
                errorSubject.onNext("恢复飞行任务失败: $error")
            }
        }
    }

    private val listener = object : WaypointMissionOperatorListener {
        override fun onExecutionFinish(error: DJIError?) {
            log(this, "onExecutionFinish ${error?.description}")
            stateSubject.onNext(MissionState.None)

            if (error != null) {
                errorSubject.onNext(error.description)
            }
        }

        override fun onExecutionStart() {
            log(this, "onExecutionStart")
        }

        override fun onUploadUpdate(event: WaypointMissionUploadEvent) {
            log(this, "onUploadUpdate ${event.currentState.name}, error: ${event.error}")

            if (lastState == WaypointMissionState.READY_TO_EXECUTE && event.currentState == WaypointMissionState.UPLOADING) {
                lastState = event.currentState
                return
            } else {
                lastState = event.currentState
            }

            when (event.currentState) {
                WaypointMissionState.READY_TO_UPLOAD -> stateSubject.onNext(MissionState.None)
                WaypointMissionState.UPLOADING -> stateSubject.onNext(MissionState.Uploading)
                WaypointMissionState.EXECUTING -> stateSubject.onNext(MissionState.Executing)
                WaypointMissionState.EXECUTION_PAUSED -> stateSubject.onNext(MissionState.Paused)
                WaypointMissionState.READY_TO_EXECUTE -> stateSubject.onNext(MissionState.ReadyToExecuting)
            }

            val error = event.error
            if (error != null) {
                log(this, "on upload update error: $error")
                errorSubject.onNext(error.description)
            }
        }

        override fun onDownloadUpdate(event: WaypointMissionDownloadEvent) {
            val error = event.error
            if (error != null) {
                errorSubject.onNext(error.description)
            }
        }

        override fun onExecutionUpdate(event: WaypointMissionExecutionEvent) {
            log(this, "onExecutionUpdate ${event.currentState.name}, error: ${event.error}")

            val error = event.error
            if (error != null) {
                errorSubject.onNext(error.description)
            }
            when (event.currentState) {
                WaypointMissionState.READY_TO_UPLOAD -> stateSubject.onNext(MissionState.None)
                WaypointMissionState.UPLOADING -> stateSubject.onNext(MissionState.Uploading)
                WaypointMissionState.EXECUTING -> stateSubject.onNext(MissionState.Executing)
                WaypointMissionState.EXECUTION_PAUSED -> stateSubject.onNext(MissionState.Paused)
                WaypointMissionState.READY_TO_EXECUTE -> stateSubject.onNext(MissionState.ReadyToExecuting)
            }
        }
    }

    fun getDJIOperator(): WaypointMissionOperator? {
        val operator = djiOperator
        if (operator == null) {
            val djiOperator = DJISDKManager.getInstance()?.missionControl?.waypointMissionOperator

            log(this, "get dji operator $operator")

            if (djiOperator == null) {
                errorSubject.onNext("dji operator init failed")
                log(this, "dji operator init failed")
            } else {
                djiOperator.addListener(listener)
                log(this, "dji operator init success")
            }

            this.djiOperator = djiOperator
        }
        return djiOperator
    }

    private fun toDJIMission(mission: Mission): WaypointMission {
        val building = WaypointMission.Builder()
                .autoFlightSpeed(mission.speed)
                .finishedAction(mission.finishedAction)
                .gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY)
                .setGimbalPitchRotationEnabled(true)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .headingMode(WaypointMissionHeadingMode.AUTO)
                .maxFlightSpeed(15f)

        var lastWayPoint = mission.wayPoints[0]

        mission.wayPoints.forEach {

            if (lastWayPoint.altitude != it.altitude) {
                val wgs84Point = gcj02towgs84(LatLng(lastWayPoint.coordinate.latitude, lastWayPoint.coordinate.longitude))
                val waypoint = Waypoint(wgs84Point.latitude, wgs84Point.longitude, it.altitude)
                waypoint.heading = lastWayPoint.heading
                waypoint.cornerRadiusInMeters = lastWayPoint.cornerRadiusInMeters
                waypoint.turnMode = lastWayPoint.turnMode
                waypoint.gimbalPitch = lastWayPoint.gimbalPitch
                waypoint.speed = lastWayPoint.speed
                waypoint.shootPhotoTimeInterval = lastWayPoint.shootPhotoTimeInterval
                waypoint.shootPhotoDistanceInterval = lastWayPoint.shootPhotoDistanceInterval
                building.addWaypoint(waypoint)

            }

            val wgs84Point = gcj02towgs84(LatLng(it.coordinate.latitude, it.coordinate.longitude))
            val waypoint = Waypoint(wgs84Point.latitude, wgs84Point.longitude, it.altitude)

            waypoint.heading = it.heading
            waypoint.cornerRadiusInMeters = it.cornerRadiusInMeters
            waypoint.turnMode = it.turnMode
            waypoint.gimbalPitch = it.gimbalPitch
            waypoint.speed = it.speed
            waypoint.shootPhotoTimeInterval = it.shootPhotoTimeInterval
            waypoint.shootPhotoDistanceInterval = it.shootPhotoDistanceInterval
            building.addWaypoint(waypoint)
            lastWayPoint = it

        }

        val wayPointMission = building.build()
        val error = wayPointMission.checkParameters()
        if (error != null) {
        }
        return wayPointMission
    }
}