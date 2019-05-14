package com.kaisavx.AircraftController.model

import dji.common.battery.BatteryCellVoltageLevel
import dji.common.battery.BatteryState
import dji.common.battery.ConnectionState

class BatteryState{
    var cellVoltageLevel: BatteryCellVoltageLevel? = null
    var fullChargeCapacity: Int = 0
    var chargeRemaining: Int = 0
    var voltage: Int = 0
    var current: Int = 0
    var lifetimeRemaining: Int = 0
    var chargeRemainingInPercent: Int = 0
    var temperature: Float = 0.toFloat()
    var numberOfDischarges: Int = 0
    var isBeingCharged: Boolean = false
    var isSingleBattery: Boolean = false
    var isBigBattery: Boolean = false
    var connectionState: ConnectionState? = null

    constructor(state:BatteryState){
        cellVoltageLevel = state.cellVoltageLevel
        fullChargeCapacity = state.fullChargeCapacity
        chargeRemaining = state.chargeRemaining
        voltage = state.voltage
        current = state.current
        lifetimeRemaining = state.lifetimeRemaining
        chargeRemainingInPercent = state.chargeRemainingInPercent
        temperature = state.temperature
        numberOfDischarges = state.numberOfDischarges
        isBeingCharged = state.isBeingCharged
        isSingleBattery = state.isInSingleBatteryMode
        isBigBattery  = state.isBigBattery
        connectionState = state.connectionState
    }
}