package com.kaisavx.AircraftController.device

import com.kaisavx.AircraftController.App.Companion.context
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.module.dji.OnBoardCommand
import com.kaisavx.AircraftController.module.dji.OnBoardReceive

open class Device(val no:String,
                  val firmware:String,
                  val type: Byte,
                  val sendFunction: (OnBoardCommand) -> Unit){
    enum class DeviceType(val value:Byte){
        NONE(0x00),
        AIR_DETECTOR(0x01),
        WATER_SAMPLER(0x02),
        FLOW_DETECTOR(0x03),
    }
    enum class BaseCommand(val value:Byte){
        NO(0x01),
        FIRMWARE(0x02),
    }

    open fun processReceive(receive: OnBoardReceive){

    }

    open fun destory(){

    }

    open fun sendCommand(command:OnBoardCommand){
        sendFunction.invoke(command)
    }

    fun getDisplayName():String{
        when(type){
            DeviceType.AIR_DETECTOR.value->{
                return context.getString(R.string.device_type_air_detector)
            }

            DeviceType.WATER_SAMPLER.value->{
                return context.getString(R.string.device_type_water_sampler)
            }
            DeviceType.FLOW_DETECTOR.value->{
                return context.getString(R.string.device_type_flow_detector)
            }
            else->{
                return context.getString(R.string.device_type_none)
            }
        }
    }
}