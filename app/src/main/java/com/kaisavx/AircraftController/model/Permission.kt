package com.kaisavx.AircraftController.model

data class Permission(val permission:Int){

    enum class BIT(val value:Int){
        LIVE(0),
        VIDEO_DATA(1),
        MEDIA_UPLOAD(2),
        WAYPOINT_SYNC(3),
        FLYDATA_SYNC(4),
        REALTIME_CONTROL(5),
        IS_GAS(6),
        IS_WATER(7)
    }

    private fun isBitCheck(bitCount:Int):Boolean{
        val p = permission.shr(bitCount).and(1)
        if(p==1){
            return true
        }else{
            return false
        }
    }

    fun isLive():Boolean{
        return isBitCheck(BIT.LIVE.value)
    }

    fun isVideoData():Boolean{
        return isBitCheck(BIT.VIDEO_DATA.value)
    }

    fun isMediaUpload():Boolean{
        return isBitCheck(BIT.MEDIA_UPLOAD.value)
    }

    fun isWaypointSync():Boolean{
        return isBitCheck(BIT.WAYPOINT_SYNC.value)
    }

    fun isFlydataSync():Boolean{
        return isBitCheck(BIT.WAYPOINT_SYNC.value)
    }

    fun isRealtimeControl():Boolean{
        return isBitCheck(BIT.REALTIME_CONTROL.value)
    }

    fun isGas():Boolean{
        return isBitCheck(BIT.IS_GAS.value)
    }

    fun isWater():Boolean{
        return isBitCheck(BIT.IS_WATER.value)
    }

}