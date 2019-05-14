package com.kaisavx.AircraftController.mamager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.kaisavx.AircraftController.util.Share
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import dji.common.battery.BatteryState
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.flightcontroller.ConnectionFailSafeBehavior
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
import dji.common.flightcontroller.virtualstick.RollPitchControlMode
import dji.common.flightcontroller.virtualstick.VerticalControlMode
import dji.common.flightcontroller.virtualstick.YawControlMode
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.battery.Battery
import dji.sdk.camera.Camera
import dji.sdk.flightcontroller.FlightController
import dji.sdk.gimbal.Gimbal
import dji.sdk.media.DownloadListener
import dji.sdk.media.MediaFile
import dji.sdk.media.MediaManager
import dji.sdk.mission.MissionControl
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.useraccount.UserAccountManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

object DJIManager2 {

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

    var isConnectedSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    var isRegisterSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val isFlyingSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val isLandingNeedSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val flightStateSubject: BehaviorSubject<FlightControllerState> = BehaviorSubject.create()
    val batteryStateSubject: BehaviorSubject<BatteryState> = BehaviorSubject.create()
    val aircraftNoSubject: BehaviorSubject<String> = BehaviorSubject.create()
    val downloadProgressSubject: BehaviorSubject<Int> = BehaviorSubject.create()
    val downloadMaxSubject: BehaviorSubject<Int> = BehaviorSubject.create()
    val errorSubject: BehaviorSubject<String> = BehaviorSubject.create()
    val isCameraInitSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val userAccountStateSubject:BehaviorSubject<UserAccountState> = BehaviorSubject.create()
    val disposable = CompositeDisposable()

    val accountSubject: BehaviorSubject<String> = BehaviorSubject.create()

    val flightControllerSubejct: BehaviorSubject<FlightController> = BehaviorSubject.create()

    private var lastTime = -1L

    private val managerCallback = object : DJISDKManager.SDKManagerCallback {
        override fun onRegister(error: DJIError) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                log(this, "dji sdk success")
                DJISDKManager.getInstance().startConnectionToProduct()
                isRegisterSubject.onNext(true)

            } else {
                isRegisterSubject.onNext(false)
                log(this, "dji sdk failed, error: $error")
            }
        }

        override fun onProductConnect(product: BaseProduct?) {
            if (product is Aircraft) {

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
                isConnectedSubject.onNext(true)
                flightControllerSubejct.onNext(new)
                //configFlightController(new)
            } else if (new is Camera) {
                log(this, "is Camera")
                initMediaManager()
            } else if (new is Gimbal) {
                log(this, "is Gimbal")
            } else if (new is Battery) {
                log(this, "is Battery")

                configBattery(new)
            }
        }

        override fun onProductDisconnect() {
            isConnectedSubject.onNext(false)

        }
    }


    private fun startSDKRegistration(context: Context) {
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

    private val mediaFileList = ArrayList<MediaFile>()
    val destDir = File(Environment.getExternalStorageDirectory().path + "/AircraftController/pictures")

    private var currentFileListState: MediaManager.FileListState = MediaManager.FileListState.UNKNOWN

    private val updateFileListStateListener = object : MediaManager.FileListStateListener {
        override fun onFileListStateChange(state: MediaManager.FileListState) {
            currentFileListState = state
        }
    }

    fun configFlightController(context: Context, flightController: FlightController) {
        logMethod(this)
        log(this, "getSerialNumber")
        flightController.getSerialNumber(object : CommonCallbacks.CompletionCallbackWith<String> {
            override fun onSuccess(serial: String?) {
                log(this, "serial:$serial")
                serial?.let {
                    aircraftNoSubject.onNext(it)
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
            isFlyingSubject.onNext(state.isFlying)
            isLandingNeedSubject.onNext(state.isLandingConfirmationNeeded)

            state.let {
                flightStateSubject.onNext(it)
            }
        }

        flightController.verticalControlMode = VerticalControlMode.VELOCITY
        flightController.rollPitchControlMode = RollPitchControlMode.VELOCITY
        flightController.rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        flightController.yawControlMode = YawControlMode.ANGULAR_VELOCITY
    }

    private fun configBattery(battery: Battery) {
        battery.setStateCallback {
            batteryStateSubject.onNext(it)
        }
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

    fun getMissionControlInstance(): MissionControl? {
        return DJISDKManager.getInstance()?.missionControl
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
        isCameraInitSubject.onNext(true)
        getMediaManagerInstance()?.let { mm ->

            mm.addUpdateFileListStateListener(updateFileListStateListener)
            getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, {
                if (it == null) {
                    if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                        log(this, "currentFileListState error:$currentFileListState")
                        isCameraInitSubject.onNext(false)
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
                                isCameraInitSubject.onNext(false)
                            } else {
                                isCameraInitSubject.onNext(false)
                                errorSubject.onNext("获取无人机文件列表失败")
                                log(this, "get file list failed:${it.description}")
                            }
/*
                            getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                                it?.let {
                                    errorSubject.onNext("设置相机模式失败")
                                    log(this , "set Mode error:${it.description}")
                                }
                            })
                            */

                        })
                    }
                } else {
                    isCameraInitSubject.onNext(false)
                    errorSubject.onNext("设置相机模式失败")
                    log(this, "Set cameraMode failed:${it.description}")
                }
            })

        }

    }

    fun getFileList(isUpload: Boolean) {
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

                                if (mediaFileList.size > 0 && isUpload) {
                                    downloadMaxSubject.onNext(mediaFileList.size)
                                    downloadFileByIndex(mediaFileList.size - 1)
                                } else {
                                    if (isUpload) {
                                        errorSubject.onNext("没有无人机照片文件需要下载")
                                        getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, {
                                            it?.let {
                                                errorSubject.onNext("设置相机模式失败")
                                                log(this, "Set cameraMode failed:${it.description}")
                                            }

                                        })
                                    } else {
                                        mediaFileList.clear()
                                    }
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

            downloadProgressSubject.onNext(-1)
            //uploadFiles()
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
                downloadProgressSubject.onNext(-1)
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

                downloadProgressSubject.onNext(index)
            }

            override fun onSuccess(filePath: String) {
                log(this, "index:$index download success")
                downloadProgressSubject.onNext(index)
                downloadFileByIndex(index - 1)

            }
        })

    }

    fun checkLogin(){
        disposable.clear()
        disposable.add(Observable.interval(500,TimeUnit.MILLISECONDS)
                .takeUntil(userAccountStateSubject)
                .observeOn(Schedulers.computation())
                .subscribe {
                    UserAccountManager.getInstance()?.let { manager->
                        val state = manager.userAccountState
                        if(state!=UserAccountState.UNKNOWN){
                            userAccountStateSubject.onNext(state)
                        }
                    }
                })
    }

    fun login(context: Context,failedFun:()->Unit) {
        UserAccountManager.getInstance()?.let { manager ->
            val state = manager.userAccountState
            if (state != UserAccountState.NOT_AUTHORIZED &&
                    state != UserAccountState.AUTHORIZED) {
                manager.logIntoDJIUserAccount(context, object : CommonCallbacks.CompletionCallbackWith<UserAccountState> {
                    override fun onSuccess(state: UserAccountState?) {
                        log(this, "login success")
                        manager.getLoggedInDJIUserAccountName(object : CommonCallbacks.CompletionCallbackWith<String> {
                            override fun onSuccess(account: String) {
                                accountSubject.onNext(account)
                            }

                            override fun onFailure(error: DJIError?) {
                                error?.let {
                                    log(this, "error:${it.description}")
                                }

                            }
                        })
                    }

                    override fun onFailure(error: DJIError?) {
                        log(this, "login failed $error")
                        failedFun.invoke()
                    }
                })

            }else{
                manager.getLoggedInDJIUserAccountName(object:CommonCallbacks.CompletionCallbackWith<String>{
                    override fun onFailure(error: DJIError?) {
                        error?.let {
                            log(this, "error:${it.description}")
                        }
                    }

                    override fun onSuccess(account: String) {
                        accountSubject.onNext(account)
                    }
                })
            }
        }
    }

    fun logout() {
        UserAccountManager.getInstance().logoutOfDJIUserAccount {
            it?.let {
                log(this, "logoutOfDJIUserAccount:${it.description}")
            }
        }
    }

    fun checkAndRequestPermissions(activity: Activity) {
        logMethod(this)
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(activity, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration(activity)

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            log(this, "Need to grant the permissions!")
            ActivityCompat.requestPermissions(activity, missingPermission.toArray(Array<String>(missingPermission.size, { "" })), REQUEST_PERMISSION_CODE)
        }
    }

    fun onRequestPermissionsResult(context: Context, requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        logMethod(this)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                    missingPermission.remove(permissions[i])
                }
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration(context)
        } else {

        }
    }
}