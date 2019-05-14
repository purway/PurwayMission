package com.kaisavx.AircraftController.model


data class AircraftStatusRecord(val userId:String , val aircraftId:String , val status:Int){
    var messsage:String?=null
    var createdTime:Long?=null

    enum class STATUS(val value:Int){
        NONE(0),
        UPLOADING(1),
        WAIT_HOME_SET(2),
        WAIT_INIT(3),
        START(4),
        EXECUTING(5),
        PAUSED(6),
        RESUMING(7),
        DOWNLOAD_PICTURES(8),
        UPLOAD_PICTURES(9),
        FINISHED(10),
        STOP(11),

        CONNECTED(20),
        DISCONNECTED(21),
    }
}