package com.kaisavx.AircraftController.module.dji

import com.kaisavx.AircraftController.device.Device
import com.kaisavx.AircraftController.device.air.AirDetector
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.NullableObject
import com.kaisavx.AircraftController.util.plusAssign
import com.kaisavx.AircraftController.util.toHex
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

open class OnBoardProcessor(val sendFunction: (OnBoardCommand, PublishSubject<Int>) -> Unit) {
    companion object {
        private val TAG ="OnBoardProcessor"
    }

    private val disposable = CompositeDisposable()
    private val versionDisposable = CompositeDisposable()
    private val deviceSub = BehaviorSubject.create<NullableObject<Device>>()
    private val serialNumberSub = BehaviorSubject.create<String>()
    private val firmwareVersionSub = BehaviorSubject.create<ByteArray>()
    protected val errorSub = PublishSubject.create<Int>()
    val deviceOb: Observable<NullableObject<Device>> = deviceSub

    fun receiveOnBoardData(data:ByteArray){
        val receive = OnBoardReceive(data)
        when(receive.command){
            Device.BaseCommand.NO.value->{
                val serialNumber = receive.dataArray.toHex().toUpperCase()
                KLog.i(TAG,"Device serialNo:$serialNumber")
                serialNumberSub.onNext(serialNumber)
            }
            Device.BaseCommand.FIRMWARE.value->{
                val version = receive.dataArray
                KLog.i(TAG,"firmware version:${version.toHex().toUpperCase()}")
                firmwareVersionSub.onNext(version)
            }
            else->{
                deviceSub.value?.value?.processReceive(receive)
            }
        }
    }

    fun connectDevice(){
        startGetSerialNumber()
        
        val dOb:Observable<Pair<String,ByteArray>> = Observable.combineLatest(
                serialNumberSub,
                firmwareVersionSub,
                BiFunction { t1, t2 ->  Pair(t1,t2) }
        )
        
        disposable += dOb
                .observeOn(Schedulers.computation())
                .subscribe { 
                    val no = it.first
                    val version = it.second.toHex().toUpperCase()
                    val type = it.second[0]
                    val device:Device = when(type){
                        Device.DeviceType.AIR_DETECTOR.value->{
                            AirDetector(no,version,{cmd->
                                sendCommand(cmd)
                            })
                        }
                        else->{
                            Device(no,version,type,{cmd->
                                sendCommand(cmd)
                            })
                        }
                    }
                    deviceSub.onNext(NullableObject(device))

                }
    }

    private fun startGetSerialNumber(){
        disposable += Observable.interval(500,TimeUnit.MILLISECONDS)
                .takeUntil(serialNumberSub)
                .observeOn(Schedulers.computation())
                .subscribe {
                    getSerialNumber()
                }

        disposable += serialNumberSub
                .observeOn(Schedulers.computation())
                .subscribe {
                    startGetDeviceVersion()
                }
        
    }

    private fun startGetDeviceVersion(){
        var count = 0
        versionDisposable.clear()

        versionDisposable += Observable.interval(500,TimeUnit.MILLISECONDS)
                .takeUntil(firmwareVersionSub)
                .observeOn(Schedulers.computation())
                .subscribe {
                    if(++count <= 3) {
                        getFirmwareVersion()
                    }else{
                        versionDisposable.clear()
                    }
                }
    }

    fun disconnectDevice(){
        KLog.i(TAG,"disconnectDevice")
        deviceSub.value?.value?.destory()
        deviceSub.onNext(NullableObject(null))
        disposable.clear()
        versionDisposable.clear()
    }

    fun sendCommand(command:OnBoardCommand){
        sendFunction.invoke(command, errorSub)
    }

    fun getSerialNumber(){
        KLog.i(TAG,"getSerialNumber")
        sendCommand(OnBoardCommand(actionNumber++, Device.BaseCommand.NO.value))
    }

    fun getFirmwareVersion(){
        KLog.i(TAG,"getFirmwareVersion")
        sendCommand(OnBoardCommand(actionNumber++, Device.BaseCommand.FIRMWARE.value))
    }

}

