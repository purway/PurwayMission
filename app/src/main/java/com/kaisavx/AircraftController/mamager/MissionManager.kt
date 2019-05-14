package com.kaisavx.AircraftController.mamager

import com.kaisavx.AircraftController.model.AircraftMission
import java.util.*

class MissionManager{
    private val missionQueue = LinkedList<AircraftMission>()

    inner class MissionProcessThread:Thread(){
        override fun run() {
            while(!interrupted()){
                val size = missionQueue.size

                if(size<=0)continue


            }
        }
    }

    fun addAircraftMission(aircraftMission: AircraftMission){
        synchronized(missionQueue){
            missionQueue.offer(aircraftMission)
        }
    }
}