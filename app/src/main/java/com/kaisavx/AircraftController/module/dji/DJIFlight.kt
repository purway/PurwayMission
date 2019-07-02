package com.kaisavx.AircraftController.module.dji

import com.kaisavx.AircraftController.util.KLog
import dji.common.error.DJIError
import dji.common.flightcontroller.FlightControllerState
import dji.common.util.CommonCallbacks
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class DJIFlight(val aircraft: Aircraft ,val fc:FlightController){
    companion object {
        private val TAG="DJIFlight"
    }

    val displayName:String?=aircraft.model?.displayName

    var serialNumber:String?=null
    private set

    var firmwareVersion:String?=null
    private set

    private val fcStateSub = BehaviorSubject.create<FlightControllerState>()

    val fcStateOb: Observable<FlightControllerState> = fcStateSub
    val onBoardProcessor = OnBoardProcessor{command,errorSub->
        if(fc.isOnboardSDKDeviceAvailable){
            fc.sendDataToOnboardSDKDevice(command.toByte(),{error->
                error?.let {
                    KLog.e(TAG,"sendDataToOnboardSDKDevice:${it.description}")
                }
            })
        }
    }

    init {
        setupFC()
    }

    private fun setupFC(){
        fc.setOnboardSDKDeviceDataCallback {data->
            onBoardProcessor.receiveOnBoardData(data)
        }

        fc.setStateCallback { state->
            fcStateSub.onNext(state)
        }

        fc.getSerialNumber(object:CommonCallbacks.CompletionCallbackWith<String>{
            override fun onSuccess(no: String?) {
                serialNumber = no
            }

            override fun onFailure(error: DJIError?) {
                KLog.e(TAG,"getSerialNumber error:${error?.description}")
            }
        })

        fc.getFirmwareVersion(object:CommonCallbacks.CompletionCallbackWith<String>{
            override fun onSuccess(version: String?) {
                firmwareVersion = version
            }

            override fun onFailure(error: DJIError?) {
                KLog.e(TAG,"getFirmwareVersion error:${error?.description}")
            }
        })
        onBoardProcessor.connectDevice()

    }

    fun destory(){
        onBoardProcessor.disconnectDevice()
    }
}