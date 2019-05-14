package com.kaisavx.AircraftController.service

import com.amap.api.maps.model.LatLng
import com.kaisavx.AircraftController.RealmObject.MissionData
import com.kaisavx.AircraftController.RealmObject.WayPointData
import com.kaisavx.AircraftController.interfaces.WayPointOperator
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
    None, Uploading, Executing, Paused, Resuming, ReadyToExecuting ,Finish
}

// type 0: Area 1: Path
class Mission(val addressLatitude:Double , val addressLongitude:Double, val type: Int) {

    companion object {
        val SPACING = 30F
        val ALTITUDE = 75F
        val SPEED = 5F
        val TIME_INTERVAL = 10F
        val DISTANCE_INTERVAL = 0F
        val GIMBAL_PITCH = -70f
        val FINISHED_ACTION = WaypointMissionFinishedAction.GO_HOME.value()
    }

    var __id:String?=null
    var id: String?=null
    var userId:String?=null
    var name=""
    var spacing = SPACING
    var address = ""
    var createdTime = Date().time

    var bearing=0f

    var altitude = ALTITUDE
    var speed = SPEED
    var timeInterval = TIME_INTERVAL
    var distanceInterval = DISTANCE_INTERVAL
    var gimbalPitch = GIMBAL_PITCH
    var finishedAction = FINISHED_ACTION

    var isAltitudeOverall = true
    var isSpeedOverall = true
    var isTimeIntervalOverall = true
    var isDistanceOverall = true
    var isGimbalPitchOverall = true

    var markerPointList: List<LatLng> = listOf()

    var wayPointList: List<Waypoint> = listOf()

    init {
        __id=UUID.randomUUID().toString()
    }

    constructor(data: MissionData) : this(data.addressLatitude, data.addressLongitude, data.type) {
        __id = data._id
        id = data.id
        userId = data.userId
        name =data.name
        address = data.address
        spacing = data.spacing
        createdTime = data.createdAt

        bearing = data.bearing
        altitude = data.altitude
        speed = data.speed
        timeInterval = data.timeInterval
        distanceInterval = data.distanceInterval
        gimbalPitch = data.gimbalPitch
        finishedAction = data.finishedAction

        isAltitudeOverall = data.isAltitudeOverall
        isSpeedOverall = data.isSpeedOverall
        isTimeIntervalOverall = data.isTimeIntervalOverall
        isDistanceOverall = data.isDistanceOverall
        isGimbalPitchOverall = data.isGimbalPitchOverall

        markerPointList = parseByteDataToPoints(data.markerPointsData)

        wayPointList=data.wayPointsData.map {
            val waypoint = Waypoint(it.latitude, it.longitude, it.altitude)
            waypoint.heading = it.heading
            waypoint.cornerRadiusInMeters = it.cornerRadiusInMeters
            waypoint.turnMode = WaypointTurnMode.find(it.turnMode)
            waypoint.gimbalPitch = it.gimbalPitch
            waypoint.speed = it.speed
            waypoint.shootPhotoTimeInterval = it.shootPhotoTimeInterval
            waypoint.shootPhotoDistanceInterval = it.shootPhotoDistanceInterval

            waypoint

            //log(this , "waypoint : $waypoint")
        }

        //wayPoints = parseByteDataToPoints(data.wayPointsData)
    }

    fun save() {
        logMethod(this)

        RealmKit.executeTransaction {realm ->
            log(this,"id:$id __id:$__id")
            var exist: MissionData?=null

            __id?.let {
                exist = realm.where(MissionData::class.java)
                        .equalTo("_id", __id).findFirst()
            }



            if(exist == null){
                id?.let {
                    exist = realm.where(MissionData::class.java)
                            .equalTo("id", id).findFirst()
                }
            }

            if (exist == null) {
                log(this, "exist is null create")
                exist = realm.createObject(MissionData::class.java)

            } else {
                log(this, "exist is exist")
            }

            exist?.id = id
            exist?._id=__id
            exist?.userId = userId
            exist?.type = type
            exist?.address = address
            exist?.name = name
            exist?.spacing = spacing
            exist?.createdAt = createdTime

            exist?.addressLatitude = addressLatitude
            exist?.addressLongitude = addressLongitude

            exist?.bearing = bearing
            exist?.altitude = altitude
            exist?.speed = speed
            exist?.timeInterval = timeInterval
            exist?.distanceInterval = distanceInterval
            exist?.gimbalPitch = gimbalPitch
            exist?.finishedAction = finishedAction

            exist?.isAltitudeOverall = isAltitudeOverall
            exist?.isSpeedOverall = isSpeedOverall
            exist?.isTimeIntervalOverall = isTimeIntervalOverall
            exist?.isDistanceOverall = isDistanceOverall
            exist?.isGimbalPitchOverall = isGimbalPitchOverall

            exist?.markerPointsData = generatePointsData(markerPointList)
            exist?.wayPointsData?.clear()
            //log(this , "finishedAction:$finishedAction")
            log(this , "wayPointList:${wayPointList.size}")
            wayPointList.map {
                log(this , "shootPotoTimeInterval:${it.shootPhotoTimeInterval}")
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

    fun newMission(center: LatLng, type: Int, markerPointList: List<LatLng>) {
        log(this, "newMission")
        val newMission = Mission(center.latitude,center.longitude, type)
        newMission.address = "${center.longitude}, ${center.latitude}"
        newMission.name = newMission.address
        newMission.markerPointList = markerPointList

        setCurrentMission(newMission)

        missions.add(newMission)
        missionListSubject.onNext(missions)
    }

    fun newWayPoint(mission: Mission, position: LatLng): Waypoint {
        var altitude = 0f

        if(mission.isAltitudeOverall){
            altitude = mission.altitude
        }else{
            altitude = Mission.ALTITUDE
        }

        val wayPoint = Waypoint(position.latitude, position.longitude, altitude)
        wayPoint.heading = 0
        wayPoint.cornerRadiusInMeters = 0.2f
        wayPoint.turnMode = WaypointTurnMode.CLOCKWISE

        if(mission.isSpeedOverall){
            wayPoint.speed = mission.speed
        }else{
            wayPoint.speed = Mission.SPEED
        }
        if(mission.isTimeIntervalOverall){
            wayPoint.shootPhotoTimeInterval = mission.timeInterval
        }else{
            wayPoint.shootPhotoTimeInterval = Mission.TIME_INTERVAL
        }
        if(mission.isGimbalPitchOverall){
            wayPoint.gimbalPitch = mission.gimbalPitch
        }else {
            wayPoint.gimbalPitch = Mission.GIMBAL_PITCH
        }
        if(mission.isDistanceOverall){
            wayPoint.shootPhotoDistanceInterval = mission.distanceInterval
        }else{
            wayPoint.shootPhotoDistanceInterval = Mission.DISTANCE_INTERVAL
        }

        return wayPoint
    }

    fun addMission(m:Mission){
        var flag = true
        missions.forEach {
            if(m.id == it.id){
                flag = false
            }
        }
        if(flag){
            m.save()
            missions.add(m)
            missionListSubject.onNext(missions)
            log(this ,"add mission:${m.id} ")
        }else{
            log(this ,"mission is exist")
        }


    }

    fun deleteMission(mission: Mission) {
        mission.delete()
        if (mission == currentMissionSubject.value!!.value) {
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
                    log(this , "$it")

                    it.wayPointsData.map {
                        log(this , "$it")
                    }
                    Mission(it)
                }
    }
}