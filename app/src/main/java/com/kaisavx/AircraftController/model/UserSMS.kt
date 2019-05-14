package com.kaisavx.AircraftController.model

data class UserSMS(
        val type:Int,
        val prefixer: String,
                   val phone: String,
                   val code: String? = null) {

    enum class TYPE(val value:Int){
        REGISTER(0),
        LOGIN(1),
        ALTER(2),
    }

}