package com.kaisavx.AircraftController.util

import java.io.*
import java.net.ServerSocket
import java.net.Socket


class TCPLog(port:Int , filePath:String) {

    val TIMEOUT = 2000
    var isListen = true
    var port=0
    var filePath:String?=null

    val SST = ArrayList<ServerSocketThread>()

    init {
        this.port = port
        this.filePath = filePath
    }

    fun createServer() {
        Log.d(this, "createServer")
        try {

            val serverSocket = ServerSocket(port)
            while (isListen) {
                getSocket(serverSocket)?.let { SST.add(ServerSocketThread(it,filePath)) }
            }
            serverSocket.close();
        }catch(e:IOException){
            e.printStackTrace()
        }
    }

    fun destoryServer(){
        Log.d(this , "destoryServer")
        isListen = false
        SST.forEach{
            it.destory()
        }
        SST.clear()
    }

    private fun getSocket(serverSocket:ServerSocket): Socket?{
        try{
            return serverSocket.accept()
        }catch (e:Exception){
            e.printStackTrace()
            return null
        }
    }

    inner class ServerSocketThread internal constructor(socket:Socket,filePath:String?):Thread(){

        internal var socket:Socket?=null
        private var pw: PrintWriter?=null
        private var inputStream:InputStream?=null
        private var outputStream:OutputStream?=null
        private var fileStream:FileInputStream?=null
        private var ip:String ?=null
        private var isRun = true
        private var curTime:Long = 0
        private var filePath:String?=null

        init {
            this.socket = socket
            this.filePath = filePath
            ip = socket.inetAddress.toString()

            log(this , "has new connect $ip $port")
            try {
                outputStream = socket.getOutputStream()
                inputStream = socket.getInputStream()
                fileStream = FileInputStream(File(filePath))
                pw = PrintWriter(outputStream , true)


                fileStream!!.available().let {
                    fileStream?.channel?.position(it.toLong())
                }

                start()
            }catch (e:Exception){
                e.printStackTrace()
                Log.d(this,e.toString())
            }
        }

        override fun run() {
            val buff = ByteArray(4096)
            var rcvMsg:String
            var rcvLen = 0
            var msgLen = 0
            log(this, "run")
            curTime = System.currentTimeMillis()
            while (isRun && !socket!!.isClosed() && !socket!!.isInputShutdown() && System.currentTimeMillis()-curTime <= TIMEOUT) {

                rcvLen = 0
                msgLen = 0
                try {
                    rcvLen = inputStream!!.read(buff)
                    if (rcvLen != -1) {
                        rcvMsg = String(buff, 0 , rcvLen)
                        if (rcvMsg == "QuitServer") {
                            isRun = false
                        }else if(rcvMsg == "HeartBeat"){
                            //send("HeartBeat")
                            curTime = System.currentTimeMillis()
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                try {
                    msgLen = fileStream!!.read(buff)
                    if(msgLen > 0){
                        send(String(buff,0,msgLen))
                    }
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
            log(this,"jump out while")
            try {
                pw?.close()
                outputStream?.close()
                inputStream?.close()
                fileStream?.close()
                socket?.close()

                log(this , "$ip $port close")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun send(msg:String){
            pw?.print(msg)
            pw?.flush()
        }

        fun destory(){
            Log.d(this , "$ip $port destory")
            isRun = false
        }
    }
}