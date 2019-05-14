package com.kaisavx.AircraftController.model

import dji.common.battery.BatteryState
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.virtualstick.FlightControlData

data class RealMessage(val reqCode:Int , val num:Int){

    enum class REQ_CODE(val value:Int){
        NONE(0),
        REQUEST_CONNECT(1),
        CONNECT(2),
        REQUEST_DISCONNECT(3),
        DISCONNECT(4),
        HEART_BEAT(5),

        STATUS_REPORT(6),

        VIRTUAL_ENABLE(10),
        VIRTUAL_DISABLE(11),
        VIRTUAL_CONTROL(12),

        START_TAKE_OFF(13),
        CANCEL_TAKE_OFF(14),
        START_LANDING(15),
        CANCEL_LANDING(16),
        CONFIRM_LADING(17),
        START_GO_HOME(18),
        CANECL_GO_HOME(19),
        START_MISSION(20),
        PAUSE_MISSION(21),
        RESUME_MISSION(22),
        STOP_MISSION(23),

    }

    enum class RET_CODE(val value:Int){
        SUCCESS(0),
        UNKNOW(-1),
        USER_HAVE_BEEN_CONNECTED(-2),
        MUST_REQUEST_CONNECT_FIRST(-3),
        USER_IS_DISCONNECTED(-4),
        MUST_REQUEST_DISCONNECT_FIRST(-5),

        USER_CANCEL_REQUEST(-6),

        AIRCRAFT_NOT_CONNECT(-11),

        WAYPOINT_MISSION_ID_IS_NULL(-21),

        OTHER_ERROR(-100),
    }

    enum class USER_CONNECT_STATUS(val value:Int){
        CONNECT(1),
        DISCONNECT(2),
    }

    var retCode:Int?=null
    var retMsg:String?=null
    var wayPointMissionId:String?=null
    var flightControlData: FlightControlData?=null
    var batteryState:BatteryState?=null
    var flightControllerState: FlightControllerState?=null
    var status:Int?=null

}