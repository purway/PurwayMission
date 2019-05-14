package com.kaisavx.AircraftController.util

import android.content.Context
import android.content.SharedPreferences
import com.kaisavx.AircraftController.model.User

class Share {
    companion object {

        val SHARE_NAME = "SHARE"

        val LOW_BATTERY_BACK = "LOW_BATTERY_BACK"
        val CONNECTION_FAIL_BACK = "CONNECTION_FAIL_BACK"
        val BACK_HEIGHT = "BACK_HEIGHT"

        val USER_ID = "USER_ID"
        val USER_NAME = "USER_NAME"
        val USER_PASSWORD = "USER_PASSWORD"
        val USER_DJI_ACCOUNT = "USER_DJI_ACCOUNT"
        val USER_COMPANY = "USER_COMPANY"
        val USER_PRODUCT_ID = "USER_PRODUCT_ID"
        val USER_PERMISSION ="USER_PERMISSION"

        val IS_VIDEO_DATA ="IS_VIDEO_DATA"
        val IS_MEDIA_UPLOAD ="IS_MEDIA_UPLOAD"
        val IS_WAYPOINT_SYNC = "IS_WAYPOINT_SYNC"
        val IS_FLYDATA_SYNC = "IS_FLYDATA_SYNC"
        val IS_REALTIME_CONTROL ="IS_REALTIME_CONTROL"

        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARE_NAME, Context.MODE_PRIVATE)
        }

        fun setUser(context: Context, user: User?) {
            val editor = getPreferences(context).edit()
            editor.putString(USER_ID, user?.id)
            editor.putString(USER_DJI_ACCOUNT , user?.djiAccount)
            editor.putString(USER_NAME, user?.name)
            editor.putString(USER_COMPANY, user?.company)
            editor.putString(USER_PRODUCT_ID, user?.productId)

            editor.apply()
        }

        fun getUser(context: Context): User? {
            val p = getPreferences(context)
            val id = p.getString(USER_ID, null)
            val account = p.getString(USER_DJI_ACCOUNT, null)

            val productId = p.getString(USER_PRODUCT_ID, null)
            val name = p.getString(USER_NAME ,null)
            val company = p.getString(USER_COMPANY , null)
            if(id == null){
                return null
            }
            val user = User(id, account)
            user.name = name
            user.company = company
            user.productId = productId

            return user
        }

        fun setUserId(context: Context, userId: String?) {
            val editor = getPreferences(context).edit()
            editor.putString(USER_ID, userId)
            editor.apply()
        }

        fun getUserId(context: Context): String? {
            return getPreferences(context).getString(USER_ID, null)
        }

        fun setUserPassword(context: Context, userPassword: String?) {
            val editor = getPreferences(context).edit()
            /*
            val password = userPassword?.let {
                KPswd.encode(it)
            }
            */
            editor.putString(USER_PASSWORD, userPassword)
            editor.apply()
        }

        fun getUserPassword(context: Context): String? {
            val password = getPreferences(context).getString(USER_PASSWORD, null)
            return password
        }

        fun setUserName(context: Context, userName: String?) {
            val editor = getPreferences(context).edit()
            editor.putString(USER_NAME, userName)
            editor.apply()
        }

        fun getUserName(context: Context): String? {
            return getPreferences(context).getString(USER_NAME, null)
        }

        fun setUserAccount(context: Context,userAccount:String?){
            val editor = getPreferences(context).edit()
            editor.putString(USER_DJI_ACCOUNT, userAccount)
            editor.apply()
        }

        fun getUserAccount(context:Context):String?{
            return getPreferences(context).getString(USER_DJI_ACCOUNT,null)
        }

        fun setUserCompany(context:Context,company:String?){
            val editor = getPreferences(context).edit()
            editor.putString(USER_COMPANY, company)
            editor.apply()
        }

        fun getUserCompany(context: Context): String? {
            return getPreferences(context).getString(USER_COMPANY, null)
        }

        fun setUserProductId(context: Context , productId:String?){
            val editor = getPreferences(context).edit()
            editor.putString(USER_PRODUCT_ID, productId)
            editor.apply()
        }

        fun getUserProductId(context: Context):String?{
            return getPreferences(context).getString(USER_PRODUCT_ID , null)
        }

        fun setPermission(context:Context , permission:Int){
            val editor = getPreferences(context).edit()
            editor.putInt(USER_PERMISSION , permission)
            editor.apply()
        }

        fun getPermission(context: Context):Int{
            return getPreferences(context).getInt(USER_PERMISSION,0)
        }

        //UserOption
        fun setIsVideoData(context:Context , isVideoData:Boolean){
            val editor = getPreferences(context).edit()
            editor.putBoolean(IS_VIDEO_DATA,isVideoData)
            editor.apply()
        }

        fun getIsVideoData(context: Context):Boolean{
            return getPreferences(context).getBoolean(IS_VIDEO_DATA,true)
        }

        fun setIsMediaUpload(context: Context , isMediaUpload:Boolean){
            val editor = getPreferences(context).edit()
            editor.putBoolean(IS_MEDIA_UPLOAD,isMediaUpload)
            editor.apply()
        }

        fun getIsMediaUpload(context: Context):Boolean{
            return getPreferences(context).getBoolean(IS_MEDIA_UPLOAD,true)
        }

        fun setIsWaypointSync(context: Context , isWaypointSync:Boolean){
            val editor = getPreferences(context).edit()
            editor.putBoolean(IS_WAYPOINT_SYNC , isWaypointSync)
            editor.apply()
        }

        fun getIsWaypointSync(context: Context):Boolean{
            return getPreferences(context).getBoolean(IS_WAYPOINT_SYNC ,true)
        }

        fun setIsFlydataSync(context: Context, isFlydataSync:Boolean){
            val editor = getPreferences(context).edit()
            editor.putBoolean(IS_FLYDATA_SYNC , isFlydataSync)
            editor.apply()
        }

        fun getIsFlydataSync(context: Context):Boolean{
            return getPreferences(context).getBoolean(IS_FLYDATA_SYNC , true)
        }

        fun setIsRealtimeControl(context: Context , isRealtimeControl:Boolean){
            val editor = getPreferences(context).edit()
            editor.putBoolean(IS_REALTIME_CONTROL , isRealtimeControl)
            editor.apply()
        }

        fun getIsRealtimeControl(context: Context):Boolean{
            return getPreferences(context).getBoolean(IS_REALTIME_CONTROL , true)
        }

        //aircraftOption

        fun getLowBatteryBack(context: Context): Int {
            return getPreferences(context).getInt(LOW_BATTERY_BACK, 20)
        }

        fun setLowBatteryBack(context: Context, lowBattery: Int) {
            val editor = getPreferences(context).edit()
            editor.putInt(LOW_BATTERY_BACK, lowBattery)
            editor.apply()
        }

        fun getConnectionFailBack(context: Context): Boolean {
            return getPreferences(context).getBoolean(CONNECTION_FAIL_BACK, true)
        }

        fun setConnectionFailBack(context: Context, connectionFail: Boolean) {
            val editor = getPreferences(context).edit()
            editor.putBoolean(CONNECTION_FAIL_BACK, connectionFail)
            editor.apply()
        }

        fun getBackHeight(context: Context): Int {
            return getPreferences(context).getInt(BACK_HEIGHT, 100)
        }

        fun setBackHeight(context: Context, backHeight: Int) {
            val editor = getPreferences(context).edit()
            editor.putInt(BACK_HEIGHT, backHeight)
            editor.apply()
        }


    }
}