package com.kaisavx.AircraftController.RealmObject

import io.realm.RealmObject


open class WayPointData: RealmObject() {
    //var coordinate2D = LocationCoordinate2D(0.0,0.0)
    var latitude = 0.0
    var longitude = 0.0

    var altitude = 70f
    var heading = 0
    var cornerRadiusInMeters =0.2f
    //var turnMode = WaypointTurnMode.CLOCKWISE
    var turnMode = 0
    var gimbalPitch = 0f
    var speed = 5.0f
    var shootPhotoTimeInterval = 0f
    var shootPhotoDistanceInterval = 0f
}
