package com.kaisavx.AircraftController.model

import dji.common.flightcontroller.*
import dji.common.model.LocationCoordinate2D

class FlightcontrollerState{
    var orientationMode: FlightOrientationMode? = null
    var attitude: Attitude? = null
    var goHomeExecutionState: GoHomeExecutionState? = null
    var gpsSignalLevel: GPSSignalLevel? = null
    var homeLocation: LocationCoordinate2D? = null
    var aircraftLocation: LocationCoordinate3D? = null
    var flightMode: FlightMode? = null
    var goHomeAssessment: GoHomeAssessment? = null
    var isGoingHome: Boolean = false
    var isMultipModeOpen: Boolean = false
    var hasReachedMaxFlightHeight: Boolean = false
    var hasReachedMaxFlightRadius: Boolean = false
    var isHomeLocationSet: Boolean = false
    var isFailsafeEnabled: Boolean = false
    var areMotorsOn: Boolean = false
    var isUltrasonicBeingUsed: Boolean = false
    var isIMUPreheating: Boolean = false
    var isVisionPositioningSensorBeingUsed: Boolean = false
    var doesUltrasonicHaveError: Boolean = false
    var isFlying: Boolean = false
    var goHomeHeight: Int = 0
    var aircraftHeadDirection: Int = 0
    var ultrasonicHeightInMeters: Float = 0F
    var satelliteCount: Int = 0
    var flightModeString: String? = null
    var batteryThresholdBehavior: BatteryThresholdBehavior? = null
    var velocityX: Float = 0F
    var velocityY: Float = 0F
    var velocityZ: Float = 0F
    var flightTimeInSeconds: Int = 0
    var isLowerThanBatteryWarningThreshold: Boolean = false
    var isLowerThanSeriousBatteryWarningThreshold: Boolean = false
    var flightWindWarning: FlightWindWarning? = null
    var islandingConfirmationNeeded: Boolean = false

    constructor(state: FlightControllerState){
        orientationMode = state.orientationMode

        attitude = state.attitude


        goHomeExecutionState = state.goHomeExecutionState
        gpsSignalLevel = state.gpsSignalLevel

        homeLocation = state.homeLocation
        aircraftLocation = state.aircraftLocation
        flightMode = state.flightMode
        goHomeAssessment = state.goHomeAssessment
        isGoingHome = state.isGoingHome
        isMultipModeOpen = state.isMultipleModeOpen
        hasReachedMaxFlightHeight = state.hasReachedMaxFlightHeight()
        hasReachedMaxFlightRadius = state.hasReachedMaxFlightRadius()
        isHomeLocationSet = state.isHomeLocationSet
        isFailsafeEnabled = state.isFailsafeEnabled
        areMotorsOn = state.areMotorsOn()
        isUltrasonicBeingUsed = state.isUltrasonicBeingUsed
        isIMUPreheating = state.isIMUPreheating
        isVisionPositioningSensorBeingUsed = state.isVisionPositioningSensorBeingUsed
        doesUltrasonicHaveError = state.doesUltrasonicHaveError()
        isFlying = state.isFlying
        goHomeHeight = state.goHomeHeight
        aircraftHeadDirection = state.aircraftHeadDirection
        ultrasonicHeightInMeters = state.ultrasonicHeightInMeters
        satelliteCount = state.satelliteCount
        flightModeString = state.flightModeString
        batteryThresholdBehavior = state.batteryThresholdBehavior
        velocityX = state.velocityX
        velocityY = state.velocityY
        velocityZ = state.velocityZ
        flightTimeInSeconds = state.flightTimeInSeconds
        isLowerThanBatteryWarningThreshold = state.isLowerThanBatteryWarningThreshold
        isLowerThanSeriousBatteryWarningThreshold = state.isLowerThanSeriousBatteryWarningThreshold
        flightWindWarning = state.flightWindWarning
        islandingConfirmationNeeded = state.isLandingConfirmationNeeded
    }
}