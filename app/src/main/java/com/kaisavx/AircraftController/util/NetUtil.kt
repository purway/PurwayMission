package com.kaisavx.AircraftController.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object NetUtil{

    fun ping(ip:String):Boolean{
        var result:String?=null
        try {

            val p = Runtime.getRuntime().exec("ping -c 3 -w 10 ${ip}")
            val input = p .inputStream
            val reader = BufferedReader(InputStreamReader(input))
            var content:String? =null
            val buffer = StringBuffer()
            while(true){
                content = reader.readLine()
                content?:break
                buffer.append(content)
            }
            log(this , "result content:${buffer}")
            val status = p.waitFor()
            if(status == 0){
                result ="success"
                return true
            }else{
                result = "failed"
            }
        }catch (e:IOException){
            e.printStackTrace()
        }catch (e:InterruptedException){
            e.printStackTrace()
        }finally {
            log(this ,"sesult:${result}")
        }

        return false
    }
}