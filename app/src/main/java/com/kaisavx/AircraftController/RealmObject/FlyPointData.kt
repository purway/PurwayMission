package com.kaisavx.AircraftController.RealmObject

import io.realm.RealmObject

open class FlyPointData:RealmObject(){
    var latitude:Double?=null
    var longitude:Double?=null
    var altitude=0f
    var velocityX = 0f
    var velocityY = 0f
    var velocityZ = 0f
    var time=0L
}