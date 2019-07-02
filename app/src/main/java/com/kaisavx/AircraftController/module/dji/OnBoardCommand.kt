package com.kaisavx.AircraftController.module.dji

import java.nio.ByteBuffer

var actionNumber = 0

data class OnBoardCommand(val number:Int,val command:Byte,val argument:Byte?=null){
    fun toByte(): ByteArray {
        val size = if (argument == null) 5 else 6
        val buffer = ByteBuffer.allocate(size)
                .putInt(number)
                .put(command)
        argument?.let {
            buffer.put(it)
        }
        return buffer.array()
    }
}

data class OnBoardReceive(val recvData:ByteArray){
    val number:Int
    val command:Byte
    val dataArray:ByteArray
    init {
        var num = 0
        for ( i in 0 until 4){
            val shift = (4-1-i)*8
            num += recvData[i].toUInt().shl(shift).toInt()
        }
        number = num
        command = recvData[4]
        dataArray = recvData.copyOfRange(5 , recvData.size)
    }
}

