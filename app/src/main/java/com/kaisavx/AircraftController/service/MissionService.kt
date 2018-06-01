package com.kaisavx.AircraftController.service

import com.amap.api.maps.model.LatLng
import com.kaisavx.AircraftController.interfaces.WayPointOperator
import com.kaisavx.AircraftController.model.MissionData
import com.kaisavx.AircraftController.model.WayPointData
import com.kaisavx.AircraftController.util.NullableObject
import com.kaisavx.AircraftController.util.RealmKit
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import dji.common.mission.waypoint.Waypoint
import dji.common.mission.waypoint.WaypointMissionFinishedAction
import dji.common.mission.waypoint.WaypointTurnMode
import dji.common.util.CommonCallbacks
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import java.nio.ByteBuffer
import java.util.*

/**
 * Created by windless on 2017/9/21.
 */


enum class MissionState {
    None, Uploading, Executing, Paused, Resuming, ReadyToExecuting
}

// type 0: Area 1: Path
class Mission(var center: LatLng, val type: Int) {
    var id: String = ""
    var spacing = 30F
    var address = ""
    var createdAt = Date().time

    var altitude = 75F
    var speed = 5F
    var timeInterval = 2f
    var distanceInterval = 0f
    var gimbalPitch = 0f
    var finishedAction: WaypointMissionFinishedAction = WaypointMissionFinishedAction.GO_HOME

    var isAltitudeOverall = false
    var isSpeedOverall = false
    var isTimeIntervalOverall = false
    var isDistanceOverall = false
    var isGimbalPitchOverall = false

    var markerPoints: List<LatLng> = listOf()

    var wayPoints: List<Waypoint> = listOf()

    init {
        id = UUID.randomUUID().toString()
        if (type == 0) {
            isAltitudeOverall = true
            isSpeedOverall = true
            isTimeIntervalOverall = true
            isDistanceOverall = true
            isGimbalPitchOverall = true
        }
    }

    constructor(data: MissionData) : this(LatLng(data.addressLatitude, data.addressLongitude), data.type) {
        id = data.id
        address = data.address
        spacing = data.spacing
        createdAt = data.createdAt

        altitude = data.altitude
        speed = data.speed
        timeInterval = data.timeInterval
        distanceInterval = data.distanceInterval
        gimbalPitch = data.gimbalPitch
        finishedAction = WaypointMissionFinishedAction.find(data.finishedAction)

        isAltitudeOverall = data.isAltitudeOverall
        isSpeedOverall = data.isSpeedOverall
        isTimeIntervalOverall = data.isTimeIntervalOverall
        isDistanceOverall = data.isDistanceOverall
        isGimbalPitchOverall = data.isGimbalPitchOverall

        markerPoints = parseByteDataToPoints(data.markerPointsData)

        data.wayPointsData.map {
            val waypoint = Waypoint(it.latitude, it.longitude, it.altitude)
            waypoint.heading = it.heading
            waypoint.cornerRadiusInMeters = it.cornerRadiusInMeters
            waypoint.turnMode = WaypointTurnMode.find(it.turnMode)
            waypoint.gimbalPitch = it.gimbalPitch
            waypoint.speed = it.speed
            waypoint.shootPhotoTimeInterval = it.shootPhotoTimeInterval
            waypoint.shootPhotoDistanceInterval = it.shootPhotoDistanceInterval

            wayPoints += waypoint
        }

        //wayPoints = parseByteDataToPoints(data.wayPointsData)
    }

    fun save() {
        logMethod(this)

        RealmKit.executeTransaction {
            var exist = it.where(MissionData::class.java)
                    .equalTo("id", id)
                    .findFirst()
            if (exist == null) {
                log(this, "exist is null create")
                exist = it.createObject(MissionData::class.java)
            } else {
                log(this, "exist is exist")
            }

            exist?.id = id
            exist?.type = type
            exist?.address = address
            exist?.spacing = spacing
            exist?.createdAt = createdAt

            exist?.addressLatitude = center.latitude
            exist?.addressLongitude = center.longitude

            exist?.altitude = altitude
            exist?.speed = speed
            exist?.timeInterval = timeInterval
            exist?.distanceInterval = distanceInterval
            exist?.gimbalPitch = gimbalPitch
            exist?.finishedAction = finishedAction.value()

            exist?.isAltitudeOverall = isAltitudeOverall
            exist?.isSpeedOverall = isSpeedOverall
            exist?.isTimeIntervalOverall = isTimeIntervalOverall
            exist?.isDistanceOverall = isDistanceOverall
            exist?.isGimbalPitchOverall = isGimbalPitchOverall

            exist?.markerPointsData = generatePointsData(markerPoints)
            exist?.wayPointsData?.clear()
            wayPoints.map {
                //log(this , "${it}")
                val wayPointData = WayPointData()
                wayPointData.latitude = it.coordinate.latitude
                wayPointData.longitude = it.coordinate.longitude
                wayPointData.altitude = it.altitude
                wayPointData.heading = it.heading
                wayPointData.cornerRadiusInMeters = it.cornerRadiusInMeters
                wayPointData.turnMode = it.turnMode.value()
                wayPointData.gimbalPitch = it.gimbalPitch
                wayPointData.speed = it.speed
                wayPointData.shootPhotoTimeInterval = it.shootPhotoTimeInterval
                wayPointData.shootPhotoDistanceInterval = it.shootPhotoDistanceInterval
                exist?.wayPointsData?.add(wayPointData)
            }

        }
    }

    fun delete() {
        logMethod(this)

        RealmKit.executeTransaction {
            val data = it.where(MissionData::class.java)
                    .equalTo("id", id)
                    .findFirst()
            data?.deleteFromRealm()
        }
    }

    private fun parseByteDataToPoints(bytes: ByteArray): List<LatLng> {
        val result: ArrayList<LatLng> = arrayListOf()

        val buffer = ByteBuffer.wrap(bytes)
        for (i in 0 until bytes.size step 8 * 2) {
            val latitude = buffer.getDouble(i)
            val longitude = buffer.getDouble(i + 8)
            result.add(LatLng(latitude, longitude))
        }
        return result
    }

    private fun generatePointsData(points: List<LatLng>): ByteArray {
        val buffer = ByteBuffer.allocate(points.size * 8 * 2)
        points.forEach {
            buffer.putDouble(it.latitude)
            buffer.putDouble(it.longitude)
        }
        return buffer.array()
    }

}

class MissionService(val operator: WayPointOperator) {
    private val missions: ArrayList<Mission> = arrayListOf()

    val currentMissionSubject: BehaviorSubject<NullableObject<Mission>> = BehaviorSubject.createDefault(NullableObject(null))
    val missionListSubject: BehaviorSubject<List<Mission>> = BehaviorSubject.create()

    var lastMission: Mission? = null

    init {
        logMethod(this)
        missions.clear()
        getMissionDataList().forEach {
            missions.add(it)
        }
        missionListSubject.onNext(missions)
    }

    fun newMission(center: LatLng, type: Int, markerPoints: List<LatLng>) {
        log(this, "newMission")
        val newMission = Mission(center, type)
        newMission.address = "${center.longitude}, ${center.latitude}"
        newMission.markerPoints = markerPoints

        setCurrentMission(newMission)

        missions.add(newMission)
        missionListSubject.onNext(missions)
    }

    fun newWayPoint(mission: Mission, position: LatLng): Waypoint {
        var altitude = 0f

        if(mission.isAltitudeOverall){
            altitude = mission.altitude
        }else{
            altitude = 75f
        }

        val wayPoint = Waypoint(position.latitude, position.longitude, altitude)
        wayPoint.heading = 0
        wayPoint.cornerRadiusInMeters = 0.2f
        wayPoint.turnMode = WaypointTurnMode.CLOCKWISE

        if(mission.isSpeedOverall){
            wayPoint.speed = mission.speed
        }else{
            wayPoint.speed = 5f
        }
        if(mission.isTimeIntervalOverall){
            wayPoint.shootPhotoTimeInterval = mission.timeInterval
        }else{
            wayPoint.shootPhotoTimeInterval = 2f
        }
        if(mission.isGimbalPitchOverall){
            wayPoint.gimbalPitch = mission.gimbalPitch
        }else {
            wayPoint.gimbalPitch = -90f
        }
        if(mission.isDistanceOverall){
            wayPoint.shootPhotoDistanceInterval = mission.distanceInterval
        }else{
            wayPoint.shootPhotoDistanceInterval = 0f
        }

        return wayPoint
    }

    fun deleteMission(mission: Mission) {
        mission.delete()
        if (mission == currentMissionSubject.value.value) {
            setCurrentMission(null)
        }
        missions.remove(mission)
        missionListSubject.onNext(missions)
    }

    fun uploadMission(mission: Mission, callback: CommonCallbacks.CompletionCallback) {
        mission.save()
        operator.upload(mission, callback)
    }

    fun startMission(callback: CommonCallbacks.CompletionCallback) {
        operator.start(callback)
    }

    fun stopMission() {
        operator.stop()
    }

    fun pauseMission() {
        operator.pause()
    }

    fun resumeMission() {
        operator.resume()
    }

    fun setCurrentMission(mission: Mission?) {

        lastMission = mission
        currentMissionSubject.onNext(NullableObject(mission))
    }

    private fun getMissionDataList(): List<Mission> {
        logMethod(this)
        return Realm.getDefaultInstance()
                .where(MissionData::class.java)
                .findAll()
                .map {
                    //log(this , "$it")

                    it.wayPointsData.map {
                        //log(this , "$it")
                    }
                    Mission(it)
                }
    }
}