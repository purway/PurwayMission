package com.kaisavx.AircraftController.mamager

import com.kaisavx.AircraftController.RealmObject.FlyRecordData
import com.kaisavx.AircraftController.model.FlyRecord
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import java.text.SimpleDateFormat
import java.util.*

object FlyRecordManager {
    val flyRecords: ArrayList<FlyRecord> = arrayListOf()

    val flyRecordListSubject: BehaviorSubject<List<FlyRecord>> = BehaviorSubject.create()

    init {
        reload()
    }

    private fun getFlyRecordList(): List<FlyRecord> {
        logMethod(this)
        return Realm.getDefaultInstance()
                .where(FlyRecordData::class.java)
                .findAll()
                .map {

                    val flyRecord=FlyRecord(it)
                    log(this ,"flyRecord:${flyRecord.name} ${flyRecord.flyPointList.size}")

                    flyRecord.flyPointList.map {
                        log(this ,"t:${SimpleDateFormat("MMdd HH:mm:ss").format(Date(it.time))} lat:${it.latitude} lng:${it.longitude}")
                    }

                flyRecord
                }
    }

    fun reload(){
        flyRecords.clear()
        flyRecords += getFlyRecordList()
        flyRecordListSubject.onNext(flyRecords)
    }

    fun removeFlyRecord(flyRecord: FlyRecord){
        flyRecord.delete()
        flyRecords.remove(flyRecord)
    }

    fun addFlyRecord(flyRecord: FlyRecord){
        val f = flyRecords.find { (it.id!=null && it.id == flyRecord.id ) }
        if(f==null){
            flyRecord.save()
            flyRecords.add(flyRecord)
        }
    }
}