package com.kaisavx.AircraftController.activity

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.kaisavx.AircraftController.BuildConfig
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.interfaces.DJIFlightOperator
import com.kaisavx.AircraftController.interfaces.DJIWayPointOperator
import com.kaisavx.AircraftController.mamager.DJIManager2
import com.kaisavx.AircraftController.model.*
import com.kaisavx.AircraftController.processor.FlyRecordProcessor
import com.kaisavx.AircraftController.service.MissionService
import com.kaisavx.AircraftController.util.*
import com.kaisavx.AircraftController.view.MissionPanel
import com.kaisavx.AircraftController.viewmodel.MissionViewModel
import dji.common.camera.SettingsDefinitions
import dji.common.flightcontroller.ConnectionFailSafeBehavior
import dji.common.flightcontroller.GPSSignalLevel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_flight.*
import kotlinx.android.synthetic.main.item_seekbar.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FlightActivity : BaseActivity() {

    private val TCP_LOG_PORT = 3000
    private var tcpLog: TCPLog? = null

    private val REQUEST_CODE = 1000

    private var isMapMini = true

    private var height: Int = 0
    private var width: Int = 0
    private var margin: Int = 0
    private var deviceWidth: Int = 0
    private var deviceHeight: Int = 0

    private var locationClient: AMapLocationClient? = null
    private val locationOption = AMapLocationClientOption()

    private var isSetup = true

    val loadingDialog by lazy {
        val dialog = ProgressDialog(this)
        dialog.setMessage(resources.getString(R.string.alert_init))
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog
    }

    private val pathDrawer by lazy { FlightPathDrawer(mapWidget.map, this) }
    private val operator by lazy { DJIWayPointOperator() }

    val missionService by lazy { MissionService(operator) }
    private val flightOperator = DJIFlightOperator()

    private var missionViewModel: MissionViewModel? = null

    private var connectingTimer: Timer? = null

    
    private val commandDialog by lazy{CommandDialog(this)}

    private var aircraftId: String? = null
    private var createdTime = -1L

    private val moveGesture = object : GestureDetector.OnGestureListener {
        private val FLIP_DISTANCE = 50f

        override fun onDown(e: MotionEvent?): Boolean {
            return false
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1.y - e2.y > FLIP_DISTANCE) {
                log(this, "move up")
                isShowCameraDetail(false)
                return true
            }

            if (e2.y - e1.y > FLIP_DISTANCE) {
                log(this, "move down")
                isShowCameraDetail(true)
                return true
            }

            return false
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            return false
        }

        override fun onLongPress(e: MotionEvent?) {

        }

        override fun onShowPress(e: MotionEvent?) {

        }

    }
    private val moveDetector by lazy { GestureDetector(this, moveGesture) }

    private val disposable = CompositeDisposable()

    private val flyRecordProcessor by lazy{ FlyRecordProcessor(this)}

    private val popupAircraftView by lazy {
        layoutInflater.inflate(R.layout.pop_aircraft_option, null)
    }

    private val swrPush by lazy {
        popupAircraftView.findViewById<Switch>(R.id.swrPush)
    }
    private val txtPush by lazy {
        popupAircraftView.findViewById<TextView>(R.id.txtPush)
    }

    private val layoutPush by lazy {
        popupAircraftView.findViewById<ViewGroup>(R.id.layoutPush)
    }

    private val layoutVideoData by lazy {
        popupAircraftView.findViewById<ViewGroup>(R.id.layoutVideoData)
    }

    private val layoutMediaUpload by lazy {
        popupAircraftView.findViewById<ViewGroup>(R.id.layoutMediaUpload)
    }

    private val layoutWaypointSync by lazy {
        popupAircraftView.findViewById<ViewGroup>(R.id.layoutWaypointSync)
    }

    private val layoutRealTimeControl by lazy {
        popupAircraftView.findViewById<ViewGroup>(R.id.layoutRealTimeControl)
    }


    private val popupAircraftWindow by lazy {
        val popupWindow = PopupWindow(popupAircraftView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.isFocusable = true
        popupWindow.isTouchable = true
        popupWindow.isOutsideTouchable = true

        popupWindow.animationStyle = R.style.mypopwindow_anim_style
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        popupWindow
    }

    //region Life-cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)
        logMethod(this)
        initTcpLog()
        supportActionBar?.hide()
        setContentView(R.layout.activity_flight)

        initView(savedInstanceState)
        initMap()

        missionViewModel = MissionViewModel(this, supportFragmentManager.findFragmentById(R.id.missionPanel) as MissionPanel, missionStatePanel, textState, btnStop, mapWidget)
        try {
            missionViewModel?.bindMissionService(missionService)
            missionViewModel?.updateAircraftStatus = { status, msg ->
                updateAircraftStatus(status, msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log(this, "error:${e.message}")
        }
        initDisposable()
    }

    override fun onStart() {
        super.onStart()
        logMethod(this)
        flyRecordProcessor.start()

    }

    override fun onResume() {
        super.onResume()
        logMethod(this)
        mapWidget.onResume()

        setupAircraftoption()
    }

    override fun onPause() {
        super.onPause()
        logMethod(this)
        mapWidget.onPause()
    }

    override fun onStop() {
        super.onStop()
        logMethod(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        logMethod(this)
        disposable.clear()

        locationClient?.stopLocation()
        locationClient?.onDestroy()
        mapWidget.onDestroy()

        tcpLog?.destoryServer()

        missionViewModel?.clearBinder()

        connectingTimer?.cancel()
        connectingTimer = null

        commandDialog.destory()

        flyRecordProcessor.destory()

    }

    override fun onLowMemory() {
        super.onLowMemory()
        logMethod(this)
        mapWidget.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        logMethod(this)
        mapWidget.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logMethod(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logMethod(this)

        DJIManager2.getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
            if (it == null) {
            } else {
                log(this, "camera setMode Error:${it.description}")
            }
        })

    }

    //init
    private fun initView(savedInstanceState: Bundle?) {
        height = DensityUtil.dip2px(this, 100f)
        width = DensityUtil.dip2px(this, 150f)
        margin = DensityUtil.dip2px(this, 12f)

        val displayMetrics = resources.displayMetrics
        deviceHeight = displayMetrics.heightPixels
        deviceWidth = displayMetrics.widthPixels

        mapWidget.onCreate(savedInstanceState)

        btnSetting.setOnClickListener {
            /*
            if (listViewSetting.visibility == View.GONE) {
                listViewSetting.visibility = View.VISIBLE
            } else {
                listViewSetting.visibility = View.GONE
            }
            */
            if (popupAircraftWindow.isShowing) {
                popupAircraftWindow.dismiss()
            } else {
                signal.measure(0, 0)
                log(this, "signal height:${signal.measuredHeight}")
                popupAircraftWindow.showAtLocation(layoutFlight, Gravity.END or Gravity.TOP, 0, 0)
            }
        }

        btnMapSatellite.setOnClickListener {
            btnMapSatellite.visibility = View.GONE
            btnMapNormal.visibility = View.VISIBLE
            mapWidget.map.setMapType(AMap.MAP_TYPE_SATELLITE)
        }

        btnMapNormal.setOnClickListener {
            btnMapNormal.visibility = View.GONE
            btnMapSatellite.visibility = View.VISIBLE
            mapWidget.map.setMapType(AMap.MAP_TYPE_NORMAL)

        }

        btnTaskHide.setOnClickListener {
            layoutMission?.visibility = View.GONE
            btnTaskShow.visibility = View.VISIBLE
        }

        btnTaskShow.setOnClickListener {
            layoutMission?.visibility = View.VISIBLE
            btnTaskShow.visibility = View.GONE
        }

        btnMediaManager.setOnClickListener {
            if(DJIManager2.getMediaManagerInstance() ==null){
                commandDialog.showError(resources.getString(R.string.msg_aircraft_unlink))
            }else {
                val intent = Intent(this, MediaActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE)
            }
        }

        textureView.alpha = 0.0f

        textureView.setOnClickListener {
            switchViewClick()
        }

        fpvOverlayWidget.setOnTouchListener { v, event ->
            moveDetector.onTouchEvent(event)
            fpvOverlayWidget.onTouch(v, event)
        }

        //initSetting()

        initAircraftoption()
        btnDisable.setOnClickListener {
            DJIManager2.getFlightControllerInstance()?.setVirtualStickModeEnabled(false, {
                if (it != null) {
                    log(this, "setVirtualStickModeEnabled:${it.description}")
                } else {
                }
            })
        }

        btnDisconnect.setOnClickListener {
        }

    }

    private fun initTcpLog() {
        val fileDir = File(Environment.getExternalStorageDirectory(), "/AircraftController/xlog").absolutePath
        val filePath = fileDir + "/" + SimpleDateFormat("yyyy-MM-dd").format(Date())
        if (BuildConfig.DEBUG) {
            Thread {
                tcpLog = TCPLog(TCP_LOG_PORT, filePath)
                tcpLog?.createServer()
            }.start()
        }
    }

    private fun initMap() {
        locationClient = AMapLocationClient(applicationContext)
        locationClient?.setLocationListener {
            //log(this, "location:$it")
            var zoom = 0f
            if (it.latitude == 0.0 || it.longitude == 0.0) {
                return@setLocationListener
            }

            if (isSetup) {
                log(this, "Self Location lat:${it.latitude} log:${it.longitude}")

                isSetup = false
                zoom = 19f
                mapWidget.map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), zoom))
            } else {
                zoom = mapWidget.map.cameraPosition.zoom
            }

        }

        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
        locationClient?.setLocationOption(locationOption)
        locationClient?.startLocation()

        mapWidget.map.uiSettings.isMyLocationButtonEnabled = false
        mapWidget.map.uiSettings.isZoomControlsEnabled = false

        val myLocationStyle = MyLocationStyle().anchor(0.5f, 1f)
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);

        myLocationStyle.strokeColor(Color.TRANSPARENT)
        myLocationStyle.radiusFillColor(Color.TRANSPARENT)
        myLocationStyle.strokeWidth(0f)
        mapWidget.map.myLocationStyle = myLocationStyle


        mapWidget.map.isMyLocationEnabled = true


    }

    private fun initAircraftoption() {
        val sekLowBattery = popupAircraftView.findViewById<SeekBar>(R.id.sekLowBattery)
        val txtLowBattery = popupAircraftView.findViewById<TextView>(R.id.txtLowBattery)

        val lowMin = 15
        val lowMax = 50
        val lowValue = Share.getLowBatteryBack(this)

        sekLowBattery.progress = lowValue - lowMin
        sekLowBattery.max = lowMax - lowMin

        txtLowBattery.setText("$lowValue%")

        sekLowBattery.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtLowBattery.setText("${progress + lowMin}%")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val value = it.progress + lowMin
                    Share.setLowBatteryBack(this@FlightActivity, value)
                    flightOperator.getDJIFlightController()?.let {
                        it.setLowBatteryWarningThreshold(value, {

                        })
                        it.setSmartReturnToHomeEnabled(true, {

                        })

                    }
                }
            }
        })


        val sekBackHeight = popupAircraftView.findViewById<SeekBar>(R.id.sekBackHeight)
        val txtBackHeight = popupAircraftView.findViewById<TextView>(R.id.txtBackHeight)

        val hightMin = 20
        val hightMax = 500
        val hightValue = Share.getBackHeight(this)

        sekBackHeight.progress = hightValue - hightMin
        sekBackHeight.max = hightMax - hightMin

        txtBackHeight.setText("${hightValue}M")

        sekBackHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtBackHeight.setText("${progress + hightMin}M")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekbar?.let {
                    val value = it.progress + hightMin
                    Share.setBackHeight(this@FlightActivity, value)
                    flightOperator.getDJIFlightController()?.let {
                        it.setGoHomeHeightInMeters(value, {

                        })
                    }

                }
            }
        })

        val swrConnectionFailBack = popupAircraftView.findViewById<Switch>(R.id.swrConnectionFailBack)
        swrConnectionFailBack.isChecked = Share.getConnectionFailBack(this)
        swrConnectionFailBack.setOnCheckedChangeListener { buttonView, isChecked ->
            Share.setConnectionFailBack(this, isChecked)
            var behavior = ConnectionFailSafeBehavior.HOVER
            if (isChecked) {
                behavior = ConnectionFailSafeBehavior.GO_HOME
            }
            flightOperator.getDJIFlightController()?.let {
                it.setConnectionFailSafeBehavior(behavior, {

                })
            }
        }

        swrPush.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {
                val controller = DJIManager2.getFlightControllerInstance()

                if(controller == null){
                    commandDialog.showError(resources.getString(R.string.msg_aircraft_unlink))
                    swrPush.isChecked = false
                    return@setOnCheckedChangeListener
                }
                val userId = Share.getUserId(this)
                val userPasword = Share.getUserPassword(this)
                if (userId != null && userPasword != null) {
                    commandDialog.setWaitShow(true)
                    val requsetWithUserId = RequestWithUserId(User(userId, null, userPasword))
                }else{
                    commandDialog.showError(resources.getString(R.string.msg_user_unlogin))
                }

            } else {
            }
        }

        val swrVideoData = popupAircraftView.findViewById<Switch>(R.id.swrVideoData)
        swrVideoData.isChecked = Share.getIsVideoData(this)
        swrVideoData.setOnCheckedChangeListener { buttonView, isChecked ->
            Share.setIsVideoData(this, isChecked)
        }

        val swrMediaUpload = popupAircraftView.findViewById<Switch>(R.id.swrMediaUpload)
        swrMediaUpload.isChecked = Share.getIsMediaUpload(this)
        swrMediaUpload.setOnCheckedChangeListener { buttonView, isChecked ->
            Share.setIsMediaUpload(this, isChecked)
        }

        val swrWaypointSync = popupAircraftView.findViewById<Switch>(R.id.swrWaypointSync)
        swrWaypointSync.isChecked = Share.getIsWaypointSync(this)
        swrWaypointSync.setOnCheckedChangeListener { buttonView, isChecked ->
            Share.setIsWaypointSync(this, isChecked)
        }

        val swrRealTimeControl = popupAircraftView.findViewById<Switch>(R.id.swrRealTimeControl)
        swrRealTimeControl.isChecked = Share.getIsRealtimeControl(this)
        swrRealTimeControl.setOnCheckedChangeListener { buttonView, isChecked ->
            Share.setIsRealtimeControl(this, isChecked)
        }


    }

    private fun initDisposable() {

        disposable.add(DJIManager2.isConnectedSubject
                .distinctUntilChanged()
                .subscribe {
                    if (it) {

                        //changeCamera()
                    } else {
                        connectingTimer?.cancel()
                        connectingTimer = null

                        val userId = Share.getUserId(this)
                        val userPasword = Share.getUserPassword(this)
                        if (userId != null && userPasword != null) {
                            val requsetWithUserId = RequestWithUserId(User(userId, null, userPasword))
                            DJIManager2.aircraftNoSubject.value?.let {
                                userId?.let { id ->
                                    val aircraftData = AircraftData(aircraftId, it, it, id, false)
                                    requsetWithUserId.aircraft = aircraftData

                                    updateAircraftStatus(AircraftStatusRecord.STATUS.DISCONNECTED.value, "$it disconnected")
                                }
                            }
                        }
                    }
                })

        disposable.add(DJIManager2.flightStateSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    pathDrawer.setFlightAngle(-state.aircraftHeadDirection.toFloat())
                    state.aircraftLocation?.let {
                        val wgs = wgs84togcj02(LatLng(it.latitude, it.longitude))
                        //log(this, "wgs:$wgs")

                        if (state.gpsSignalLevel != GPSSignalLevel.LEVEL_0 &&
                                state.gpsSignalLevel != GPSSignalLevel.LEVEL_1 /*&&
                                    state.gpsSignalLevel != GPSSignalLevel.LEVEL_2*/) {

                            var isEdit = false
                            missionViewModel?.isEditingSubject?.value?.let {
                                isEdit = it
                            }
                            pathDrawer.setFlightMarker(wgs, state.isFlying && !isEdit)

                            if (isSetup) {
                                isSetup = false
                                mapWidget.map.animateCamera(CameraUpdateFactory.newLatLngZoom(wgs, 19f))
                            }

                            if (state.isHomeLocationSet) {
                                state.homeLocation?.let {
                                    val wgs = wgs84togcj02(LatLng(it.latitude, it.longitude))
                                    pathDrawer.setHomeMaker(wgs)

                                    //log(this , "home location:$wgs")
                                }
                            }
                        }
                    }

                    if (state.isFlying && !state.isGoingHome && state.isLowerThanBatteryWarningThreshold) {
                        log(this, "low battery go home")

                    }
                    //log(this , "Ultrasonic:${state.isUltrasonicBeingUsed} ${state.ultrasonicHeightInMeters}")
                })

        disposable.add(DJIManager2.isFlyingSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    /*
                    if (it) {
                    } else {
                        flightOperator.stateSubject.onNext(DJIFlightOperator.FlightState.NONE)
                        if (missionViewModel?.isFlying == true) {
                            val permission = Permission(Share.getPermission(this))

                            val flag = permission.isMediaUpload() && Share.getIsMediaUpload(this)
                            DJIManager2.getFileList(flag)
                        }
                    }

                    missionViewModel?.isFlying = it
*/
                })

        disposable.add(DJIManager2.isLandingNeedSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it) {
                        flightOperator.stateSubject.onNext(DJIFlightOperator.FlightState.LANDING_CONFIRM)
                    }
                })

        disposable.add(DJIManager2.isCameraInitSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    log(this, "isCameraInitSubject:$it")
                    /*
                    if (it) {
                        loadingDialog.show()
                    } else {
                        loadingDialog.dismiss()
                        DJIManager2.getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                            if (it == null) {
                                log(this, "setCameraMode:success")
                            } else {
                                log(this, "camera setMode Error:${it.description}")
                            }

                        })
                    }
                    */
                })

        disposable.add(DJIManager2.aircraftNoSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { no ->
                    runOnUiThread {
                        txtPush.setText(no)
                    }
                    val userId = Share.getUserId(this)
                    val userPasword = Share.getUserPassword(this)
                    if (userId != null && userPasword != null) {
                        val requsetWithUserId = RequestWithUserId(User(userId, null, userPasword))
                        userId?.let { id ->

                            val aircraftData = AircraftData(null, no, no, id, true)

                            requsetWithUserId.aircraft = aircraftData
                            log(this, "updateOrCreateAircraftWithUserId")


                        }
                    }
                })


        disposable.add(flyRecordProcessor.flyPointListSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { flyPointList ->
                    val flyRoute: ArrayList<LatLng> = arrayListOf()
                    flyPointList.map {
                        if (it.latitude != null &&
                                it.longitude != null) {
                            flyRoute.add(LatLng(it.latitude, it.longitude))
                        }
                    }
                    pathDrawer.setFlyRoute(flyRoute)
                })

    }

    private fun initTimer() {
        connectingTimer = Timer()
        connectingTimer?.schedule(object : TimerTask() {
            override fun run() {
                val userId = Share.getUserId(this@FlightActivity)
                val userPasword = Share.getUserPassword(this@FlightActivity)
                if (userId != null && userPasword != null) {
                    val requestWithUserId = RequestWithUserId(User(userId, null, userPasword))
                    DJIManager2.aircraftNoSubject.value?.let {
                        if (aircraftId != null) {
                            userId?.let { id ->
                                val aircraftData = AircraftData(aircraftId, it, it, id, true)
                                requestWithUserId.aircraft = aircraftData

                            }

                        }
                    }

                    if (createdTime < 0) return

                    requestWithUserId.skip = 0
                    requestWithUserId.limit = 10
                    requestWithUserId.createdTime = createdTime
                    /*
                    AircraftApi.getAircraftMissionListGTCreatedTimeWithUserId(requestWithUserId, object : Api.ApiCallback {
                        override fun onSuccess(json: JSONObject) {
                            //logMethod(this)
                            //log(this, "json:${json}")

                            val responseData = AircraftApi.gson.fromJson(json.toString(), ResponseData::class.javaObjectType)
                            responseData.aircraftMissionList?.map {
                                it.createdTime?.let {
                                    createdTime = it
                                }
                                when (it.type) {
                                    AircraftMission.TYPE.START.value -> {
                                        it.param1?.let {
                                            proccessStartMission(it)
                                        }

                                    }
                                    AircraftMission.TYPE.RESUME.value -> {
                                        operator.resume()
                                    }
                                    AircraftMission.TYPE.PAUSE.value -> {
                                        operator.pause()
                                    }
                                    AircraftMission.TYPE.STOP.value -> {
                                        operator.stop()
                                    }

                                    AircraftMission.TYPE.RTH.value -> {
                                        flightOperator.startGoHome()
                                    }
                                    AircraftMission.TYPE.CANCEL_RTH.value -> {
                                        flightOperator.cancelGoHome()
                                    }

                                    AircraftMission.TYPE.TAKE_OFF.value -> {
                                        flightOperator.startTakeOff()
                                    }
                                    AircraftMission.TYPE.CANCEL_TAKE_OFF.value -> {
                                        flightOperator.cancelTakeOff()
                                    }

                                    AircraftMission.TYPE.LAND.value -> {
                                        flightOperator.startLanding()
                                    }
                                    AircraftMission.TYPE.LAND.value -> {
                                        flightOperator.cancelLanding()
                                    }
                                    else -> {

                                    }
                                }
                            }
                        }

                        override fun onError(code: Int) {
                            //logMethod(this)
                        }

                        override fun onFailure() {
                            //logMethod(this)
                        }
                    })
*/
                }
            }
        }, 0, 1000)
    }

    private fun proccessStartMission(id: String) {
        logMethod(this)
        val userId = Share.getUserId(this)
        val userPasword = Share.getUserPassword(this)
        if (userId != null && userPasword != null) {
            commandDialog.setWaitShow(true)
            val requestWithUserId = RequestWithUserId(User(userId, null, userPasword))
            requestWithUserId.wayPointMissionId = id
        }else{
            commandDialog.showError(resources.getString(R.string.msg_user_unlogin))
        }
    }

    private fun updateAircraftStatus(status: Int, msg: String?) {

        val userId = Share.getUserId(this)
        val userPasword = Share.getUserPassword(this)

        val requestWithUserId = RequestWithUserId(User(userId, null, userPasword))

        if (userId != null && userPasword != null)
            aircraftId?.let {
                userId?.let { id ->
                    val aircraftStatusRecord = AircraftStatusRecord(id, it, status)
                    aircraftStatusRecord.messsage = msg
                    requestWithUserId.aircraftStatusRecord = aircraftStatusRecord


                }

            }

    }

    private fun switchViewClick() {
        isMapMini = !isMapMini
        if (isMapMini) {
            mapWidget.map.uiSettings.isMyLocationButtonEnabled = false
            mapWidget.map.uiSettings.isZoomControlsEnabled = false

            leftLayout.removeView(fpvWidget)
            fullLayout.removeView(mapWidget)
            leftLayout.addView(mapWidget)
            fullLayout.addView(fpvWidget)

            layoutMap.visibility = View.GONE
            layoutCamera.visibility = View.VISIBLE
        } else {
            mapWidget.map.uiSettings.isMyLocationButtonEnabled = true
            mapWidget.map.uiSettings.isZoomControlsEnabled = true
            leftLayout.removeView(mapWidget)
            fullLayout.removeView(fpvWidget)
            leftLayout.addView(fpvWidget)
            fullLayout.addView(mapWidget)

            layoutMap.visibility = View.VISIBLE
            layoutCamera.visibility = View.GONE

            val fpvParams = fpvWidget.layoutParams
                    fpvParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fpvParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            fpvWidget.layoutParams = fpvParams
        }
    }

    private fun setupAircraftoption() {
        val permission = Permission(Share.getPermission(this))

        if (permission.isLive()) {
            layoutPush.visibility = View.VISIBLE
        } else {
            layoutPush.visibility = View.GONE
        }

        if (permission.isVideoData()) {
            layoutVideoData.visibility = View.VISIBLE
        } else {
            layoutVideoData.visibility = View.GONE
        }

        if (permission.isMediaUpload()) {
            layoutMediaUpload.visibility = View.VISIBLE
        } else {
            layoutMediaUpload.visibility = View.GONE
        }

        if (permission.isWaypointSync()) {
            layoutWaypointSync.visibility = View.VISIBLE
        } else {
            layoutWaypointSync.visibility = View.GONE
        }

        if (permission.isRealtimeControl()) {
            layoutRealTimeControl.visibility = View.VISIBLE
        } else {
            layoutRealTimeControl.visibility = View.GONE
        }
    }

    private fun isShowCameraDetail(isShow: Boolean) {
        if (!isMapMini) return
        if (isShow) {
            leftLayout.visibility = View.VISIBLE
            textureView.isClickable = true
            layoutRandar.visibility = View.VISIBLE
            takeOffReturnPanel.visibility = View.VISIBLE
            layoutDashboard.visibility = View.VISIBLE

            layoutCameraDetail.visibility = View.VISIBLE
        } else {
            leftLayout.visibility = View.GONE
            textureView.isClickable = false
            layoutRandar.visibility = View.GONE
            takeOffReturnPanel.visibility = View.GONE
            layoutDashboard.visibility = View.GONE

            layoutCameraDetail.visibility = View.GONE
        }
    }


}