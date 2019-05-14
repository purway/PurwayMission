package com.kaisavx.AircraftController.model

data class UserRegisterSMS(val prefixer: String,
                           val phone: String,
                           val code: String? = null) {

}