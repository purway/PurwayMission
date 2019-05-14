package com.kaisavx.AircraftController.mamager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.kaisavx.AircraftController.util.Share
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import dji.common.battery.BatteryState
import dji.common.camera.SettingsDefinitions
import dji.common.camera.StorageState
import dji.common.camera.ThermalAreaTemperatureAggregations
import dji.common.camera.ThermalMeasurementMode
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.flightcontroller.ConnectionFailSafeBehavior
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.flyzone.FlyZoneInformation
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
import dji.common.flightcontroller.virtualstick.RollPitchControlMode
import dji.common.flightcontroller.virtualstick.VerticalControlMode
import dji.common.flightcontroller.virtualstick.YawControlMode
import dji.common.gimbal.GimbalState
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks
import dji.internal.useraccount.UserAccountInfo
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.battery.Battery
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.flightcontroller.FlightController
import dji.sdk.gimbal.Gimbal
import dji.sdk.media.DownloadListener
import dji.sdk.media.MediaFile
import dji.sdk.media.MediaManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DJIManager(val context: Context) {
    private val REQUEST_PERMISSION_CODE = 12345
    private val REQUIRED_PERMISSION_LIST = arrayOf(
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

    private var missingPermission = ArrayList<String>()

    val isFlying: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val isLandingNeed: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val flightState: BehaviorSubject<FlightControllerState> = BehaviorSubject.create()
    val storageState: BehaviorSubject<StorageState> = BehaviorSubject.create()
    val batteryStateSubject: BehaviorSubject<BatteryState> = BehaviorSubject.create()
    val aircraftNo: BehaviorSubject<String> = BehaviorSubject.create()
    val downloadProgress: BehaviorSubject<Int> = BehaviorSubject.create()
    val downloadMax: BehaviorSubject<Int> = BehaviorSubject.create()
    val uploadProgress: BehaviorSubject<Int> = BehaviorSubject.create()
    val uploadMax: BehaviorSubject<Int> = BehaviorSubject.create()
    val errorSubject: BehaviorSubject<String> = BehaviorSubject.create()
    val isInitSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    private val disposable = CompositeDisposable()

    private val destDir = File(Environment.getExternalStorageDirectory().path + "/AircraftController/pictures")
    var videoDataListener: VideoFeeder.VideoDataListener? = null
    var onRegisterSuccess: (() -> Unit)? = null
    var onProductConnected: (() -> Unit)? = null
    var onProductDisconnect: (() -> Unit)? = null
    var initCamera: (() -> Unit)? = null

    var xtCamera: Camera? = null

    var thermalSportCallback: Camera.TemperatureDataCallback? = null
    var thermalAreaCallback: ThermalAreaTemperatureAggregations.Callback? = null
    var isRun = true

    var gimbalState: GimbalState? = null
    var gimbal: Gimbal? = null

    val mediaFileList = ArrayList<MediaFile>()

    var lastTime = -1L

    private var currentFileListState: MediaManager.FileListState = MediaManager.FileListState.UNKNOWN

    private val updateFileListStateListener = object : MediaManager.FileListStateListener {
        override fun onFileListStateChange(state: MediaManager.FileListState) {
            currentFileListState = state
        }
    }


    private val managerCallback = object : DJISDKManager.SDKManagerCallback {
        override fun onRegister(error: DJIError) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                log(this, "dji sdk success")
                DJISDKManager.getInstance().startConnectionToProduct()

                UserAccountManager.getInstance().logIntoDJIUserAccount(context, object : CommonCallbacks.CompletionCallbackWith<UserAccountState> {
                    override fun onSuccess(state: UserAccountState?) {
                        log(this, "login success")

                        DJISDKManager.getInstance().flyZoneManager.getFlyZonesInSurroundingArea(object : CommonCallbacks.CompletionCallbackWith<ArrayList<FlyZoneInformation>> {
                            override fun onSuccess(flyZones: ArrayList<FlyZoneInformation>) {
                                //showToast("get surrounding Fly Zone Success!");
                                logMethod(this)
                                //updateFlyZonesOnTheMap(flyZones);
                                //showSurroundFlyZonesInTv(flyZones);
                            }

                            override fun onFailure(error: DJIError) {
                                log(this, "get flyZone error:$error")
                            }
                        })

                    }

                    override fun onFailure(error: DJIError?) {
                        log(this, "login failed $error")
                    }
                })
                UserAccountInfo()
                onRegisterSuccess?.invoke()
            } else {
                log(this, "dji sdk failed, error: $error")
            }
        }
/*
        override fun onProductChange(old: BaseProduct?, new: BaseProduct?) {
            log(this, "onProductChange, old: $old, new: $new")
            new?.setBaseProductListener(baseProductListener)

            if (new is Aircraft) {
                disposable.add(
                        getFlightController(new).subscribe {
                            configFlightController(it)
                        })

                disposable.add(
                        getCameras(new).subscribe { cameraList ->
                            var str=""

                            cameraList.map { camera ->
                                str+="$camera\n"
                                configXTCamera(camera)
                            }
                            log(this, "cameraList:$str")
                        }
                )

                disposable.add(
                        getCamera(new).subscribe { camera ->

                        }
                )

                disposable.add(
                        getGimbal(new).subscribe {

                        }
                )

                disposable.add(
                        getGimbals(new).subscribe { gimbalList ->
                            var str=""
                            gimbalList.map { gimbal ->
                                str+="$gimbal\n"
                                configGimbal(gimbal)
                            }
                            log(this, "gimbalList:$str")


                        }
                )

            }
        }
*/

        override fun onProductConnect(product: BaseProduct?) {
            if (product is Aircraft) {
                onProductConnected?.invoke()
            }
        }

        override fun onComponentChange(key: BaseProduct.ComponentKey?, old: BaseComponent?, new: BaseComponent?) {
            log(this, "onComponentChange new:$new old:$old")
            //new?.setComponentListener(componentListener)
            if (old === new) return

            new?.setComponentListener {
                log(this, "$new change $it")
            }

            if (new is FlightController) {
                log(this, "is FlightController")
                configFlightController(new)
            } else if (new is Camera) {
                log(this, "is Camera")
                initMediaManager()
                initCamera?.invoke()
            } else if (new is Gimbal) {
                log(this, "is Gimbal")
            } else if (new is Battery) {
                log(this, "is Battery")
                configBattery(new)
            }
        }

        override fun onProductDisconnect() {
            onProductDisconnect?.invoke()
        }
    }

/*
    private val baseProductListener = object : BaseProduct.BaseProductListener {
        override fun onComponentChange(key: BaseProduct.ComponentKey?, old: BaseComponent?, new: BaseComponent?) {
            log(this, "onComponentChange new:$new old:$old")
            //new?.setComponentListener(componentListener)
            if (old === new) return

            new?.setComponentListener {
                log(this, "$new change $it")
            }

            if (new is FlightController) {
                log(this, "is FlightController")
                configFlightController(new)
            } else if (new is Camera) {
                log(this, "is Camera")
                configXTCamera(new)
            } else if (new is Gimbal) {
                log(this, "is Gimbal")
            } else if (new is Battery) {
                log(this, "is Battery")
            }
        }

        override fun onConnectivityChange(flag: Boolean) {
            log(this, "onConnectivityChange $flag")
        }
    }
*/

    //function
    private fun startSDKRegistration() {
        logMethod(this)
        try {
            if (!DJISDKManager.getInstance().hasSDKRegistered()) {
                log(this, "start register dji sdk")
                DJISDKManager.getInstance().registerApp(context, managerCallback)
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

    fun checkAndRequestPermissions() {
        logMethod(this)
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(context, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration()

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            log(this, "Need to grant the permissions!")
            ActivityCompat.requestPermissions(context as Activity, missingPermission.toArray(Array<String>(missingPermission.size, { "" })), REQUEST_PERMISSION_CODE)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        logMethod(this)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices) {
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

    private fun configFlightController(flightController: FlightController) {
        logMethod(this)
        log(this, "getSerialNumber")
        flightController.getSerialNumber(object : CommonCallbacks.CompletionCallbackWith<String> {
            override fun onSuccess(serial: String?) {
                log(this, "serial:$serial")
                serial?.let {
                    aircraftNo.onNext(it)
                }
            }

            override fun onFailure(error: DJIError?) {
                error?.let {
                    log(this, "error:${error.description}")
                }
            }

        })

        val lowBattery = Share.getLowBatteryBack(context)
        val connectionFail = Share.getConnectionFailBack(context)
        val backHeight = Share.getBackHeight(context)

        log(this, "setLowBatteryWarningThreshold")
        flightController.setLowBatteryWarningThreshold(lowBattery, CommonCallbacks.CompletionCallback { error ->
            if (error != null) {
                log(this, "error:${error.description}")
            } else {
                log(this, "setLowBatteryWarningThreshold success")
            }

        })
        log(this, "setSmartReturnToHomeEnabled")
        flightController.setSmartReturnToHomeEnabled(true, CommonCallbacks.CompletionCallback { error ->
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
        flightController.setConnectionFailSafeBehavior(behavior, CommonCallbacks.CompletionCallback { error ->
            if (error != null) {
                log(this, "error:${error.description}")
            } else {
                log(this, "setConnectionFailSafeBehavior success")
            }
        })

        log(this, "setGoHomeHeightInMeters")
        flightController.setGoHomeHeightInMeters(backHeight, CommonCallbacks.CompletionCallback { error ->
            if (error != null) {
                log(this, "error:${error.description}")
            } else {
                log(this, "setGoHomeHeightInMeters success")
            }
        })

        log(this, "onComponentChange setStateCallback")
        flightController.setStateCallback { state ->
            isFlying.onNext(state.isFlying)
            isLandingNeed.onNext(state.isLandingConfirmationNeeded)

            state.let {
                flightState.onNext(it)
            }
        }

        flightController.verticalControlMode = VerticalControlMode.VELOCITY
        flightController.rollPitchControlMode = RollPitchControlMode.VELOCITY
        flightController.rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        flightController.yawControlMode = YawControlMode.ANGULAR_VELOCITY

    }

    private fun configXTCamera(camera: Camera) {
        if (!camera.isThermalCamera) {
            return
        }

        xtCamera = camera

        xtCamera?.getThermalProfile(object : CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ThermalProfile> {
            override fun onSuccess(thermalProfile: SettingsDefinitions.ThermalProfile?) {
                thermalProfile?.let {
                    when (it.resolution) {
                        SettingsDefinitions.ThermalResolution.RESOLUTION_336x256 -> {
                            log(this, "reusolotion:335x256")

                        }

                        SettingsDefinitions.ThermalResolution.RESOLUTION_640x512 -> {
                            log(this, "reusolotion:640x512")

                        }
                        SettingsDefinitions.ThermalResolution.UNKNOWN -> {
                            log(this, "reusolotion:unknown")

                        }
                        else -> {
                            log(this, "reusolotion:else")
                        }
                    }
                }

            }

            override fun onFailure(error: DJIError?) {
                error?.let {
                    log(this, "xtCamera getThermalProfile error:${it.description}")
                }

            }
        })

        xtCamera?.setThermalAreaTemperatureAggregationsCallback(thermalAreaCallback)
        xtCamera?.setThermalTemperatureCallback(thermalSportCallback)

        xtCamera?.setThermalMeasurementMode(ThermalMeasurementMode.AREA_METERING, {
            log(this, "setThermalMeasurementMode error:${it.description}")
        })

        xtCamera?.setThermalMeteringArea(RectF(0f, 0f, 1f, 1f), { error ->
            error?.let {
                log(this, "setThermalMeteringArea error:${it.description}")
            }
        })
    }

    private fun configGimbal(gimbal: Gimbal) {
        this.gimbal = gimbal
        gimbal.setStateCallback {
            //log(this , "$gimbal $it")
            gimbalState = it
        }
    }

    private fun configBattery(battery: Battery) {
        battery.setStateCallback {
            batteryStateSubject.onNext(it)
        }
    }

    fun getXTCamera(): Camera? {
        if (xtCamera == null) {
            log(this, "xtCamera is null try to get")
            DJISDKManager.getInstance()?.product?.camera?.let {
                if (it.isThermalCamera) {
                    xtCamera = it
                }
            }
        }

        log(this, "xtCamera $xtCamera")

        return xtCamera
    }

    fun destory() {
        disposable.clear()
        isRun = false
    }

    fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

    fun getFlightControllerInstance(): FlightController? {
        val product = getProductInstance()
        if (product is Aircraft) {
            return product.flightController
        }
        return null
    }

    fun getCameraInstance(): Camera? {
        val product = getProductInstance()
        return product?.camera
    }

    fun getMediaManagerInstance(): MediaManager? {
        getCameraInstance()?.let {
            return it.mediaManager
        }
        return null
    }

    fun initMediaManager() {
        isInitSubject.onNext(true)
        getMediaManagerInstance()?.let { mm ->

            mm.addUpdateFileListStateListener(updateFileListStateListener)
            getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, {
                if (it == null) {
                    if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                        log(this, "currentFileListState error:$currentFileListState")
                        isInitSubject.onNext(false)
                    } else {
                        mm.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, {
                            if (it == null) {
                                val fileList = ArrayList<MediaFile>()
                                fileList += mm.sdCardFileListSnapshot
                                Collections.sort(fileList, object : Comparator<MediaFile> {
                                    override fun compare(lhs: MediaFile, rhs: MediaFile): Int {
                                        if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                            return 1
                                        } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                            return -1
                                        }
                                        return 0
                                    }
                                })
                                if (fileList.size > 0) {
                                    lastTime = fileList[0].timeCreated
                                }
                                isInitSubject.onNext(false)
                            } else {
                                isInitSubject.onNext(false)
                                errorSubject.onNext("获取无人机文件列表失败")
                                log(this, "get file list failed:${it.description}")
                            }

                            getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                                it?.let {
                                    errorSubject.onNext("设置相机模式失败")
                                    log(this, "set Mode error:${it.description}")
                                }
                            })

                        })
                    }
                } else {
                    isInitSubject.onNext(false)
                    errorSubject.onNext("设置相机模式失败")
                    log(this, "Set cameraMode failed:${it.description}")
                    //setResultToToast("Set cameraMode failed:${it.description}")
                }
            })

        }

    }

    fun getFileList() {
        getMediaManagerInstance()?.let { mm ->
            getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, {
                if (it == null) {
                    if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                        log(this, "currentFileListState error:$currentFileListState")
                    } else {
                        mm.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, {
                            if (it == null) {
                                val fileList = ArrayList<MediaFile>()
                                fileList += mm.sdCardFileListSnapshot
                                Collections.sort(fileList, object : Comparator<MediaFile> {
                                    override fun compare(lhs: MediaFile, rhs: MediaFile): Int {
                                        if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                            return 1
                                        } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                            return -1
                                        }
                                        return 0
                                    }
                                })
                                mediaFileList.clear()
                                for (i in fileList.indices) {
                                    if (fileList[i].timeCreated > lastTime) {
                                        mediaFileList += fileList[i]
                                    } else {
                                        break;
                                    }
                                }
                                if (fileList.size > 0) {
                                    lastTime = fileList[0].timeCreated
                                }

                                if (mediaFileList.size > 0) {
                                    downloadMax.onNext(mediaFileList.size)
                                    downloadFileByIndex(mediaFileList.size - 1)
                                } else {
                                    errorSubject.onNext("没有无人机照片文件需要下载")
                                    getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                                        it?.let {
                                            errorSubject.onNext("设置相机模式失败")
                                            log(this, "Set cameraMode failed:${it.description}")
                                        }

                                    })
                                }


                            } else {
                                errorSubject.onNext("获取文件列表失败")
                                log(this, "get file list failed:${it.description}")
                                getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                                    it?.let {
                                        errorSubject.onNext("设置相机模式失败")
                                        log(this, "Set cameraMode failed:${it.description}")
                                    }

                                })

                            }
                        })
                    }
                } else {
                    errorSubject.onNext("设置相机模式失败")
                    log(this, "Set cameraMode failed:${it.description}")
                }
            })
        }
    }

    private fun downloadFileByIndex(index: Int) {

        if (index >= mediaFileList.size) {
            log(this, "mediaFileList out of index")
            return
        }
        if (index < 0) {
            getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                it?.let {
                    errorSubject.onNext("设置相机模式失败")
                    log(this, "Set cameraMode failed:${it.description}")
                }

            })

            downloadProgress.onNext(-1)
            uploadFiles()
            return
        }

        if (mediaFileList[index].mediaType == MediaFile.MediaType.PANORAMA || mediaFileList[index].mediaType == MediaFile.MediaType.SHALLOW_FOCUS) {
            return
        }

        mediaFileList[index].fetchFileData(destDir, null, object : DownloadListener<String> {
            override fun onFailure(error: DJIError) {
                /*
                hideDownloadDialog()
                setResultToToast("Download File Failed" + error.description)
                currentProgress = -1
                */
                log(this, "index:$index download failure:${error.description}")
                getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                    it?.let {
                        errorSubject.onNext("设置相机模式失败")
                        log(this, "Set cameraMode failed:${it.description}")
                    }
                })
                errorSubject.onNext("下载文件失败")
                downloadProgress.onNext(-1)
            }

            override fun onProgress(total: Long, current: Long) {}

            override fun onRateUpdate(total: Long, current: Long, persize: Long) {

                val tmpProgress = (1.0 * current / total * 100).toInt()
                /*
                if (tmpProgress != currentProgress) {
                    downloadDialog?.setProgress(tmpProgress)
                    currentProgress = tmpProgress
                }*/


                //downloadProgress.onNext(tmpProgress)
            }

            override fun onStart() {
                /*
                currentProgress = -1
                showDownloadDialog()
                */
                downloadProgress.onNext(index)
            }

            override fun onSuccess(filePath: String) {
                log(this, "index:$index download success")
                downloadProgress.onNext(index)
                downloadFileByIndex(index - 1)
                //downloadProgress.onNext(-1)

                /*
                hideDownloadDialog()
                setResultToToast("Download File Success:$filePath")
                currentProgress = -1
                */
            }
        })

    }

    var fileList = destDir.absoluteFile.listFiles()

    fun uploadFiles() {
        fileList = destDir.absoluteFile.listFiles()
        val fileIndex = fileList.size

        log(this, "fileList:")
        fileList.forEach {
            if (it.isFile) {
                log(this, "file:${it.name}")
            }
        }
        uploadMax.onNext(fileList.size)

        uploadProgress.onNext(fileList.size)

        uploadFileByIndex(fileList.size - 1)
    }

    private fun uploadFileByIndex(index: Int) {

        if (index >= fileList.size) {
            uploadProgress.onNext(-1)
            return
        }

        if (index < 0) {
            uploadProgress.onNext(-1)
            return
        }

        val userId = Share.getUserId(context)
        val cosPath = "picture/" + SimpleDateFormat("yyyy-MM-dd").format(Date()) + "/" + fileList[index].name

        val srcPath = fileList[index].absolutePath
    }

/*
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
*/
}