package com.kaisavx.AircraftController.model

import com.kaisavx.AircraftController.RealmObject.FlyPointData
import com.kaisavx.AircraftController.RealmObject.FlyRecordData
import com.kaisavx.AircraftController.util.RealmKit
import com.kaisavx.AircraftController.util.logMethod
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class FlyRecord(val type:Int){
    var addressLatitude:Double?=null
    var addressLongitude:Double?=null
    var __id:String?=null
    var id:String?=null
    var userId:String?=null
    var name=""
    var address = ""
    var aircraftNo: String = ""
    var bearing=0f

    var startTime: Long = 0
    var stopTime: Long = 0

    var flyPointList=CopyOnWriteArrayList<FlyPoint>()
    
    init {
        __id = UUID.randomUUID().toString()
    }

    constructor(data:FlyRecordData):this(data.type){
        __id = data.__id
        id = data.id
        addressLatitude = data.addressLatitude
        addressLongitude = data.addressLongitude
        userId = data.userId
        name = data.name
        address = data.address
        aircraftNo = data.aircraftNo
        bearing = data.bearing
        startTime = data.startTime
        stopTime = data.stopTime
        data.flyPointList.map {
            flyPointList.add(
            FlyPoint(it.latitude,
                    it.longitude,
                    it.altitude,
                    it.velocityX,
                    it.velocityY,
                    it.velocityZ,
                    it.time))
        }
    }
    
    fun save(){
        RealmKit.executeTransaction {realm ->
            var exist:FlyRecordData ?= null
            __id?.let {
                exist = realm.where(FlyRecordData::class.java)
                        .equalTo("__id",__id).findFirst()
            }
            if(exist == null){
                id?.let {
                    exist = realm.where(FlyRecordData::class.java)
                            .equalTo("id",id).findFirst()
                }
            }

            if(exist == null){
                exist = realm.createObject(FlyRecordData::class.java)
            }else{

            }

            exist?.id = id
            exist?.__id = __id
            exist?.userId = userId
            exist?.name = name
            exist?.address = address
            exist?.aircraftNo = aircraftNo
            exist?.addressLatitude = addressLatitude
            exist?.addressLongitude = addressLongitude
            exist?.bearing = bearing
            exist?.type = type
            exist?.startTime = startTime
            exist?.stopTime = stopTime
            exist?.flyPointList?.clear()

            flyPointList.map {
                val flyPointData = FlyPointData()
                flyPointData.latitude = it.latitude
                flyPointData.longitude = it.longitude
                flyPointData.altitude = it.altitude
                flyPointData.velocityX = it.velocityX
                flyPointData.velocityY = it.velocityY
                flyPointData.velocityZ = it.velocityZ
                flyPointData.time = it.time
                exist?.flyPointList?.add(flyPointData)
            }

        }
    }
    
    fun delete(){
        logMethod(this)
        RealmKit.executeTransaction {realm ->
            var data = realm.where(FlyRecordData::class.java)
                    .equalTo("__id",__id).findFirst()
            if(data == null){
                id?.let {
                    data = realm.where(FlyRecordData::class.java)
                            .equalTo("id",id).findFirst()
                }
            }
            data?.deleteFromRealm()
        }
    }
}