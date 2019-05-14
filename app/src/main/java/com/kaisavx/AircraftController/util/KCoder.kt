package com.kaisavx.AircraftController.util

import java.util.*
import kotlin.experimental.xor

class KCoder(obviouslyKey: Int, hKey: Int) {

    companion object {
        val BASE_KEY_LEN = 256
        val OKEY_LEN = 125
    }

    val baseKey =ArrayList<Byte>()
    val oKey= ArrayList<Byte>()

    init {
        makeBaseKey()
        var str=""

        for(i in baseKey.indices){
            if(i%16 == 0){
                str+="\n"
            }
            str+=" ${Integer.toHexString(baseKey[i].toInt() and 0xFF)}"

        }
        log(this , "baseKey:$str")

        shiftBaseKey(hKey)

        makeOkeys(obviouslyKey)

    }

    private fun makeBaseKey() {
        val primes = ArrayList<Byte>()
        var flag = true
        var findFlag: Boolean
        var dif: Int

        primes.add(2.toByte())
        primes.add(3.toByte())

        baseKey.add(3.toByte())
        var i = 5
        while (primes.size < 1024 && baseKey.size < BASE_KEY_LEN / 2) {
            flag = true
            var n = 1
            while ((primes[n].toInt() and 0xFF) * (primes[n].toInt() and 0xFF) <= i) {
                if (i % (primes[n].toInt() and 0xFF) == 0) {
                    flag = false
                    break
                }
                n++
            }
            if (flag) {
                primes.add(i.toByte())
                if (i >= 255) {
                    findFlag = false
                    dif = i % 256
                    for (k in baseKey.indices) {
                        if ((baseKey[k].toInt()and 0xFF) == dif) {
                            findFlag = true
                            break
                        }
                    }
                    if (!findFlag) {
                        baseKey.add(dif.toByte())
                    }
                } else {
                    baseKey.add(i.toByte())
                }
            }
            i += 2
        }


        for (i in 0 until BASE_KEY_LEN / 2) {
            baseKey.add((baseKey[i] - 1).toByte())
        }
    }

    internal fun shiftColumns(sKey: Int) {
        val datas = ArrayList<Byte>()
        for (r in 0..15) {
            datas.clear()
            for (c in 0..15) {
                datas.add(baseKey[16 * r + (c + sKey + r) % 16])
            }
            for (c in 0..15) {
                baseKey[16 * r + c] = datas[c]
            }
        }
    }

    internal fun shiftRows(sKey: Int) {
        val datas = ArrayList<Byte>()
        for (c in 0..15) {
            datas.clear()
            for (r in 0..15) {
                datas.add(baseKey[16 * ((r + sKey + c) % 16) + c])
            }
            for (r in 0..15) {
                baseKey[16 * r + c] = datas[r]
            }
        }
    }

    internal fun shiftBaseKey(hideKey: Int) {
        var sKey: Int
        sKey = hideKey shr 4 and 0xF
        shiftColumns(sKey)

        sKey = hideKey and 0xF
        shiftRows(sKey)

    }

    internal fun makeOkeys(obviouslyKey: Int) {
        var key = 0
        var k = 0
        var i = 0
        var j = 0
        while (i < OKEY_LEN) {
            while (j < BASE_KEY_LEN) {
                j += 1
                key = baseKey[BASE_KEY_LEN - j].toInt() and 0xFF xor obviouslyKey
                if (key != 0xFF && key >= 0x80 && key < 0xFF) {
                    k = baseKey[key and 0xF shl 4 or (key shr 4 and 0xF)].toInt() and 0xFF
                    if (k != 0xFF && k != 0x00) {
                        break
                    }
                }
            }
            oKey.add(key.toByte())
            i++
        }
    }

    fun encode(datas: ArrayList<Byte> , index:Int = 0) {
        val k = oKey[index].toInt() and 0xFF
        val key = baseKey[k and 0xF shl 4 or (k shr 4 and 0xF)]
        for (i in 0 until datas.size ) {
            if(i == 0){
                datas[i] = ((datas[i].toInt() xor k ) and 0xFF).toByte()
            }else {
                datas[i] = ((datas[i] xor datas[i - 1]).toInt() and 0xFF).toByte()
            }
        }

        for (i in 0 until datas.size ) {
            if (i == 0) {
                datas[i] = ((datas[i] + key)and 0xFF).toByte()
            } else {
                datas[i] = ((datas[i] + datas[i - 1]) and 0xFF).toByte()
            }
        }
    }

    fun decode(datas: ArrayList<Byte> , index:Int = 0) {
        val k = oKey[index].toInt() and 0xFF
        val key = baseKey[k and 0xF shl 4 or (k shr 4 and 0xF)]

        for (i in datas.indices.reversed()) {
            if (i == 0) {
                datas[i] = ((datas[i] - key) and 0xFF).toByte()
            } else {
                datas[i] = ((datas[i] - datas[i - 1]) and 0xFF).toByte()
            }
        }

        for (i in datas.indices.reversed()) {
            if (i == 0) {
                datas[i] = ((datas[i].toInt() xor k) and 0xFF).toByte()
            }else{
                datas[i] = ((datas[i] xor datas[i - 1]).toInt() and 0xFF).toByte()
            }
        }

    }


}