package com.kaisavx.AircraftController.util

import dji.common.error.DJIError
import dji.common.error.DJIMissionError

object DJIErrorTranslation{
    fun translate(error: DJIError):String{
        var msg=""
        when(error){
            DJIMissionError.ALTITUDE_TOO_HIGH -> msg="设置的高度过高或没有登录dji帐号"
            DJIMissionError.ALTITUDE_TOO_LOW -> msg = "高度过低"

            DJIMissionError.HOME_POINT_DIRECTION_UNKNOWN -> msg = "返航位置无法识别"
            DJIMissionError.HOME_POINT_NOT_RECORDED -> msg = "返航位置没获取"
            DJIMissionError.MISSION_PARAMETERS_INVALID -> msg = "任务参数不合法"

            DJIError.COMMON_EXECUTION_FAILED -> msg ="任务无法执行"

            else -> msg =error.description
        }
        return msg
    }
}