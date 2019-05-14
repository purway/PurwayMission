package com.kaisavx.AircraftController.processor

import android.content.Context
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.kaisavx.AircraftController.mamager.DJIManager2
import com.kaisavx.AircraftController.model.FlyPoint
import com.kaisavx.AircraftController.model.FlyRecord
import com.kaisavx.AircraftController.util.logMethod
import com.kaisavx.AircraftController.util.wgs84togcj02
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.GPSSignalLevel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class FlyRecordProcessor(val context: Context){

    private val disposable = CompositeDisposable()

    val flyPointListSubject: PublishSubject<List<FlyPoint>> = PublishSubject.create()

    var flyRecord: FlyRecord?=null

    init {
        initDisposable()
    }

    fun start(){

    }

    fun destory(){
        disposable.clear()
    }

    private fun initDisposable() {
        disposable.add(DJIManager2.flightStateSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {state ->

                    if(state.isFlying){
                        if(flyRecord == null){
                            startProcess()
                        }
                        addPoint(state)
                    }else{
                        if(flyRecord != null){
                            stopProcess()
                        }
                    }
                })
    }

    private fun startProcess(){
        logMethod(this)
        flyRecord = FlyRecord(0)
        DJIManager2.aircraftNoSubject.value?.let {
            flyRecord?.aircraftNo =it
        }
        flyRecord?.startTime = System.currentTimeMillis()

    }

    private fun stopProcess(){
        logMethod(this)
        flyRecord?.stopTime = System.currentTimeMillis()
        flyRecord?.save()
        flyRecord = null
    }

    private fun addPoint(state:FlightControllerState){
        logMethod(this)
        var wgs:LatLng?=null
        state.aircraftLocation?.let {
            if(state.gpsSignalLevel != GPSSignalLevel.LEVEL_0 &&
                    state.gpsSignalLevel!= GPSSignalLevel.LEVEL_1 /*&&
                                    state.gpsSignalLevel != GPSSignalLevel.LEVEL_2*/){
                wgs = wgs84togcj02(LatLng(it.latitude, it.longitude))
            }
        }

        if(flyRecord!!.addressLatitude ==null ||
                flyRecord!!.addressLongitude == null){
            wgs?.let {
                flyRecord?.addressLatitude = it.latitude
                flyRecord?.addressLongitude = it.longitude
                getAddress(it)
            }

        }

        val flyPoint = FlyPoint(wgs?.latitude,
                wgs?.longitude,
                state.aircraftLocation.altitude,
                state.velocityX,
                state.velocityY,
                state.velocityZ,
                System.currentTimeMillis())
        flyRecord?.flyPointList?.add(flyPoint)
        flyRecord?.flyPointList?.let {
            flyPointListSubject.onNext(it)
        }
    }

    private fun getAddress(location:LatLng){
        val search = GeocodeSearch(context)
        search.setOnGeocodeSearchListener(object:GeocodeSearch.OnGeocodeSearchListener{
            override fun onGeocodeSearched(result: GeocodeResult?, p1: Int) {

            }

            override fun onRegeocodeSearched(result: RegeocodeResult?, p1: Int) {
                result?.let {
                    flyRecord?.address = it.regeocodeAddress.formatAddress
                    flyRecord?.name =flyRecord!!.address
                }
            }
        })
        search.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(location.latitude,location.longitude),100f , GeocodeSearch.AMAP))

    }
}
