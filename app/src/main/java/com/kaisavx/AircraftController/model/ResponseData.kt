package com.kaisavx.AircraftController.model

import com.kaisavx.AircraftController.service.Mission

data class ResponseData(val retCode: Int, val retMsg: String) {
    var user: User? = null
    var aircraft: AircraftData? = null
    var product: Product? = null

    var aircraftMission: AircraftMission? = null
    var aircraftMissionList: List<AircraftMission>? = null

    var flyRecord: FlyRecord? = null
    var flyRecordList: List<FlyRecord>? = null

    var total: Int = 0

    var wayPointMissionDetail: Mission? = null
    var wayPointMissionList: List<Mission>? = null

    var url: String? = null
}