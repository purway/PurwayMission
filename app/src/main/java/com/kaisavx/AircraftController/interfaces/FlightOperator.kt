package com.kaisavx.AircraftController.interfaces

import dji.common.model.LocationCoordinate2D
import dji.common.util.CommonCallbacks
import io.reactivex.Observable

interface FlightOperator {
    val error:Observable<String>



//Flight Action
    fun turnOnMotors()
    fun turnOffMotors()
    fun startTakeOff()
    fun cancelTakeOff()
    fun startLanding()
    fun cancelLanding()
    fun confirmLanding()

//Home
    fun startGoHome()
    fun cancelGoHome()

    fun setHomeLocation()
    fun getHomeLocation(callback: CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>)


    /*
    fun setGoHomeHeightInMeters(height:Int , callback: CommonCallbacks.CompletionCallback)
    fun getGoHomeHeightInMeters(callback: CommonCallbacks.CompletionCallbackWith<Int>)

    //fail safes
    fun setConnectionFailSafeBehavior(behavior: ConnectionFailSafeBehavior , callback:CommonCallbacks.CompletionCallback)
    fun getConnectionFailSafeBehavior(callback: CommonCallbacks.CompletionCallbackWith<ConnectionFailSafeBehavior>)
    fun setLowBatteryWarningThreshold(percent:Int,callback: CommonCallbacks.CompletionCallback)
    fun getLowBatteryWarningThreshold(callback: CommonCallbacks.CompletionCallback)
    fun setSeriousLowBatteryWarningThreshold(percent:Int ,callback: CommonCallbacks.CompletionCallback)
    fun getSeriousLowBatteryWarningThreshold(callback: CommonCallbacks.CompletionCallbackWith<Int>)
    fun setSmartReturnToHomeEnabled(enabled:Boolean , callback: CommonCallbacks.CompletionCallback)
    fun getSmartReturnToHomeEnabled(callback: CommonCallbacks.CompletionCallbackWith<Int>)
    fun confirmSmartReturnToHomeRequest(enabled: Boolean,callback: CommonCallbacks.CompletionCallback)
*/

}