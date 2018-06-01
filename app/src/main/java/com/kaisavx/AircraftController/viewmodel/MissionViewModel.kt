package com.kaisavx.AircraftController.viewmodel

import android.app.AlertDialog
import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.controller.MapMissionController
import com.kaisavx.AircraftController.controller.WayHolder
import com.kaisavx.AircraftController.service.Mission
import com.kaisavx.AircraftController.service.MissionService
import com.kaisavx.AircraftController.service.MissionState
import com.kaisavx.AircraftController.util.NullableObject
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import com.kaisavx.AircraftController.view.MissionPanel
import dji.common.error.DJIError
import dji.common.mission.waypoint.Waypoint
import dji.common.mission.waypoint.WaypointMissionFinishedAction
import dji.common.mission.waypoint.WaypointTurnMode
import dji.common.util.CommonCallbacks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_mission.*
import java.util.*

/**
 * Created by windless on 2017/12/21.
 */
class MissionViewModel(
        private val context: Context,
        private val missionPanel: MissionPanel,
        private val missionStatePanel: View,
        private val missionStateText: TextView,
        private val missionStopButton: Button,
        private val mapView: TextureMapView) {

    private val minSpeed = 1
    private val minSpacing = 10
    private val minAltitude = 30
    private val minShootTimeInterval = 2
    private val minGimbalPitch = -90

    private val mapMissionController by lazy { MapMissionController(mapView.map, context) }
    private lateinit var missionService: MissionService

    private val binder = CompositeDisposable()

    private var wayHolder:WayHolder?=null

    val isEditingSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    fun bindMissionService(missionService: MissionService) {
        logMethod(this)
        this.missionService = missionService


        val currentMission = missionService.currentMissionSubject

        val missionState = missionService.operator.state.distinctUntilChanged()

        binder.add(missionState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    log(this , "binder state:$it")
                    setMissionState(it)
                })

        val isMissionStopped: Observable<Boolean> = Observable.combineLatest(
                currentMission, missionState,
                BiFunction { mission, state ->
                    mission.value != null && state == MissionState.None
                }
        )
        binder.add(isMissionStopped
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it) {
                        setEditing(true)
                    }
                })

        binder.add(currentMission
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleCurrentMissionChanged(it, missionService)
                })
        binder.add( missionService.missionListSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    missionPanel.setMissions(it)
                })

        missionPanel.onSelectMission = { _, mission ->
            missionService.setCurrentMission(mission)
        }

        missionPanel.onCreateMission = { _, type ->
            log(this , "onCreateMission")
            val missionType = if (type == MapMissionController.ControlType.Area) {
                0
            } else {
                1
            }
            val mapCenter = getMapCenterPoint()

            zoomInMap(mapCenter, {
                missionService.newMission(mapCenter, missionType, getMarkerPoints(mapCenter, type))
                setMissionState(MissionState.None)
            })
        }

        binder.add(missionService.operator.error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {msg ->
                    Toast.makeText(context , msg , Toast.LENGTH_LONG).show()

                })

    }

    private fun handleCurrentMissionChanged(it: NullableObject<Mission>, missionService: MissionService) {
        //logMethod(this)
        val mission = it.value

        if (mission == null) {
            mapMissionController.onWayPointUpdated = null
            mapMissionController.onMarkerUpdated = null
            mapMissionController.onMarkerSelect = null
            mapMissionController.onAreaPointUpdated = null
            mapMissionController.onNewWaypoint = null

            missionPanel.onBackPressed = null
            missionPanel.onDeleteMission = null
            missionPanel.onUploadPressed = null
            missionPanel.onStartPressed = null
            missionPanel.onPausePressed = null
            missionPanel.onResumePressed = null
            missionPanel.onCancelPressed = null
            missionPanel.onSpacingChanged = null
            missionPanel.onAltitudeChanged = null
            missionPanel.onSpeedChanged = null
            missionPanel.onGimbalPitchChanged = null

            missionPanel.onAltitudeOverall = null
            missionPanel.onSpeedOverall = null
            missionPanel.onGimbalOverall = null
            missionPanel.onShootTimeIntervalOverall = null
            missionPanel.onATHChanged = null

            missionPanel.onItemSpeedChanged = null
            missionPanel.onItemShootTimeIntervalChanged = null
            missionPanel.onItemLatLngChanged = null
            missionPanel.onItemAltitudeChanged = null
            missionPanel.onItemGimbalPitchChanged = null

            missionStopButton.setOnClickListener(null)
        } else {
            mapMissionController.onWayPointUpdated = { wayPoints ->
                //log(this , "onWayPointUpdated")
                mission.wayPoints = wayPoints
                updateDistance(mission)
            }
            mapMissionController.onAreaPointUpdated = { points ->
                //log(this , "onAreaPointUpdated:${points.size}")

                mission.wayPoints = points.map {
                    val altitude = missionPanel.seekbarAltitude.progress + minAltitude
                    val waypoint = Waypoint(it.latitude , it.longitude , altitude.toFloat())
                    //log(this , "${waypoint.coordinate.latitude} , ${waypoint.coordinate.longitude} , ${waypoint.altitude}")

                    waypoint.heading = 0
                    waypoint.cornerRadiusInMeters = 0.2f
                    waypoint.turnMode = WaypointTurnMode.CLOCKWISE
                    waypoint.gimbalPitch = -90f
                    waypoint.speed = (missionPanel.seekbarSpeed.progress + minSpeed).toFloat()
                    waypoint.shootPhotoTimeInterval = (missionPanel.seekbarShootTimeInterval.progress + minShootTimeInterval).toFloat()

                    waypoint
                }
                updateDistance(mission)

            }
            mapMissionController.onMarkerUpdated = { markerPoints ->
                //log(this , "onMarkerUpdated")
                mission.markerPoints = markerPoints
            }

            mapMissionController.onMarkerSelect = { wayHolder ->
                this.wayHolder = wayHolder
                if(wayHolder!=null){
                    setWayHolder(wayHolder)
                }else{
                    missionPanel.showDetail()
                }
            }
            mapMissionController.onNewWaypoint = { position ->
                missionService.newWayPoint(mission,position)
            }

            missionPanel.onBackPressed = {
                if(missionPanel.linearOverall.visibility == View.GONE){
                    missionService.lastMission?.save()
                    missionService.setCurrentMission(null)
                }
                mapMissionController.removeSelectMarker()
            }
            missionPanel.onDeleteMission = {
                mapMissionController.removeSelectMarker()
                missionService.deleteMission(mission)
                missionService.stopMission()
            }
            missionPanel.onUploadPressed = {
                if(mission.wayPoints.size>0) {
                    uploadMission(mission)
                }else{
                    Toast.makeText(context , "mission is empty",Toast.LENGTH_LONG).show()
                }
            }
            missionPanel.onStartPressed = {
                startMission()
                mapMissionController.removeSelectMarker()
            }
            missionPanel.onPausePressed = {
                missionService.pauseMission()
            }
            missionPanel.onResumePressed = {
                missionService.resumeMission()
            }
            missionPanel.onCancelPressed = {
                missionService.stopMission()
            }
            missionPanel.onSpacingChanged = { spacing ->
                mission.spacing = spacing.toFloat()
                mapMissionController.setInterval(spacing, true)
            }
            missionPanel.onAltitudeChanged = { altitude ->
                mission.altitude = altitude.toFloat()
                if(mission.isAltitudeOverall){
                    mission.wayPoints.map {
                        it.altitude = mission.altitude
                    }
                }
            }
            missionPanel.onSpeedChanged = { speed ->
                mission.speed = speed.toFloat()
                if(mission.isSpeedOverall){
                    mission.wayPoints.map {
                        it.speed = mission.speed
                    }
                }
                updateDistance(mission)
            }
            missionPanel.onGimbalPitchChanged = {gimbalPitch ->
                mission.gimbalPitch = gimbalPitch.toFloat()
                if(mission.isGimbalPitchOverall){
                    mission.wayPoints.map {
                        it.gimbalPitch = mission.gimbalPitch
                    }
                }
            }

            missionPanel.onShootTimeIntervalChanged = {shootTimeInterval ->
                mission.timeInterval = shootTimeInterval.toFloat()
                if(mission.isTimeIntervalOverall){
                    mission.wayPoints.map {
                        it.shootPhotoTimeInterval = mission.timeInterval
                    }
                }
            }

            missionPanel.onAltitudeOverall = {isChecked ->
                mission.isAltitudeOverall = isChecked
                if (isChecked){
                    mission.wayPoints.map {
                        it.altitude = mission.altitude
                    }
                }

            }

            missionPanel.onSpeedOverall = {isChecked ->
                mission.isSpeedOverall = isChecked
                if(isChecked){

                    mission.wayPoints.map {
                        it.speed = mission.speed
                    }
                }
            }

            missionPanel.onGimbalOverall ={isChecked ->
                mission.isGimbalPitchOverall = isChecked
                if(isChecked){
                    mission.wayPoints.map {
                        it.gimbalPitch = mission.gimbalPitch
                    }
                }

            }

            missionPanel.onShootTimeIntervalOverall = {isChecked ->
                mission.isTimeIntervalOverall = isChecked
                if(isChecked){
                    mission.wayPoints.map {
                        it.shootPhotoTimeInterval = mission.timeInterval
                    }
                }
            }

            missionPanel.onATHChanged = {isGoHome ->
                if(isGoHome){
                    mission.finishedAction = WaypointMissionFinishedAction.GO_HOME
                }else{
                    mission.finishedAction = WaypointMissionFinishedAction.NO_ACTION
                }

            }

            missionPanel.onItemSpeedChanged= {speed ->
                wayHolder?.wayPoint?.speed = speed.toFloat()

            }

            missionPanel.onItemShootTimeIntervalChanged = {shootTimeInterval ->
                wayHolder?.wayPoint?.shootPhotoTimeInterval = shootTimeInterval.toFloat()

            }

            missionPanel.onItemLatLngChanged = {location->
                wayHolder?.wayPoint?.coordinate = location
                val position = LatLng(location.latitude , location.longitude)
                wayHolder?.marker?.position = position
                mapMissionController.setSelectMarker(position)
                mapMissionController.updatePath()
            }

            missionPanel.onItemAltitudeChanged = {altitude ->
                wayHolder?.wayPoint?.altitude = altitude.toFloat()

            }

            missionPanel.onItemGimbalPitchChanged ={gimbalPitch ->
                wayHolder?.wayPoint?.gimbalPitch = gimbalPitch.toFloat()

            }


            missionStopButton.setOnClickListener {
                missionService.stopMission()
            }
        }


        setCurrentMission(mission)
        //log(this , "finish")
    }

    private fun startMission(){
        logMethod(this)
        /*
        val flightActivity = context as FlightActivity
        flightActivity.layoutMission.visibility = View.GONE
        missionService.operator.stateSubject.onNext(MissionState.Executing)
        */
        missionService.startMission(object:CommonCallbacks.CompletionCallback{
            override fun onResult(error: DJIError?) {
                if(error==null){
                    //val flightActivity = context as FlightActivity
                    //flightActivity.layoutMission.visibility = View.GONE

                }else{
                    log(this , "start mission error:${error.description}")
                    missionService.operator.errorSubject.onNext("start mission failed:${error.description}")
                }
            }
        })

    }

    private fun uploadMission(mission:Mission){
        missionService.uploadMission(mission , object:CommonCallbacks.CompletionCallback{
            override fun onResult(error: DJIError?) {
                if(error == null){
                    setEditing(false)
                }else{
                    log(this , "upload mission error:${error.description}")
                    missionService.operator.errorSubject.onNext("上传任务失败${error.description}")
                }
            }
        })
    }



    private fun setMissionState(state: MissionState) {
        log(this ,"setMissionState:$state")
        if (state == MissionState.ReadyToExecuting) {
            showStartMissionDialog(missionService)
        }

        var time = Date().toString()
        missionStopButton.visibility = View.VISIBLE
        when (state) {
            MissionState.Uploading -> {
                log(this , "$time mission uploading")
                missionStatePanel.visibility = View.GONE
                missionStateText.text = "正在上传任务"
                missionStopButton.visibility = View.GONE
            }
            MissionState.Executing -> {
                log(this , "$time mission executing")
                missionStatePanel.visibility = View.VISIBLE
                missionStateText.text = "正在执行任务"
            }
            MissionState.Paused -> {
                missionStatePanel.visibility = View.VISIBLE
                missionStateText.text = "任务已暂停"
            }
            else -> {
                missionStatePanel.visibility = View.GONE
                missionStateText.text = ""
            }
        }

        missionPanel.setMissionState(state)
    }

    private var startDialog: AlertDialog? = null

    private fun showStartMissionDialog(missionService: MissionService) {
        startDialog?.dismiss()
        startDialog = AlertDialog.Builder(context).setMessage(R.string.start_mission_alert)
                .setPositiveButton(android.R.string.ok, { startDialog, _ ->
                    startDialog.dismiss()
                    startMission()
                })
                .setNegativeButton(android.R.string.cancel, { startDialog, _ ->
                    startDialog.dismiss()
                }).show()
    }


    private fun setEditing(isEditing: Boolean) {
        mapMissionController.setControlVisible(isEditing)
        mapMissionController.setTouchable(isEditing)
        isEditingSubject.onNext(isEditing)
    }

    private fun zoomInMap(center: LatLng, runnable: () -> Unit) {
        val update = CameraUpdateFactory.newCameraPosition(CameraPosition(center, 19f, 0f, 0f))
        mapView.map.moveCamera(update)

        // 如果不延迟执行的话，端点计算会出错
        Handler().postDelayed(runnable, 100)

    }

    private fun getMarkerPoints(mapCenter: LatLng, type: MapMissionController.ControlType): List<LatLng> =
            when (type) {
                MapMissionController.ControlType.Area -> {
                    val centerPoint = mapView.map.projection.toScreenLocation(mapCenter)
                    listOf(
                            Point(centerPoint.x - 100, centerPoint.y - 100),
                            Point(centerPoint.x + 100, centerPoint.y - 100),
                            Point(centerPoint.x + 100, centerPoint.y + 100),
                            Point(centerPoint.x - 100, centerPoint.y + 100)
                    ).map { mapView.map.projection.fromScreenLocation(it) }
                }
                MapMissionController.ControlType.Path -> listOf()
            }

    fun clearBinder() {
        binder.clear()
    }

    private fun setCurrentMission(mission: Mission?) {
        if (mission != null) {
            missionPanel.seekbarAltitude.progress = mission.altitude.toInt() - minAltitude
            missionPanel.seekbarSpeed.progress = mission.speed.toInt() - minSpeed
            missionPanel.seekbarSpacing.progress = mission.spacing.toInt() - minSpacing
            missionPanel.seekbarShootTimeInterval.progress = mission.timeInterval.toInt() - minShootTimeInterval
            missionPanel.seekbarGimbalPitch.progress = mission.gimbalPitch.toInt() - minGimbalPitch
            if(mission.finishedAction == WaypointMissionFinishedAction.GO_HOME) {
                missionPanel.switchATH.isChecked = true
            }else{
                missionPanel.switchATH.isChecked = false
            }
            if (mission.type == 0) {
                mapMissionController.setType(MapMissionController.ControlType.Area)
                mapMissionController.setInterval(mission.spacing.toInt())
                missionPanel.switchAltitude.visibility = View.GONE
                missionPanel.switchSpeed.visibility = View.GONE
                missionPanel.switchGimbalPitch.visibility = View.GONE
                missionPanel.switchShootTimeInterval.visibility = View.GONE
                setEditing(true)

                zoomInMap(mission.center, {
                    mapMissionController.setInterval(mission.spacing.toInt())
                    mapMissionController.setAreaMakerPoints(mission.markerPoints)
                })

            } else {
                missionPanel.switchAltitude.visibility = View.VISIBLE
                missionPanel.switchSpeed.visibility = View.VISIBLE
                missionPanel.switchGimbalPitch.visibility = View.VISIBLE
                missionPanel.switchShootTimeInterval.visibility = View.VISIBLE

                missionPanel.switchAltitude.isChecked = mission.isAltitudeOverall
                if(missionPanel.switchAltitude.isChecked){
                    missionPanel.linearAltitude.visibility = View.VISIBLE
                    missionPanel.seekbarItemAltitude.isEnabled = false
                }else{
                    missionPanel.linearAltitude.visibility = View.GONE
                    missionPanel.seekbarItemAltitude.isEnabled = true
                }

                missionPanel.switchSpeed.isChecked = mission.isSpeedOverall
                if(missionPanel.switchSpeed.isChecked){
                    missionPanel.linearSpeed.visibility = View.VISIBLE
                    missionPanel.seekbarItemSpeed.isEnabled = false
                }else{
                    missionPanel.linearSpeed.visibility = View.GONE
                    missionPanel.seekbarItemSpeed.isEnabled = true
                }

                missionPanel.switchGimbalPitch.isChecked = mission.isGimbalPitchOverall
                if(missionPanel.switchGimbalPitch.isChecked){
                    missionPanel.linearGimbalPitch.visibility = View.VISIBLE
                    missionPanel.seekbarItemGimbalPitch.isEnabled = false
                }else{
                    missionPanel.linearGimbalPitch.visibility = View.GONE
                    missionPanel.seekbarItemGimbalPitch.isEnabled = true
                }

                missionPanel.switchShootTimeInterval.isChecked = mission.isTimeIntervalOverall
                if(missionPanel.switchShootTimeInterval.isChecked){
                    missionPanel.linearShootTimeInterval.visibility = View.VISIBLE
                    missionPanel.seekbarItemShootTimeInterval.isEnabled = false
                }else{
                    missionPanel.linearShootTimeInterval.visibility = View.GONE
                    missionPanel.seekbarItemShootTimeInterval.isEnabled = true
                }

                mapMissionController.setType(MapMissionController.ControlType.Path)
                mapMissionController.setInterval(mission.spacing.toInt())

                setEditing(true)
                zoomInMap(mission.center, {
                    mapMissionController.setInterval(mission.spacing.toInt())
                    mapMissionController.setWayMakerPoints(mission.wayPoints)
                })
            }

            getAddress(mission)
        } else {
            missionPanel.seekbarAltitude.progress = minAltitude
            missionPanel.seekbarSpeed.progress = minSpeed
            missionPanel.seekbarSpacing.progress = minSpacing

            mapMissionController.clear()
            setEditing(false)
        }
    }

    private fun setWayHolder(wayHolder:WayHolder){
        missionPanel.showItem()
        missionPanel.editLat.setText("${wayHolder.wayPoint.coordinate.latitude}")
        missionPanel.editLng.setText("${wayHolder.wayPoint.coordinate.longitude}")

        missionPanel.seekbarItemAltitude.progress = wayHolder.wayPoint.altitude.toInt() - minAltitude
        missionPanel.seekbarItemShootTimeInterval.progress = wayHolder.wayPoint.shootPhotoTimeInterval.toInt() - minShootTimeInterval
        missionPanel.seekbarItemSpeed.progress = wayHolder.wayPoint.speed.toInt() - minSpeed
        missionPanel.seekbarItemGimbalPitch.progress = wayHolder.wayPoint.gimbalPitch.toInt() - minGimbalPitch
    }


    private fun updateDistance(mission: Mission?) {
        if (mission != null) {
            val wayPointSize = mission.wayPoints.size
            val distance = calculateDistance(mission.wayPoints)

            val exTime: Double = if (wayPointSize > 1) wayPointSize * (0.6 * mission.speed + 2.0) else 0.0
            val time = distance / mission.speed + exTime

            missionPanel.setDistance(distance)
            missionPanel.setTime(time.toInt())
        } else {
            missionPanel.setDistance(0)
            missionPanel.setTime(0)
        }
    }

    private fun calculateDistance(wayPoints: List<Waypoint>): Int {
        var distance = 0F
        var prePoint: LatLng? = null
        for (point in wayPoints) {

            val p = LatLng(point.coordinate.latitude,point.coordinate.longitude)
            if (prePoint != null) {
                distance += AMapUtils.calculateLineDistance(prePoint, p)
            }
            prePoint = p
        }
        return distance.toInt()
    }

    private fun getMapCenterPoint(): LatLng {
        val left = mapView.left
        val top = mapView.top
        val right = mapView.right
        val bottom = mapView.bottom
        val x = mapView.x + (right - left) / 2
        val y = mapView.y + (bottom - top) / 2
        return mapView.map.projection.fromScreenLocation(Point(x.toInt(), y.toInt()))
    }

    private fun getAddress(mission: Mission) {
        log(this, "get address")
        val search = GeocodeSearch(context)
        search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onRegeocodeSearched(result: RegeocodeResult?, p1: Int) {
                if (result != null) {
                    log(this, "get address success")
                    mission.address = result.regeocodeAddress.formatAddress
                    missionPanel.updateUI()
                }
            }

            override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) {
            }
        })
        search.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(mission.center.latitude, mission.center.longitude), 100f, GeocodeSearch.AMAP))
    }
}