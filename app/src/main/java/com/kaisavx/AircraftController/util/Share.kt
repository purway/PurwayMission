package com.kaisavx.AircraftController.util

import android.content.Context
import android.content.SharedPreferences

class Share {
    companion object {

        val SHARE_NAME ="SHARE"

        val LOW_BATTERY_BACK = "LOW_BATTERY_BACK"
        val CONNECTION_FAIL_BACK = "CONNECTION_FAIL_BACK"
        val BACK_HEIGHT = "BACK_HEIGHT"

        fun getPreferences(context:Context):SharedPreferences{
            return context.getSharedPreferences(SHARE_NAME , Context.MODE_PRIVATE)
        }

        fun getLowBatteryBack(context: Context):Int{
            return getPreferences(context).getInt(LOW_BATTERY_BACK ,20)
        }

        fun setLowBatteryBack(context: Context , lowBattery:Int){
            val editor = getPreferences(context).edit()
            editor.putInt(LOW_BATTERY_BACK , lowBattery)
            editor.apply()
        }

        fun getConnectionFailBack(context:Context):Boolean{
            return getPreferences(context).getBoolean(CONNECTION_FAIL_BACK , true)
        }

        fun setConnectionFailBack(context:Context , connectionFail:Boolean){
            val editor = getPreferences(context).edit()
            editor.putBoolean(CONNECTION_FAIL_BACK , connectionFail)
            editor.apply()
        }

        fun getBackHeight(context: Context):Int{
            return getPreferences(context).getInt(BACK_HEIGHT , 100)
        }

        fun setBackHeight(context: Context , backHeight:Int){
            val editor = getPreferences(context).edit()
            editor.putInt(BACK_HEIGHT , backHeight)
            editor.apply()
        }
    }
}