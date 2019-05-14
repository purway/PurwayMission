package com.kaisavx.AircraftController.model

data class FlyPoint(val latitude:Double? ,
                    val longitude:Double? ,
                    val altitude:Float,
                    val velocityX:Float,
                    val velocityY:Float,
                    val velocityZ:Float,
                    val time:Long){
}