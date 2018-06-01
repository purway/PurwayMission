package com.kaisavx.AircraftController.interfaces

import com.kaisavx.AircraftController.util.log
import dji.common.error.DJIError
import dji.common.model.LocationCoordinate2D
import dji.common.util.CommonCallbacks
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class DJIFlightOperator:FlightOperator{
    private val errorSubject: PublishSubject<String> = PublishSubject.create()

    override val error: Observable<String> = errorSubject

    val stateSubject:PublishSubject<FlightState> = PublishSubject.create()

    val state:Observable<FlightState> =stateSubject

    enum class FlightState{
        NONE,
        MOTOR_ON,
        TAKE_OFF,
        TAKE_OFF_CANCELING,
        TAKE_OFF_CANCEL,
        FLYING,
        LANDING_START,
        LANDING,
        LANDING_CANCEL,
        LANDING_CONFIRM,
        LANDING_CONFIRM_START,
        LANDING_FINISH,
        MOTOR_OFF
    }

    private var flightController: FlightController?=null

    override fun turnOnMotors() {

        getDJIFlightController()?.turnOnMotors {
            if(it==null) {
                stateSubject.onNext(FlightState.MOTOR_ON)
            }else{
                showError("turn on mottors", it)
            }

        }

    }



    override fun turnOffMotors() {
        getDJIFlightController()?.turnOffMotors {
            if(it == null){
                stateSubject.onNext(FlightState.MOTOR_OFF)
            }else {
                showError("turn off mottors", it)
            }

        }
    }

    override fun startTakeOff(){
        getDJIFlightController()?.let {
            stateSubject.onNext(FlightState.TAKE_OFF)
            it.startTakeoff { error ->
                if (error == null) {
                    stateSubject.onNext(FlightState.FLYING)
                } else {
                    showError("start take off", error)
                    stateSubject.onNext(FlightState.NONE)
                }
            }
        }
    }

    override fun cancelTakeOff() {
        getDJIFlightController()?.let {
            stateSubject.onNext(FlightState.TAKE_OFF_CANCELING)
            it.cancelTakeoff { error ->
                if (error == null) {
                    stateSubject.onNext(FlightState.TAKE_OFF_CANCEL)
                } else {
                    showError("cancel take off", error)
                    stateSubject.onNext(FlightState.TAKE_OFF)
                }

            }
        }

    }

    override fun startLanding(){
        //val state = stateSubject.blockingFirst()
         getDJIFlightController()?.let {
             stateSubject.onNext(FlightState.LANDING_START)
             it.startLanding { error ->
                 if(error == null) {
                     stateSubject.onNext(FlightState.LANDING)
                 }else{
                     showError("start landing", error)
                     stateSubject.onNext(FlightState.FLYING)
                 }
             }
         }
    }

    override fun cancelLanding(){

        getDJIFlightController()?.let {
            stateSubject.onNext(FlightState.LANDING_CANCEL)
            it.cancelLanding {error->
                if(error == null){
                    stateSubject.onNext(FlightState.FLYING)
                }else {
                    showError("cancel landing", error)
                    stateSubject.onNext(FlightState.LANDING)
                }
            }
        }

    }

    override fun confirmLanding(){
        getDJIFlightController()?.let{
            stateSubject.onNext(FlightState.LANDING_CONFIRM_START)

            it.confirmLanding {error ->
                if(error == null){
                    stateSubject.onNext(FlightState.LANDING_FINISH)
                }else {
                    showError("confirm landing", error)
                    stateSubject.onNext(FlightState.LANDING_CONFIRM)
                }
            }
        }
    }

    //Home
    override fun startGoHome(){
        val controller = getDJIFlightController()
        if(controller !=null) {
            controller.startGoHome {error ->
                if(error == null){

                }else {
                    showError("start gohome", error)
                }
            }
        }else{
        }
    }

    override fun cancelGoHome(){
        val controller = getDJIFlightController()
        if(controller !=null) {
            controller.cancelGoHome {error ->
                if(error == null){

                }else {
                    showError("cancel gohome", error)
                }
            }
        }else{
        }
    }

    override fun setHomeLocation(){

    }

    override fun getHomeLocation(callback: CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>){
        getDJIFlightController()?.getHomeLocation(callback)
    }




    fun showError(msg:String,error: DJIError?){
        error?.let {
            val messg = "${msg} error:${error.description}"
            log(this, messg)
            errorSubject.onNext(messg)
        }

    }

    fun getDJIFlightController():FlightController?{
        var controller = flightController
        if(controller == null){
            DJISDKManager.getInstance().product?.let {
                var aircraft = it as Aircraft
                log(this,"get aircraft:$aircraft")
                aircraft?.let {
                    controller = aircraft.flightController
                    log(this,"get flight controller:$controller")

                }
            }
        }
        return controller
    }

}