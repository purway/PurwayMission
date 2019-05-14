package com.kaisavx.AircraftController.model

import com.kaisavx.AircraftController.service.Mission

data class RequestWithUserId(val user: User) {
    var aircraft: AircraftData? = null

    var wayPointMission: Mission? = null
    var wayPointMissionId: String? = null

    var flyRecord: FlyRecord? = null
    var flyRecordId: String? = null

    var createdTime: Long? = null
    var skip: Int? = null
    var limit: Int? = null
    var aircraftStatusRecord: AircraftStatusRecord? = null

}