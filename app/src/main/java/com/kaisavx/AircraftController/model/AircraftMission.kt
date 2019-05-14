package com.kaisavx.AircraftController.model

data class AircraftMission(val userId:String , val type:Int){
    var id:String?=null
    var param1:String?=null
    var param2:String?=null
    var param3:String?=null
    var param4:String?=null
    var createdTime:Long?=null


    enum class TYPE(val value:Int){
        START(1),
        PAUSE(2),
        RESUME(3),
        STOP(4),

        RTH(5),
        CANCEL_RTH(6),

        TAKE_OFF(7),
        CANCEL_TAKE_OFF(8),

        LAND(9),
        CANCEL_LAND(10),
    }
}