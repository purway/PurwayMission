package com.kaisavx.AircraftController.util

import java.util.regex.Pattern

object RegexUtil{

    val ACCOUNT_PATTERN = Pattern.compile("^\\w{4,24}\$")
    val PASSWORD_PATTERN = Pattern.compile("^\\w{8,20}\$")
    val PREFIXER_PATTERN = Pattern.compile("^\\d{1,3}\$")
    val PHONE_PATTERN = Pattern.compile("^1[3|4|5|7|8][0-9]{9}\$")

    fun isAccount(str:String):Boolean = ACCOUNT_PATTERN.matcher(str).matches()
    fun isPassword(str:String):Boolean = PASSWORD_PATTERN.matcher(str).matches()
    fun isPrefixer(str: String):Boolean = PREFIXER_PATTERN.matcher(str).matches()
    fun isPhone(str:String):Boolean = PHONE_PATTERN.matcher(str).matches()
}