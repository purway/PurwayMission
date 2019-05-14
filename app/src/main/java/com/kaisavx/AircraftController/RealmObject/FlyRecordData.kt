package com.kaisavx.AircraftController.RealmObject

import io.realm.RealmList
import io.realm.RealmObject

open class FlyRecordData:RealmObject(){
    var __id:String?=null
    var id:String?=null
    var userId:String?=null
    var name =""
    var address =""
    var aircraftNo=""
    var addressLatitude:Double?=null
    var addressLongitude:Double?=null
    var bearing=0f
    var type=0
    var startTime:Long = 0
    var stopTime:Long = 0

    var flyPointList = RealmList<FlyPointData>()
}