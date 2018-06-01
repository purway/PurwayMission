package com.kaisavx.AircraftController.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.CompoundButton
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.kaisavx.AircraftController.BuildConfig
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.adapter.SettingAdapter
import com.kaisavx.AircraftController.interfaces.DJIFlightOperator
import com.kaisavx.AircraftController.service.FlightService
import com.kaisavx.AircraftController.util.*
import com.kaisavx.AircraftController.view.MissionPanel
import com.kaisavx.AircraftController.viewmodel.MissionViewModel
import dji.common.camera.StorageState
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.flightcontroller.ConnectionFailSafeBehavior
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.GPSSignalLevel
import dji.common.flightcontroller.flyzone.FlyZoneCategory
import dji.common.flightcontroller.flyzone.FlyZoneInformation
import dji.common.flightcontroller.flyzone.SubFlyZoneShape
import dji.common.model.LocationCoordinate2D
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_flight.*
import org.jetbrains.anko.async
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FlightActivity : BaseActivity() {

    private val TCP_LOG_PORT = 3000
    private var tcpLog: TCPLog? = null

    val REQUEST_PERMISSION_CODE = 12345
    val REQUIRED_PERMISSION_LIST = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE)

    private var missingPermission = java.util.ArrayList<String>()

    private var isMapMini = true

    private var height: Int = 0
    private var width: Int = 0
    private var margin: Int = 0
    private var deviceWidth: Int = 0
    private var deviceHeight: Int = 0

    private var locationClient: AMapLocationClient? = null
    private val locationOption = AMapLocationClientOption()

    private var isSetup = true

    val pathDrawer by lazy { FlightPathDrawer(mapWidget.map, this) }


    private val managerCallback = object : DJISDKManager.SDKManagerCallback {
        override fun onRegister(error: DJIError) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                log(this, "dji sdk success")
                DJISDKManager.getInstance().startConnectionToProduct()
                UserAccountManager.getInstance().logIntoDJIUserAccount(this@FlightActivity, object : CommonCallbacks.CompletionCallbackWith<UserAccountState> {
                    override fun onSuccess(state: UserAccountState?) {
                        log(this, "login success")
                        DJISDKManager.getInstance().flyZoneManager.getFlyZonesInSurroundingArea(object : CommonCallbacks.CompletionCallbackWith<ArrayList<FlyZoneInformation>> {
                            override fun onSuccess(flyZones: ArrayList<FlyZoneInformation>) {
                                //showToast("get surrounding Fly Zone Success!");
                                logMethod(this)
                                updateFlyZonesOnTheMap(flyZones);
                                //showSurroundFlyZonesInTv(flyZones);
                            }

                            override fun onFailure(error: DJIError) {
                                log(this, "get flyZone error:$error")
                            }
                        });
                    }

                    override fun onFailure(error: DJIError?) {
                        log(this, "login failed $error")
                    }
                })

            } else {
                log(this, "dji sdk failed, error: $error")
            }

        }

        override fun onProductChange(old: BaseProduct?, new: BaseProduct?) {
            log(this, "onProductChange, old: $old, new: $new")
            if (new != null) {
                new.setBaseProductListener(baseProductListener)

                if (new is Aircraft) {

                    binder.add(
                            getFlightController(new).subscribe {
                                log(this, "set state callback")
/*
                                val lowBattery = Share.getLowBatteryBack(this@FlightActivity)
                                val connectionFail = Share.getConnectionFailBack(this@FlightActivity)
                                val backHeight = Share.getBackHeight(this@FlightActivity)

                                it.setLowBatteryWarningThreshold(lowBattery,CommonCallbacks.CompletionCallback{
                                   log(this ,"error:${it.description}")
                                })

                                it.setSmartReturnToHomeEnabled(connectionFail , CommonCallbacks.CompletionCallback {
                                    log(this , "error:${it.description}")
                                })

                                it.setGoHomeHeightInMeters(backHeight, CommonCallbacks.CompletionCallback {
                                    log(this , "error:${it.description}")
                                })
*/
                                it.setStateCallback { state ->
                                    //log(this, "isFlying: ${state.isFlying} ${state.aircraftLocation} ${state.aircraftHeadDirection}")

                                    isFlying.onNext(state.isFlying)
                                    isLandingNeed.onNext(state.isLandingConfirmationNeeded)

                                    state.let {
                                        flightState.onNext(it)
                                    }
                                }
                            })

/*
                    binder.add(

                            getCamera(new).subscribe{
                                log(this , "setPhotoTimeIntervalSettings")
                                it.setPhotoTimeIntervalSettings(SettingsDefinitions.PhotoTimeIntervalSettings(5, 2), CommonCallbacks.CompletionCallback {
                                    flightOperator.showError("setPhotoTimeIntervalSettings" ,it)
                                })

                                log(this , "setShootPhotoMode")
                                it.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.INTERVAL, CommonCallbacks.CompletionCallback {
                                    flightOperator.showError("setShootPhotoMode" ,it)
                                })

                                it.setStorageStateCallBack(StorageState.Callback {
                                    //storageState.onNext(it)
                                })


                            }
                    )
                    */

                }

            } else {

            }

        }
    }

    private fun getFlightController(aircraft: Aircraft): Observable<FlightController> {
        return Observable.create { emitter ->
            Thread({
                var flightController = aircraft.flightController

                log(this, "get flightController: $flightController")

                while (flightController == null && !emitter.isDisposed) {
                    Thread.sleep(500)
                    flightController = aircraft.flightController
                }

                if (!emitter.isDisposed) {
                    emitter.onNext(flightController)
                    emitter.onComplete()
                }
            }).start()
        }
    }

    private fun getCamera(aircraft: Aircraft): Observable<Camera> {
        return Observable.create { emitter ->
            Thread({
                var camera = aircraft.camera

                log(this, "get camera: $camera")

                while (camera == null && !emitter.isDisposed) {
                    Thread.sleep(500)
                    camera = aircraft.camera
                    //flightController = aircraft.flightController
                }

                if (!emitter.isDisposed) {
                    emitter.onNext(camera)
                    emitter.onComplete()
                }
            }).start()
        }
    }

    private val baseProductListener = object : BaseProduct.BaseProductListener {
        override fun onComponentChange(key: BaseProduct.ComponentKey?, old: BaseComponent?, new: BaseComponent?) {
            log(this, "onComponentChange new:$new old:$old")
            //new?.setComponentListener(componentListener)
            if (new is FlightController) {
                log(this, "is FlightController")
                new.setComponentListener {
                    log(this, "controller connectivity change $it")
                }
                new.getSerialNumber(object:CommonCallbacks.CompletionCallbackWith<String>{
                    override fun onSuccess(serial: String?) {
                        log(this , "serial:$serial")
                    }
                    override fun onFailure(error: DJIError?) {
                        error?.let {
                            log(this,"error:${error.description}")
                        }
                    }

                })

                val lowBattery = Share.getLowBatteryBack(this@FlightActivity)
                val connectionFail = Share.getConnectionFailBack(this@FlightActivity)
                val backHeight = Share.getBackHeight(this@FlightActivity)

                log(this, "setLowBatteryWarningThreshold")
                new.setLowBatteryWarningThreshold(lowBattery, CommonCallbacks.CompletionCallback { error ->
                    if (error != null) {
                        log(this, "error:${error.description}")
                    } else {
                        log(this, "setLowBatteryWarningThreshold success")
                    }

                })
                log(this, "setSmartReturnToHomeEnabled")
                new.setSmartReturnToHomeEnabled(true, CommonCallbacks.CompletionCallback { error ->
                    if (error != null) {
                        log(this, "error:${error.description}")
                    } else {
                        log(this, "setSmartReturnToHomeEnabled success")
                    }

                })

                var behavior = ConnectionFailSafeBehavior.HOVER
                if (connectionFail) {
                    behavior = ConnectionFailSafeBehavior.GO_HOME
                }
                log(this, "setConnectionFailSafeBehavior")
                new.setConnectionFailSafeBehavior(behavior, CommonCallbacks.CompletionCallback { error ->
                    if (error != null) {
                        log(this, "error:${error.description}")
                    } else {
                        log(this, "setConnectionFailSafeBehavior success")
                    }
                })

                log(this, "setGoHomeHeightInMeters")
                new.setGoHomeHeightInMeters(backHeight, CommonCallbacks.CompletionCallback { error ->
                    if (error != null) {
                        log(this, "error:${error.description}")
                    } else {
                        log(this, "setGoHomeHeightInMeters success")
                    }
                })


                log(this ,"onComponentChange setStateCallback")
                new.setStateCallback { state ->
                    isFlying.onNext(state.isFlying)
                    isLandingNeed.onNext(state.isLandingConfirmationNeeded)

                    state.let {
                        flightState.onNext(it)
                    }
                }

            } else if (new is Camera) {
                log(this, "is Camera")

            }
        }

        override fun onConnectivityChange(flag: Boolean) {
            log(this, "onConnectivityChange $flag")
        }
    }

    private val componentListener = object : BaseComponent.ComponentListener {
        override fun onConnectivityChange(p0: Boolean) {
            log(this, "onConnectivityChange $p0")
        }
    }

    private var missionViewModel: MissionViewModel? = null
    /*by lazy{
MissionViewModel(this, supportFragmentManager.findFragmentById(R.id.missionPanel) as MissionPanel, missionStatePanel, textState, btnStop, mapWidget)
}*/

    private val flightOperator by lazy {
        DJIFlightOperator()
    }

    val isFlying: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val isLandingNeed: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val flightState: BehaviorSubject<FlightControllerState> = BehaviorSubject.create()
    val storageState: BehaviorSubject<StorageState> = BehaviorSubject.create()
    private val binder = CompositeDisposable()

    //Setting ListView
    private val settingList = ArrayList<HashMap<String, Any>>()
    private val settingAdapter by lazy {
        SettingAdapter(this, settingList)
    }

    //region Life-cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)
        logMethod(this)

        val fileDir = File(Environment.getExternalStorageDirectory(), "/AircraftController/xlog").absolutePath
        val filePath = fileDir + "/" + SimpleDateFormat("yyyy-MM-dd").format(Date())
        if (BuildConfig.DEBUG) {
            tcpLog = TCPLog(TCP_LOG_PORT, filePath)
            async { tcpLog?.createServer() }
        }

        checkAndRequestPermissions()
        supportActionBar?.hide()
        setContentView(R.layout.activity_flight)

        initView(savedInstanceState)
        initMap()

        missionViewModel = MissionViewModel(this, supportFragmentManager.findFragmentById(R.id.missionPanel) as MissionPanel, missionStatePanel, textState, btnStop, mapWidget)


        bindService(Intent(this, FlightService::class.java), connection, Context.BIND_AUTO_CREATE)

    }

    override fun onStart() {
        super.onStart()
        logMethod(this)

    }

    override fun onResume() {
        super.onResume()
        logMethod(this)
        mapWidget.onResume()

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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        logMethod(this)
        mapWidget.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        logMethod(this)
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        mapWidget.onDestroy()

        tcpLog?.destoryServer()

        missionViewModel?.clearBinder()
        unbindService(connection)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        logMethod(this)
        mapWidget.onLowMemory()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logMethod(this)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            /*
            val i = permissions.indexOf(Manifest.permission.READ_PHONE_STATE)
            if (i >= 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    log(this, "request permission")
                    startSDKRegistration()
                }
            }
            */
            for (i in grantResults.size - 1..0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                    missingPermission.remove(permissions[i])
                }
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {

        }

    }

    //function
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(className: ComponentName) {
            logMethod(this)
        }

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            logMethod(this)

            val service = (binder as FlightService.LocalBinder).service

            try {
                missionViewModel?.bindMissionService(service.missionService)
            } catch (e: Exception) {
                e.printStackTrace()
                log(this, "error:${e.message}")
            }

            this@FlightActivity.binder.add(flightState
                    //.distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { state ->
                        //log(this, "isFlying: ${state.isFlying} ${state.aircraftLocation} ${state.aircraftHeadDirection}")


                        pathDrawer.setFlightAngle(-state.aircraftHeadDirection.toFloat())


                        if (state.gpsSignalLevel != GPSSignalLevel.LEVEL_0 &&
                                state.gpsSignalLevel != GPSSignalLevel.LEVEL_1) {

                            state.aircraftLocation?.let {
                                val wgs = wgs84togcj02(LatLng(it.latitude, it.longitude))

                                pathDrawer.setFlightMarker(wgs, state.isFlying)
                            }

                            if (state.isHomeLocationSet) {
                                state.homeLocation?.let {
                                    val wgs = wgs84togcj02(LatLng(it.latitude, it.longitude))

                                    pathDrawer.setHomeMaker(wgs)
                                }
                            }
                        }

                        if(state.isFlying && !state.isGoingHome && state.isLowerThanBatteryWarningThreshold){
                            log(this , "low battery go home")
                            flightOperator.getDJIFlightController()?.let {
                                it.startGoHome { error ->
                                    if(error!=null){
                                        log(this, "start go home error:${error.description}")
                                    }else{
                                        log(this , "start go home success")
                                    }
                                }
                            }
                        }
                    })
/*
            this@FlightActivity.binder.add(storageState
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {

                    }
            )
*/
            this@FlightActivity.binder.add(isFlying
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (!it) {
                            flightOperator.stateSubject.onNext(DJIFlightOperator.FlightState.NONE)
                        } else {

                        }
                    })

            this@FlightActivity.binder.add(isLandingNeed
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it) {
                            flightOperator.stateSubject.onNext(DJIFlightOperator.FlightState.LANDING_CONFIRM)
                        }
                    })


        }
    }

    private fun checkAndRequestPermissions() {
        logMethod(this)
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration()

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            log(this, "Need to grant the permissions!")
            ActivityCompat.requestPermissions(this, missingPermission.toArray(Array<String>(missingPermission.size, { "" })), REQUEST_PERMISSION_CODE)
        }
    }

    private fun startSDKRegistration() {
        try {
            if (!DJISDKManager.getInstance().hasSDKRegistered()) {
                log(this, "start register dji sdk")
                DJISDKManager.getInstance().registerApp(this, managerCallback)
            } else {
                log(this, "dji sdk has registered")
                DJISDKManager.getInstance().product?.let {
                    log(this, "product exist create DJIFlight")

                }
            }

        } catch (e: Exception) {
            log(this, "DJI SDK register failed, $e", e)

        }


    }

    private fun initView(savedInstanceState: Bundle?) {
        height = DensityUtil.dip2px(this, 100f)
        width = DensityUtil.dip2px(this, 150f)
        margin = DensityUtil.dip2px(this, 12f)

        val displayMetrics = resources.displayMetrics
        deviceHeight = displayMetrics.heightPixels
        deviceWidth = displayMetrics.widthPixels

        //mapWidget.initAMap(MapWidget.OnMapReadyListener { map -> map.setOnMapClickListener { onViewClick(mapWidget) } })
        mapWidget.onCreate(savedInstanceState)



        viewSwitcher.setOnClickListener {
            log(this, "viewSwitcher onclick")
            onViewClick()

        }

        btnSetting.setOnClickListener {
            if (listViewSetting.visibility == View.GONE) {
                listViewSetting.visibility = View.VISIBLE
            } else {
                listViewSetting.visibility = View.GONE
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


        initSetting()

    }

    private fun initMap() {
        locationClient = AMapLocationClient(applicationContext)
        locationClient?.setLocationListener {
            //log(this, "location:$it")
            var zoom = 0f
            if(it.latitude == 0.0 || it.longitude == 0.0){
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

    private fun initSetting() {

        val lowMin = 15
        val lowMax = 50
        var lowValue = Share.getLowBatteryBack(this)

        listViewSetting.adapter = settingAdapter

        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_SEEKBAR,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.item_low_battery_back),
                SettingAdapter.KEY_VALUE to lowValue,
                SettingAdapter.KEY_MIN to lowMin,
                SettingAdapter.KEY_MAX to lowMax,
                SettingAdapter.KEY_DATA_TEXT to "$lowValue%",
                SettingAdapter.KEY_SEEK_BAR_CHANGE_LISTENER to object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {

                        log(this, "0 onStopTrackingTouch")

                        val m = settingList[0]

                        //val seek = m[SettingAdapter.KEY_SEEK_BAR]  as SeekBar
                        seekBar?.let {
                            val value = it.progress + (m[SettingAdapter.KEY_MIN] as Int)

                            Share.setLowBatteryBack(this@FlightActivity, value)
                            m[SettingAdapter.KEY_VALUE] = value
                            m[SettingAdapter.KEY_DATA_TEXT] = "$value%"
                            settingAdapter.notifyDataSetChanged()
                            flightOperator.getDJIFlightController()?.let {
                                it.setLowBatteryWarningThreshold(value, CommonCallbacks.CompletionCallback {

                                })
                                flightOperator.getDJIFlightController()?.let {
                                    it.setSmartReturnToHomeEnabled(true, CommonCallbacks.CompletionCallback {

                                    })
                                }
                            }
                        }


                    }
                }

        ))

        val disconnectValue = Share.getConnectionFailBack(this)
        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_SWITCH,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.item_connection_fail_back),
                SettingAdapter.KEY_VALUE to disconnectValue,
                SettingAdapter.KEY_SWITCH_CHANGE_LISTENER to CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
                    log(this, "1 checked change listener")
                    Share.setConnectionFailBack(this, b)
                    var behavior = ConnectionFailSafeBehavior.HOVER
                    if (b) {
                        behavior = ConnectionFailSafeBehavior.GO_HOME
                    }
                    flightOperator.getDJIFlightController()?.let {
                        it.setConnectionFailSafeBehavior(behavior, CommonCallbacks.CompletionCallback {

                        })
                    }
                })
        )
        val hightMin = 20
        val hightMax = 500
        var hightValue = Share.getBackHeight(this)
        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_SEEKBAR,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.item_back_height),
                SettingAdapter.KEY_VALUE to hightValue,
                SettingAdapter.KEY_MIN to hightMin,
                SettingAdapter.KEY_MAX to hightMax,
                SettingAdapter.KEY_DATA_TEXT to "${hightValue}M",
                SettingAdapter.KEY_SEEK_BAR_CHANGE_LISTENER to object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {

                        log(this, "2 onStopTrackingTouch")
                        val m = settingList[2]
                        //val seek = m[SettingAdapter.KEY_SEEK_BAR]  as SeekBar
                        seekBar?.let {

                            val min = m[SettingAdapter.KEY_MIN] as Int

                            val value = it.progress + min
                            Share.setBackHeight(this@FlightActivity, value)
                            log(this, "2 value ${m[SettingAdapter.KEY_VALUE] as Int} ${value} ${it.progress} ${min}")
                            m[SettingAdapter.KEY_VALUE] = value
                            m[SettingAdapter.KEY_DATA_TEXT] = "${value}M"
                            settingAdapter.notifyDataSetChanged()

                            flightOperator.getDJIFlightController()?.let {

                                it.setGoHomeHeightInMeters(value, CommonCallbacks.CompletionCallback {

                                })

                            }

                        }

                    }
                }
        )
        )

    }

    private fun updateFlyZonesOnTheMap(flyZones: ArrayList<FlyZoneInformation>) {
        logMethod(this)
        mapWidget.map?.let {
            runOnUiThread {
                it.clear()
                for (flyZone in flyZones) {
                    var polygonItems = flyZone.subFlyZones
                    var itemSize = polygonItems.size
                    for (i in 0..itemSize - 1) {
                        if (polygonItems[i].getShape() == SubFlyZoneShape.POLYGON) {
                            addPolygonMarker(polygonItems[i].getVertices(), flyZone.getCategory(), polygonItems[i].getMaxFlightHeight());
                        } else if (polygonItems[i].getShape() == SubFlyZoneShape.CYLINDER) {
                            val tmpPos = polygonItems[i].center
                            val subRadius = polygonItems[i].radius
                            val circle = CircleOptions()
                            circle.radius(subRadius)
                            circle.center(LatLng(tmpPos.latitude,
                                    tmpPos.longitude))
                            when (flyZone.category) {
                                FlyZoneCategory.WARNING -> circle.strokeColor(Color.GREEN)
                                FlyZoneCategory.ENHANCED_WARNING -> circle.strokeColor(Color.BLUE)
                                FlyZoneCategory.AUTHORIZATION -> {
                                    circle.strokeColor(Color.YELLOW)
                                    //unlockableIds.add(flyZone.flyZoneID)
                                }
                                FlyZoneCategory.RESTRICTED -> circle.strokeColor(Color.RED)

                                else -> {
                                }
                            }
                            it.addCircle(circle)
                        }
                    }
                }
            }
        }
    }

    private fun addPolygonMarker(polygonPoints: List<LocationCoordinate2D>?, flyZoneCategory: FlyZoneCategory, height: Int) {
        if (polygonPoints == null) {
            return
        }

        val points = ArrayList<LatLng>()

        for (point in polygonPoints) {
            points.add(LatLng(point.latitude, point.longitude))
        }
        var fillColor = resources.getColor(R.color.limit_fill)
        /*
        if (painter.getHeightToColor().get(height) != null) {
            fillColor = painter.getHeightToColor().get(height)
        } else */
        if (flyZoneCategory == FlyZoneCategory.AUTHORIZATION) {
            fillColor = resources.getColor(R.color.auth_fill)
        } else if (flyZoneCategory == FlyZoneCategory.ENHANCED_WARNING || flyZoneCategory == FlyZoneCategory.WARNING) {
            fillColor = resources.getColor(R.color.gs_home_fill)
        }
        val plg = mapWidget.map.addPolygon(PolygonOptions().addAll(points)
                .strokeColor(fillColor)
                .fillColor(fillColor))

    }

    private fun onViewClick() {
        isMapMini = !isMapMini
        if (isMapMini) {
            mapWidget.map.uiSettings.isMyLocationButtonEnabled = false
            mapWidget.map.uiSettings.isZoomControlsEnabled = false

            leftLayout.removeView(fpvWidget)
            fullLayout.removeView(mapWidget)
            leftLayout.addView(mapWidget)
            fullLayout.addView(fpvWidget)

            fpvOverlayWidget.visibility = View.VISIBLE

            btnTaskShow.visibility = View.GONE
            layoutMapSwitch.visibility = View.GONE
            missionStatePanel.visibility = View.GONE
            layoutMission?.visibility = View.GONE

            manualFocusWidget.visibility = View.GONE
            layoutCamera1.visibility = View.VISIBLE
            layoutCamera2.visibility = View.VISIBLE
            cameraCapturePanel.visibility = View.VISIBLE


        } else {
            mapWidget.map.uiSettings.isMyLocationButtonEnabled = true
            mapWidget.map.uiSettings.isZoomControlsEnabled = true
            leftLayout.removeView(mapWidget)
            fullLayout.removeView(fpvWidget)
            leftLayout.addView(fpvWidget)
            fullLayout.addView(mapWidget)

            fpvOverlayWidget.visibility = View.GONE

            btnTaskShow.visibility = View.GONE
            layoutMapSwitch.visibility = View.VISIBLE

            manualFocusWidget.visibility = View.GONE
            layoutCamera1.visibility = View.GONE
            layoutCamera2.visibility = View.GONE
            layoutMission?.visibility = View.VISIBLE

            cameraCapturePanel.visibility = View.GONE
            cameraSettingAdvancedPanel.visibility = View.GONE
            cameraSettingExposurePanel.visibility = View.GONE


        }
        /*
        if (view === fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0)
            val mapViewAnimation = ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin)
            mapWidget.startAnimation(mapViewAnimation)
            isMapMini = true
        } else if (view === mapWidget && isMapMini) {
            resizeFPVWidget(width, height, margin, 2)
            val mapViewAnimation = ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0)
            mapWidget.startAnimation(mapViewAnimation)
            isMapMini = false
        }
        */
    }

    private fun resizeFPVWidget(width: Int, height: Int, margin: Int, fpvInsertPosition: Int) {
        val fpvParams = fpvWidget.layoutParams as RelativeLayout.LayoutParams
        fpvParams.height = height
        fpvParams.width = width
        fpvParams.rightMargin = margin
        fpvParams.bottomMargin = margin
        fpvWidget.layoutParams = fpvParams
/*
        rootView.removeView(fpvWidget)
        rootView.addView(fpvWidget, fpvInsertPosition)
        */
    }

    private inner class ResizeAnimation constructor(private val mView: View, private val mFromWidth: Int, private val mFromHeight: Int, private val mToWidth: Int, private val mToHeight: Int, private val mMargin: Int) : Animation() {


        init {
            duration = 300
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight
            val width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth
            val p = mView.layoutParams as RelativeLayout.LayoutParams
            p.height = height.toInt()
            p.width = width.toInt()
            p.rightMargin = mMargin
            p.bottomMargin = mMargin
            mView.requestLayout()
        }
    }
}