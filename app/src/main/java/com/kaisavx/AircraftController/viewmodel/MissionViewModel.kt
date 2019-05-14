package com.kaisavx.AircraftController.viewmodel

import android.app.AlertDialog
import android.app.ProgressDialog
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
import com.kaisavx.AircraftController.interfaces.DJIFlightOperator
import com.kaisavx.AircraftController.mamager.DJIManager2
import com.kaisavx.AircraftController.model.AircraftStatusRecord
import com.kaisavx.AircraftController.model.RequestWithUserId
import com.kaisavx.AircraftController.model.User
import com.kaisavx.AircraftController.service.Mission
import com.kaisavx.AircraftController.service.MissionService
import com.kaisavx.AircraftController.service.MissionState
import com.kaisavx.AircraftController.util.*
import com.kaisavx.AircraftController.view.MissionPanel
import dji.common.error.DJIError
import dji.common.flightcontroller.GPSSignalLevel
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

    private val disposable = CompositeDisposable()

    private var wayHolder: WayHolder? = null

    private val flightOperator by lazy { DJIFlightOperator() }

    var isTeleControl = false
    var isFlying = false

    val isEditingSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val takeOffSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val isHomeSetSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    val uploadProgressSubject: BehaviorSubject<Int> = BehaviorSubject.create()
    val uploadMaxSubject: BehaviorSubject<Int> = BehaviorSubject.create()
    private var fileList = DJIManager2.destDir.absoluteFile.listFiles()

    var updateAircraftStatus: ((Int, String?) -> Unit)? = null

    var waitDialog: ProgressDialog? = null

    val downloadDialog by lazy {
        val dialog = ProgressDialog(context)
        dialog.setTitle("正在下载图片")
        dialog.setIcon(android.R.drawable.ic_dialog_info)
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.setCanceledOnTouchOutside(false)

        dialog
    }

    val uploadDialog by lazy {
        val dialog = ProgressDialog(context)
        dialog.setTitle("正在上传图片")
        dialog.setIcon(android.R.drawable.ic_dialog_info)
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.setCanceledOnTouchOutside(false)

        dialog
    }

    fun bindMissionService(missionService: MissionService) {
        logMethod(this)
        this.missionService = missionService


        val currentMission = missionService.currentMissionSubject

        val missionState = missionService.operator.state.distinctUntilChanged()

        missionPanel.onSelectMission = { _, mission ->
            missionService.setCurrentMission(mission)
        }

        missionPanel.onCreateMission = { _, type ->
            log(this, "onCreateMission")
            val missionType = if (type == MapMissionController.ControlType.Area) {
                0
            } else {
                1
            }
            val mapCenter = getMapCenterPoint()

            zoomInMap(mapCenter, 0f, {
                missionService.newMission(mapCenter, missionType, getMarkerPoints(mapCenter, type))
                setMissionState(MissionState.None)
            })
        }

        disposable.add(missionService.operator.error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                })
        disposable.add(takeOffSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    //takeOffBeforeStartMission()
                })
        disposable.add(isHomeSetSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    //Toast.makeText(context,"isHomeSt:$it",Toast.LENGTH_SHORT).show()
                    if (it) {

                        showStartMissionDialog()

                    }
                })
        disposable.add(missionState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    log(this, "disposable state:$it")
                    setMissionState(it)
                })

        val isMissionStopped: Observable<Boolean> = Observable.combineLatest(
                currentMission, missionState,
                BiFunction { mission, state ->
                    mission.value != null && state == MissionState.None
                }
        )
        disposable.add(isMissionStopped
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it) {
                        setEditing(true)
                    }
                })

        disposable.add(currentMission
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    handleCurrentMissionChanged(it, missionService)
                })
        disposable.add(missionService.missionListSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    missionPanel.setMissions(it)
                })

        disposable.add(DJIManager2.downloadProgressSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it < 0) {
                        updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.DOWNLOAD_PICTURES.value, "error")
                        hideDownloadDialog()
                        uploadFiles()
                    } else {
                        updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.DOWNLOAD_PICTURES.value, "$it")
                        showDownloadDialog(downloadDialog.max - it)
                    }
                }
        )

        disposable.add(DJIManager2.downloadMaxSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    downloadDialog.max = it

                })
        disposable.add(uploadProgressSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it < 0) {
                        updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.UPLOAD_PICTURES.value, "error")
                        hideUploadDialog()
                    } else if (it == 0) {
                        updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.FINISHED.value, null)
                        hideUploadDialog()
                    } else {
                        updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.UPLOAD_PICTURES.value, "$it")
                        showUploadDialog(uploadDialog.max - it)
                    }
                })
        disposable.add(uploadMaxSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    uploadDialog.max = it
                })

        disposable.add(DJIManager2.errorSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                })

    }

    fun handleCurrentMissionChanged(it: NullableObject<Mission>, missionService: MissionService) {
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
            missionPanel.onSwitchShootTimeIntervalChanged = null
            missionPanel.onATHChanged = null

            missionPanel.onItemSpeedChanged = null
            missionPanel.onItemShootTimeIntervalChanged = null
            missionPanel.onItemLatLngChanged = null
            missionPanel.onItemAltitudeChanged = null
            missionPanel.onItemGimbalPitchChanged = null
            missionPanel.onSwitchItemShootTimeIntervalChanged = null

            missionStopButton.setOnClickListener(null)
        } else {
            mapMissionController.onWayPointUpdated = { wayPoints ->
                //log(this , "onWayPointUpdated")
                mission.wayPointList = wayPoints
                var altitudeStr = ""
                var speedStr = ""
                var gimbalStr = ""
                var shootTimeStr = ""

                mission.wayPointList.map {
                    if (mission.wayPointList.indexOf(it) != 0) {
                        altitudeStr += ","
                        speedStr += ","
                        gimbalStr += ","
                        shootTimeStr += ","
                    }
                    altitudeStr += it.altitude.toInt().toString()
                    speedStr += it.speed.toInt().toString()
                    gimbalStr += it.gimbalPitch.toInt().toString()
                    shootTimeStr += it.shootPhotoTimeInterval.toInt().toString()

                }
                if (!mission.isAltitudeOverall)
                    missionPanel.textAltitude.text = context.getString(R.string.mission_altitude, altitudeStr)
                if (!mission.isSpeedOverall)
                    missionPanel.textSpeed.text = context.getString(R.string.mission_speed, speedStr)
                if (!mission.isGimbalPitchOverall)
                    missionPanel.textGimbalPitch.text = context.getString(R.string.mission_gimbalPitch, gimbalStr)
                if (!mission.isTimeIntervalOverall)
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, shootTimeStr)
                updateDistance(mission)
            }
            mapMissionController.onAreaPointUpdated = { points ->
                //log(this , "onAreaPointUpdated:${points.size}")
                mission.wayPointList = points.map {
                    val altitude = missionPanel.seekbarAltitude.progress + minAltitude
                    val waypoint = Waypoint(it.latitude, it.longitude, altitude.toFloat())
                    //log(this , "${waypoint.coordinate.latitude} , ${waypoint.coordinate.longitude} , ${waypoint.altitude}")

                    waypoint.heading = 0
                    waypoint.cornerRadiusInMeters = 0.2f
                    waypoint.turnMode = WaypointTurnMode.CLOCKWISE
                    waypoint.gimbalPitch = mission.gimbalPitch
                    waypoint.speed = mission.speed
                    waypoint.shootPhotoTimeInterval = mission.timeInterval


                    waypoint
                }
                updateDistance(mission)

            }
            mapMissionController.onMarkerUpdated = { markerPointList ->
                //log(this , "onMarkerUpdated")
                mission.markerPointList = markerPointList
            }

            mapMissionController.onMarkerSelect = { wayHolder ->
                this.wayHolder = wayHolder
                if (wayHolder != null) {
                    setWayHolder(wayHolder)
                } else {
                    missionPanel.showDetail()
                }
            }
            mapMissionController.onNewWaypoint = { position ->
                missionService.newWayPoint(mission, position)
            }

            missionPanel.onBackPressed = {
                if (missionPanel.linearOverall.visibility == View.GONE) {

                    missionService.lastMission?.save()
                    missionService.setCurrentMission(null)
                }
                mapMissionController.removeSelectMarker()
            }
            missionPanel.onDeleteMission = {
                isTeleControl = false
                val userId = Share.getUserId(context)
                val userPasword = Share.getUserPassword(context)

                if (userId != null && userPasword != null) {
                    val requsetWithUserId = RequestWithUserId(User(userId, null, userPasword))
                    requsetWithUserId.wayPointMissionId = mission.id
                }

                mapMissionController.removeSelectMarker()
                missionService.deleteMission(mission)
                missionService.stopMission()
            }
            missionPanel.onUploadPressed = {
                isTeleControl = false
                if (mission.wayPointList.size > 0) {
                    uploadMission(mission)
                } else {
                    Toast.makeText(context, "任务为空", Toast.LENGTH_LONG).show()
                }
            }
            missionPanel.onStartPressed = {
                isTeleControl = false
                //startMission()
                safeStartMission()
                mapMissionController.removeSelectMarker()
            }
            missionPanel.onPausePressed = {
                isTeleControl = false
                missionService.pauseMission()
            }
            missionPanel.onResumePressed = {
                isTeleControl = false
                missionService.resumeMission()
            }
            missionPanel.onCancelPressed = {
                isTeleControl = false
                missionService.stopMission()
            }
            missionPanel.onSpacingChanged = { spacing ->
                if (mission.type == 0) {
                    mission.spacing = spacing.toFloat()

                    mapMissionController.setInterval(spacing, true)
                }
            }
            missionPanel.onAltitudeChanged = { altitude ->
                mission.altitude = altitude.toFloat()
                if (mission.isAltitudeOverall) {
                    mission.wayPointList.map {
                        it.altitude = mission.altitude
                    }
                }
            }
            missionPanel.onSpeedChanged = { speed ->
                mission.speed = speed.toFloat()
                if (mission.isSpeedOverall) {
                    mission.wayPointList.map {
                        it.speed = mission.speed
                    }
                }
                updateDistance(mission)
            }
            missionPanel.onGimbalPitchChanged = { gimbalPitch ->
                mission.gimbalPitch = gimbalPitch.toFloat()
                if (mission.isGimbalPitchOverall) {
                    mission.wayPointList.map {
                        it.gimbalPitch = mission.gimbalPitch
                    }
                }
            }

            missionPanel.onShootTimeIntervalChanged = { shootTimeInterval ->
                mission.timeInterval = shootTimeInterval.toFloat()
                if (mission.isTimeIntervalOverall) {
                    mission.wayPointList.map {
                        it.shootPhotoTimeInterval = mission.timeInterval
                    }
                }
            }

            missionPanel.onSwitchShootTimeIntervalChanged = { isChecked ->
                if (mission.isTimeIntervalOverall) {
                    if (isChecked) {
                        mission.timeInterval = Mission.TIME_INTERVAL
                        missionPanel.seekbarShootTimeInterval.progress = (mission.timeInterval - minShootTimeInterval).toInt()
                        mission.wayPointList.map {
                            it.shootPhotoTimeInterval = Mission.TIME_INTERVAL
                        }
                        missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, mission.timeInterval.toInt().toString())
                    } else {
                        mission.timeInterval = 0f

                        mission.wayPointList.map {
                            it.shootPhotoTimeInterval = 0f
                        }
                    }
                }
            }

            missionPanel.onATHChanged = { isGoHome ->
                if (isGoHome) {
                    mission.finishedAction = WaypointMissionFinishedAction.GO_HOME.value()
                } else {
                    mission.finishedAction = WaypointMissionFinishedAction.NO_ACTION.value()
                }

            }

            missionPanel.onItemSpeedChanged = { speed ->
                wayHolder?.wayPoint?.speed = speed.toFloat()

                var isOverall = true
                var string = ""
                mission.wayPointList.map {
                    if (speed.toFloat() != it.speed) {
                        isOverall = false

                    }
                    if (mission.wayPointList.indexOf(it) != 0) {
                        string += ","
                    }
                    string += "${it.speed.toInt()}"
                }
                mission.isSpeedOverall = isOverall
                missionPanel.seekbarSpeed.isEnabled = mission.isSpeedOverall
                if (isOverall) {
                    missionPanel.seekbarSpeed.progress = speed - minSpeed
                    missionPanel.textSpeed.text = context.getString(R.string.mission_speed, speed.toString())
                } else {
                    missionPanel.textSpeed.text = context.getString(R.string.mission_speed, string)
                }

            }

            missionPanel.onItemShootTimeIntervalChanged = { shootTimeInterval ->
                wayHolder?.wayPoint?.shootPhotoTimeInterval = shootTimeInterval.toFloat()

                var isOverall = true
                var string = ""

                mission.wayPointList.map {
                    if (shootTimeInterval.toFloat() != it.shootPhotoTimeInterval) {
                        isOverall = false
                    }
                    if (mission.wayPointList.indexOf(it) != 0) {
                        string += ","
                    }
                    string += "${it.shootPhotoTimeInterval.toInt()}"
                }
                mission.isTimeIntervalOverall = isOverall
                missionPanel.seekbarShootTimeInterval.isEnabled = mission.isTimeIntervalOverall

                if (isOverall) {
                    missionPanel.seekbarShootTimeInterval.progress = shootTimeInterval - minShootTimeInterval
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, shootTimeInterval.toString())
                    missionPanel.switchShootTimeInterval.isEnabled = true
                } else {
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, string)
                    missionPanel.switchShootTimeInterval.isEnabled = false
                }
            }

            missionPanel.onSwitchItemShootTimeIntervalChanged = { isChecked ->

                if (isChecked) {
                    missionPanel.textItemShootTimeInterval.text = context.getString(R.string.mission_timeInterval, mission.timeInterval.toInt().toString())
                    wayHolder?.wayPoint?.shootPhotoTimeInterval = mission.timeInterval
                } else {
                    wayHolder?.wayPoint?.shootPhotoTimeInterval = 0f
                }
                wayHolder?.wayPoint?.shootPhotoTimeInterval?.toInt()?.let {
                    missionPanel.onItemShootTimeIntervalChanged?.invoke(it)
                }

            }

            missionPanel.onItemLatLngChanged = { location ->
                wayHolder?.wayPoint?.coordinate = location
                val position = LatLng(location.latitude, location.longitude)
                wayHolder?.marker?.position = position
                mapMissionController.setSelectMarker(position)
                mapMissionController.updatePath()
            }

            missionPanel.onItemAltitudeChanged = { altitude ->
                wayHolder?.wayPoint?.altitude = altitude.toFloat()

                var isOverall = true
                var string = ""
                mission.wayPointList.map {
                    if (altitude.toFloat() != it.altitude) {
                        isOverall = false
                    }
                    if (mission.wayPointList.indexOf(it) != 0) {
                        string += ","
                    }
                    string += "${it.altitude.toInt()}"
                }

                mission.isAltitudeOverall = isOverall
                missionPanel.seekbarAltitude.isEnabled = mission.isAltitudeOverall

                if (isOverall) {
                    missionPanel.seekbarAltitude.progress = altitude - minAltitude
                    missionPanel.textAltitude.text = context.getString(R.string.mission_altitude, altitude.toString())

                } else {
                    missionPanel.textAltitude.text = context.getString(R.string.mission_altitude, string)
                }
            }

            missionPanel.onItemGimbalPitchChanged = { gimbalPitch ->
                wayHolder?.wayPoint?.gimbalPitch = gimbalPitch.toFloat()

                var isOverall = true
                var string = ""

                mission.wayPointList.map {
                    if (gimbalPitch.toFloat() != it.gimbalPitch) {
                        isOverall = false
                    }
                    if (mission.wayPointList.indexOf(it) != 0) {
                        string += ","
                    }
                    string += "${it.gimbalPitch.toInt()}"
                }
                mission.isGimbalPitchOverall = isOverall

                missionPanel.seekbarGimbalPitch.isEnabled = mission.isGimbalPitchOverall

                if (isOverall) {
                    missionPanel.seekbarGimbalPitch.progress = gimbalPitch - minGimbalPitch
                    missionPanel.textGimbalPitch.text = context.getString(R.string.mission_gimbalPitch, gimbalPitch.toString())

                } else {
                    missionPanel.textGimbalPitch.text = context.getString(R.string.mission_gimbalPitch, string)

                }

            }


            missionStopButton.setOnClickListener {
                missionService.stopMission()
            }
        }


        setCurrentMission(mission)
        //log(this , "finish")
    }

    private fun startMission() {
        logMethod(this)
        if (isTeleControl) {
        }
        updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.START.value, null)
        missionService.startMission(object : CommonCallbacks.CompletionCallback {
            override fun onResult(error: DJIError?) {
                if (error == null) {
                    //val flightActivity = context as FlightActivity
                    //flightActivity.layoutMission.visibility = View.GONE
                } else {
                    log(this, "start mission error:${error.description}")
                    updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.START.value, "start mission error:${error.description}")
                    missionService.operator.errorSubject.onNext("开始任务失败:${DJIErrorTranslation.translate(error)}")
/*
                    when(error){
                        DJIMissionError.HOME_POINT_DIRECTION_UNKNOWN ->takeOffSubject.onNext(true)
                        DJIMissionError.HOME_POINT_NOT_RECORDED->takeOffSubject.onNext(true)
                    }
                    */

                }
            }
        })

    }

    private fun waitHomeSetBeforeStartMission() {
        logMethod(this)
        if (!homeThread.isAlive) {
            homeThread.start()
        }
        homeSetDialog?.dismiss()
        homeSetDialog = AlertDialog.Builder(context).setMessage(R.string.start_mission_nogps_alert)
                .setNegativeButton(android.R.string.cancel, { dialog, _ ->
                    dialog.dismiss()
                    if (!homeThread.isInterrupted) {
                        homeThread.interrupt()
                    }
                }).show()
    }

    private fun safeStartMission() {
        val flightController = flightOperator.getDJIFlightController()
        if (flightController != null && flightController.state.isHomeLocationSet) {
            if (isTeleControl) {
                //updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.WAIT_INIT.value,null)
                //DJIManager2.initMediaManager()
                setEditing(false)
                startMission()
            } else {
                showStartMissionDialog()
            }
        } else {
            updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.WAIT_HOME_SET.value, null)
            waitHomeSetBeforeStartMission()
        }
    }

    inner class HomeThread : Thread() {
        override fun run() {
            var flag = true
            while (flag && !isInterrupted) {
                val flightController = flightOperator.getDJIFlightController()

                if (flightController != null) {
                    val isHomeSet = flightController.state.isHomeLocationSet

                    if (isHomeSet &&
                            flightController.state.gpsSignalLevel != GPSSignalLevel.LEVEL_0 &&
                            flightController.state.gpsSignalLevel != GPSSignalLevel.LEVEL_1 &&
                            flightController.state.gpsSignalLevel != GPSSignalLevel.LEVEL_2) {
                        isHomeSetSubject.onNext(isHomeSet)
                        flag = false
                    }
                } else {
                    flag = false
                }
                try {
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    val homeThread = HomeThread()

    private fun uploadMission(mission: Mission) {
        missionService.uploadMission(mission, object : CommonCallbacks.CompletionCallback {
            override fun onResult(error: DJIError?) {
                if (error == null) {
                    setEditing(false)
                } else {
                    log(this, "upload mission error:${error.description}")
                    missionService.operator.errorSubject.onNext("上传任务失败${DJIErrorTranslation.translate(error)}")
                }
            }
        })
    }

    private fun setMissionState(state: MissionState) {
        log(this, "setMissionState:$state")
        if (state == MissionState.ReadyToExecuting) {
            //showStartMissionDialog()
            safeStartMission()
        }
        /*
        else if(state == MissionState.Finish){
            updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.DOWNLOAD_PICTURES.value,null)
            //DJIManager2.getFileList()
        }*/

        var time = Date().toString()
        missionStopButton.visibility = View.VISIBLE
        when (state) {
            MissionState.Uploading -> {
                log(this, "$time mission uploading")
                missionStatePanel.visibility = View.GONE
                missionStateText.text = "正在上传任务"
                missionStopButton.visibility = View.GONE
            }
            MissionState.Executing -> {
                updateAircraftStatus?.invoke(AircraftStatusRecord.STATUS.EXECUTING.value, null)
                log(this, "$time mission executing")
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
    private var homeSetDialog: AlertDialog? = null

    fun showStartMissionDialog() {
        homeSetDialog?.dismiss()
        startDialog?.dismiss()
        startDialog = AlertDialog.Builder(context).setMessage(R.string.start_mission_alert)
                .setPositiveButton(android.R.string.ok, { startDialog, _ ->
                    startDialog.dismiss()
                    //DJIManager2.initMediaManager()
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

    private fun zoomInMap(center: LatLng, bearing: Float, runnable: () -> Unit) {
        val update = CameraUpdateFactory.newCameraPosition(CameraPosition(center, 19f, 0f, bearing))
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
        disposable.clear()
    }

    private fun setCurrentMission(mission: Mission?) {
        if (mission != null) {

            missionPanel.seekbarAltitude.progress = mission.altitude.toInt() - minAltitude
            missionPanel.seekbarSpeed.progress = mission.speed.toInt() - minSpeed
            missionPanel.seekbarSpacing.progress = mission.spacing.toInt() - minSpacing
            if (mission.timeInterval != 0f) {
                missionPanel.seekbarShootTimeInterval.progress = mission.timeInterval.toInt() - minShootTimeInterval
                missionPanel.switchShootTimeInterval.isChecked = true
                missionPanel.linearShootTimeInterval.visibility = View.VISIBLE
            } else {
                missionPanel.switchShootTimeInterval.isChecked = false
                missionPanel.linearShootTimeInterval.visibility = View.GONE
            }

            missionPanel.seekbarGimbalPitch.progress = mission.gimbalPitch.toInt() - minGimbalPitch

            missionPanel.seekbarAltitude.isEnabled = mission.isAltitudeOverall
            missionPanel.seekbarSpeed.isEnabled = mission.isSpeedOverall
            missionPanel.seekbarGimbalPitch.isEnabled = mission.isGimbalPitchOverall
            missionPanel.seekbarShootTimeInterval.isEnabled = mission.isTimeIntervalOverall

            missionPanel.switchShootTimeInterval.isEnabled = true

            if (mission.finishedAction == WaypointMissionFinishedAction.GO_HOME.value()) {
                missionPanel.switchATH.isChecked = true
            } else {
                missionPanel.switchATH.isChecked = false
            }
            if (mission.type == 0) {
                mapMissionController.setType(MapMissionController.ControlType.Area)
                mapMissionController.setInterval(mission.spacing.toInt())
                setEditing(true)

                zoomInMap(LatLng(mission.addressLatitude, mission.addressLongitude), mission.bearing, {
                    mapMissionController.setInterval(mission.spacing.toInt())
                    mapMissionController.setAreaMakerPoints(mission.markerPointList)
                })

                missionPanel.textAltitude.text = context.getString(R.string.mission_altitude, mission.altitude.toInt().toString())
                missionPanel.textSpeed.text = context.getString(R.string.mission_speed, mission.speed.toInt().toString())
                missionPanel.textGimbalPitch.text = context.getString(R.string.mission_gimbalPitch, mission.gimbalPitch.toInt().toString())
                if (mission.timeInterval != 0f) {
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, mission.timeInterval.toInt().toString())
                } else {
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval_close)
                }

            } else {

                if (!mission.isAltitudeOverall) {

                    var string = ""
                    mission.wayPointList.map { string += "${it.altitude.toInt()}," }
                    missionPanel.textAltitude.text = context.getString(R.string.mission_altitude, string)
                } else {
                    missionPanel.textAltitude.text = context.getString(R.string.mission_altitude, mission.altitude.toInt().toString())
                }

                if (!mission.isSpeedOverall) {
                    var string = ""
                    mission.wayPointList.map { string += "${it.speed.toInt()}," }
                    missionPanel.textSpeed.text = context.getString(R.string.mission_speed, string)
                } else {
                    missionPanel.textSpeed.text = context.getString(R.string.mission_speed, mission.speed.toInt().toString())
                }

                if (!mission.isGimbalPitchOverall) {
                    var string = ""
                    mission.wayPointList.map { string += "${it.gimbalPitch.toInt()}," }
                    missionPanel.textGimbalPitch.text = context.getString(R.string.mission_gimbalPitch, string)
                } else {
                    missionPanel.textGimbalPitch.text = context.getString(R.string.mission_gimbalPitch, mission.gimbalPitch.toInt().toString())
                }

                if (!mission.isTimeIntervalOverall) {
                    var string = ""
                    mission.wayPointList.map { string += "${it.shootPhotoTimeInterval.toInt()}," }
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, string)
                    missionPanel.switchShootTimeInterval.isEnabled = false
                } else if (mission.timeInterval != 0f) {
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval, mission.timeInterval.toInt().toString())
                } else {
                    missionPanel.textShootTimeInterval.text = context.getString(R.string.mission_timeInterval_close)
                }

                mapMissionController.setType(MapMissionController.ControlType.Path)
                mapMissionController.setInterval(mission.spacing.toInt())

                setEditing(true)
                zoomInMap(LatLng(mission.addressLatitude, mission.addressLongitude), mission.bearing, {
                    //mapMissionController.setInterval(mission.spacing.toInt())
                    mapMissionController.setWayMakerPoints(mission.wayPointList)

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

    private fun setWayHolder(wayHolder: WayHolder) {
        missionPanel.showItem()
        missionPanel.editLat.setText("${wayHolder.wayPoint.coordinate.latitude}")
        missionPanel.editLng.setText("${wayHolder.wayPoint.coordinate.longitude}")

        missionPanel.seekbarItemAltitude.progress = wayHolder.wayPoint.altitude.toInt() - minAltitude
        if (wayHolder.wayPoint.shootPhotoTimeInterval != 0f) {
            missionPanel.seekbarItemShootTimeInterval.progress = wayHolder.wayPoint.shootPhotoTimeInterval.toInt() - minShootTimeInterval
            missionPanel.linearItemShootTimeInterval.visibility = View.VISIBLE
            missionPanel.switchItemShootTimeInterval.isChecked = true
        } else {
            missionPanel.linearItemShootTimeInterval.visibility = View.GONE
            missionPanel.switchItemShootTimeInterval.isChecked = false
        }

        missionPanel.seekbarItemSpeed.progress = wayHolder.wayPoint.speed.toInt() - minSpeed
        missionPanel.seekbarItemGimbalPitch.progress = wayHolder.wayPoint.gimbalPitch.toInt() - minGimbalPitch
    }


    private fun updateDistance(mission: Mission?) {
        if (mission != null) {
            val wayPointSize = mission.wayPointList.size
            val distance = calculateDistance(mission.wayPointList)

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

            val p = LatLng(point.coordinate.latitude, point.coordinate.longitude)
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
                    mission.name = mission.address
                    missionPanel.updateUI()
                }
            }

            override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) {
            }
        })
        search.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(mission.addressLatitude, mission.addressLongitude), 100f, GeocodeSearch.AMAP))
    }

    fun uploadFiles() {
        fileList = DJIManager2.destDir.absoluteFile.listFiles()
        val fileIndex = fileList.size

        log(this, "fileList:")
        fileList.forEach {
            if (it.isFile) {
                log(this, "file:${it.name}")
            }
        }
        uploadMaxSubject.onNext(fileList.size)

        uploadProgressSubject.onNext(fileList.size)

        uploadFileByIndex(fileList.size - 1)
    }

    private fun uploadFileByIndex(index: Int) {

        if (index >= fileList.size) {
            uploadProgressSubject.onNext(-1)
            return
        }

        if (index < 0) {
            uploadProgressSubject.onNext(-1)
            return
        }


    }

    private fun showDownloadDialog(progress: Int) {
        if (!downloadDialog.isShowing) {
            downloadDialog.incrementProgressBy(-downloadDialog.progress)
            downloadDialog.show()
        }
        downloadDialog.progress = progress


    }

    private fun hideDownloadDialog() {
        if (downloadDialog.isShowing) {
            downloadDialog.dismiss()
        }
    }

    private fun showUploadDialog(progress: Int) {
        if (!uploadDialog.isShowing) {
            uploadDialog.incrementProgressBy(-uploadDialog.progress)
            uploadDialog.show()
        }
        uploadDialog.progress = progress
    }

    private fun hideUploadDialog() {
        if (uploadDialog.isShowing) {
            uploadDialog.dismiss()
        }

    }

    private fun showWaitDialog() {
        if (waitDialog == null) {
            waitDialog = ProgressDialog(context)
            waitDialog?.setMessage("正在初始化，请等待...")
            waitDialog?.setCanceledOnTouchOutside(false)
            waitDialog?.setCancelable(false)

        }
        waitDialog?.show()
    }

    private fun hideWaitDialog() {
        waitDialog?.let {
            it.dismiss()
        }
        waitDialog = null
    }
}