package com.kaisavx.AircraftController.device.air

import com.kaisavx.AircraftController.device.Device
import com.kaisavx.AircraftController.module.dji.OnBoardCommand
import com.kaisavx.AircraftController.module.dji.OnBoardReceive
import com.kaisavx.AircraftController.module.dji.actionNumber
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.plusAssign
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class AirDetector(no: String,
                  firmware: String,
                  sendFunction: (OnBoardCommand) -> Unit) :
        Device(no, firmware, DeviceType.AIR_DETECTOR.value, sendFunction) {

    companion object {
        private val TAG="AirDetector"
    }

    enum class Command(val value:Byte){
        AIR_COLLECT(0x03),
        AIR_SAMPLE(0x04),
    }

    enum class CollectParams(val value:Byte){
        START(0x01),
        STOP(0x00),
    }

    private val disposable = CompositeDisposable()
    private val collectTimeDisposable = CompositeDisposable()

    private val collectingAirTimeSub = BehaviorSubject.create<Int>()
    private val collectCompletedSub = BehaviorSubject.create<Unit>()
    private val airStatusSub = BehaviorSubject.create<AirDataCollection>()
    private val collectStatusSub = BehaviorSubject.create<Int>()

    val collectingAirTimeOb:Observable<Int> = collectingAirTimeSub
    val collectCompletedOb:Observable<Unit> = collectCompletedSub
    val airStatusOb:Observable<AirDataCollection> = airStatusSub

    init {
        disposable += Observable.interval(500,TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .subscribe {
                    sampleAir()
                }
    }

    override  fun processReceive(receive: OnBoardReceive){
        val cmd = receive.command
        when(cmd){
            Command.AIR_COLLECT.value->{

            }

            Command.AIR_SAMPLE.value->{
                decode(receive.dataArray)
            }
            else->{
                KLog.e(TAG,"processReceive cmd:$cmd")
            }
        }
    }

    override fun destory() {
        super.destory()
        disposable.clear()
        collectTimeDisposable.clear()
    }

    private fun decode(byteArray:ByteArray){
        val timestamp = System.currentTimeMillis()
        val len = byteArray.size
        if (len != 67){
            KLog.e(TAG,"decode data size error:$len")
            return
        }

        val buffer = ByteBuffer.wrap(byteArray)
        var index = 0
        val bumpStatus = buffer.get(index++).toInt()
        collectStatusSub.onNext(bumpStatus)

        val tInside = buffer.getShort(index)
        index+=2
        val hInside = buffer.getShort(index)
        index+=2
        val tOutside = buffer.getShort(index)
        index+=2
        val hOutside = buffer.getShort(index)
        index+=2
        val tempPressure = buffer.getInt(index)
        index+=4
        val pressure = buffer.getInt(index)
        index+=4
        val PM1p0 = buffer.getShort(index)
        index+=2
        val PM2p5 = buffer.getShort(index)
        index+=2
        val PM10 = buffer.getShort(index)
        index+=2
        val NUM0p3 = buffer.getShort(index)
        index+=2
        val NUM0p5 = buffer.getShort(index)
        index+=2
        val NUM1p0 = buffer.getShort(index)
        index +=2
        val NUM2p5 = buffer.getShort(index)
        index +=2
        val NUM5p0 = buffer.getShort(index)
        index +=2
        val NUM10 = buffer.getShort(index)
        index+=2

        val vList = ArrayList<Short>()
        for(i in 0 until 16){
            vList.add(buffer.getShort(index))
            index+=2
        }

        val airDataCollection = AirDataCollection(
                timestamp,
                tInside,
                hInside,
                tOutside,
                hOutside,
                tempPressure,
                pressure,
                PM1p0,
                PM2p5,
                PM10,
                NUM0p3,
                NUM0p5,
                NUM1p0,
                NUM2p5,
                NUM5p0,
                NUM10,
                vList
        )
        KLog.d(TAG,"airDataCollection:$airDataCollection")
        airStatusSub.onNext(airDataCollection)

    }

    fun workOnCollect(){
        collectTimeDisposable.clear()
        collectingAirTimeSub.onNext(0)

        startCollectAir()

        collectTimeDisposable += Observable.interval(1,TimeUnit.SECONDS)
                .observeOn(Schedulers.computation())
                .subscribe {
                    if(it>=120){
                        cancelCollect()
                        collectCompletedSub.onNext(Unit)
                    }else{
                        collectingAirTimeSub.onNext(it.toInt())
                    }
                }
    }

    fun cancelCollect(){
        collectTimeDisposable.clear()
        collectingAirTimeSub.onNext(-1)
        stopCollectAir()
    }

    private fun sampleAir(){
        KLog.i(TAG,"sampleAir")
        sendCommand(OnBoardCommand(actionNumber++,Command.AIR_SAMPLE.value))
    }

    private fun startCollectAir(){
        KLog.i(TAG,"startCollectAir")
        sendCommand(OnBoardCommand(actionNumber++,Command.AIR_COLLECT.value, CollectParams.START.value))
    }

    private fun stopCollectAir(){
        KLog.i(TAG,"stopCollectAir")
        sendCommand(OnBoardCommand(actionNumber++,Command.AIR_COLLECT.value, CollectParams.STOP.value))
    }
}