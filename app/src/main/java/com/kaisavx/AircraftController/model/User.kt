package com.kaisavx.AircraftController.model

data class User(val id:String?,
                val djiAccount:String?=null,
                var password:String?=null
                ){
    var name:String?=null
    var company:String?=null
    var productId:String?=null
}