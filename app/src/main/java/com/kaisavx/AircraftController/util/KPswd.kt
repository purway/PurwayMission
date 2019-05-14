package com.kaisavx.AircraftController.util

object KPswd {

    val baseKey = ArrayList<Byte>()
    val pKey = ArrayList<Byte>()

    val oKey = 0x77

    init {
        val key = ArrayList<Byte>()
        for (i in 0..127) {
            if ((0 <= i && i <= 47) ||
                    (58 <= i && i <= 64) ||
                    (91 <= i && i <= 94) ||
                    (123 <= i && i <= 127)) {
                key+=i.toByte()
            } else {
                pKey+=i.toByte()
            }
        }

        val primes = ArrayList<Int>()
        val iList = ArrayList<Int>()

        var num = 5

        primes.add(2)
        primes.add(3)
        iList.add(3)

        while(primes.size < 1024 && iList.size<32){
            var flag = true
            var n = 1
            while (primes[n] * primes[n] <= num) {
                if (num % primes[n] == 0) {
                    flag = false
                    break
                }
                n++
            }
            if (flag) {
                primes.add(num)
                if (num >= 64) {
                    var findFlag = false
                    val dif = num % 64
                    for (k in iList.indices) {
                        if (iList[k] == dif) {
                            findFlag = true
                            break
                        }
                    }
                    if (!findFlag) {
                        iList.add(dif)
                    }
                } else {
                    iList.add(num)
                }
            }
            num += 2
        }

        for(i in 0 .. 32){
            iList.add((iList[i]-1))
        }

        for(i in iList.indices){
            baseKey.add(key[iList[i]])
        }

    }

    fun encode( code:String):String{
        logMethod(this)
        val iList = ArrayList<Int>()
        for(c in code){
            for( i in pKey.indices){
                if(pKey[i] == c.toByte()){
                    iList+=i
                    break
                }
            }
        }
        log(this , "iList:$iList")

        for(i in iList.indices){
            if(i==0){
                iList[i]=(iList[i] xor oKey)%64
            }else{
                iList[i]=(iList[i] xor iList[i-1])%64
            }
            if(iList[i]<0){
                iList[i]+=64
            }
        }
        log(this , "gList:$iList")

        for(i in iList.indices){
            if(i == 0){
                iList[i]=(iList[i]+ oKey)%64
            }else{
                iList[i]=(iList[i]+iList[i-1])%64
            }
        }

        log(this , "eList:$iList")
        var encode =""
        for(i in iList.indices){
            encode += baseKey[iList[i]].toChar()
        }
        return encode
    }

    fun decode(code:String):String{
        logMethod(this)
        val iList = ArrayList<Int>()
        for(c in code){
            for( i in baseKey.indices){
                if(baseKey[i] == c.toByte()){
                    iList+=i
                    break
                }
            }
        }
        log(this , "iList:$iList")

        for(i in iList.indices.reversed()){
            if(i == 0){
                iList[i] = (iList[i]- oKey)%64
            }else{
                iList[i] = (iList[i]-iList[i-1])%64
            }
            if(iList[i]<0){
                iList[i]+=64
            }
        }
        log(this , "gList:$iList")

        for(i in iList.indices.reversed()){
            if(i == 0){
                iList[i] = (iList[i] xor oKey)%64
            }else{
                iList[i] = (iList[i] xor iList[i-1])%64
            }
            if(iList[i]<0){
                iList[i]+=64
            }
        }
        log(this , "dList:$iList")


        var decode=""

        for(i in iList.indices){
            decode += pKey[iList[i]].toChar()
        }
        //log(this ,"decode:$decode")
        return decode
    }
}