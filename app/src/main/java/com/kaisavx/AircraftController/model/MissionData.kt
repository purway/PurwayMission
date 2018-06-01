package com.kaisavx.AircraftController.model

import io.realm.RealmList
import io.realm.RealmObject

open class MissionData : RealmObject() {
    var id: String = ""
    var type: Int = 0
    var address:String = ""
    var spacing: Float = 0f
    var createdAt: Long = 0

    var addressLatitude:Double = 0.0
    var addressLongitude:Double = 0.0

    var altitude:Float = 0f
    var speed: Float = 0f
    var timeInterval:Float = 0f
    var distanceInterval:Float = 0f
    var gimbalPitch:Float = 0f
    var finishedAction: Int = 0

    var isAltitudeOverall:Boolean = false
    var isSpeedOverall = false
    var isTimeIntervalOverall = false
    var isDistanceOverall = false
    var isGimbalPitchOverall = false



    var markerPointsData: ByteArray = ByteArray(0)
    var wayPointsData = RealmList<WayPointData>()

}