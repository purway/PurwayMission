package com.kaisavx.AircraftController.device.air

data class AirDataCollection(
        val timestamp:Long,
        val tempInside:Short,
        val humInside:Short,
        val tempOutside:Short,
        val humOutside:Short,

        val tempPressure:Int,
        val pressure:Int,

        val PM1p0:Short,
        val PM2p5:Short,
        val PM10:Short,
        val NUM0p3:Short,
        val NUM0p5:Short,
        val NUM1p0:Short,
        val NUM2p5:Short,
        val NUM5p0:Short,
        val NUM10:Short,
        val voltageList:List<Short>
){
    override fun toString(): String {
        return "[TI:$tempInside HI:$humInside TO:$tempOutside HO:$humOutside] "+
                "[TP:$tempPressure P:$pressure] "+
                "[PM1.0:$PM1p0 PM2.5:$PM2p5 PM10:$PM10 NUM0.3:$NUM0p3 NUM0.5:$NUM0p5 NUM1.0:$NUM1p0 NUM2.5:$NUM2p5 NUM5.0:$NUM5p0 NUM10:$NUM10] "+
                "[vList:$voltageList]"
    }
}