package com.kaisavx.AircraftController.module.dji

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.NullableObject
import com.kaisavx.AircraftController.util.plusAssign
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.remotecontroller.RemoteController
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class DJIManager {
    companion object {
        private val TAG = "DJIManager"
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

        fun hasRegistered(): Boolean {
            return DJISDKManager.getInstance().hasSDKRegistered()
        }
    }

    private val disposable = CompositeDisposable()

    private var missingPermission = ArrayList<String>()

    private val isRegisterSubject = BehaviorSubject.create<Boolean>()
    private val aircraftSub = BehaviorSubject.create<NullableObject<Aircraft>>()
    private val djiFlightSub = BehaviorSubject.create<NullableObject<DJIFlight>>()
    private val rcSub = BehaviorSubject.create<NullableObject<RemoteController>>()
    private val fcSub = BehaviorSubject.create<NullableObject<FlightController>>()
    private val camerasSub = BehaviorSubject.createDefault<List<Camera>>(listOf())

    val isRegisterOb: Observable<Boolean> = isRegisterSubject
    val aircraftOb: Observable<NullableObject<Aircraft>> = aircraftSub
    val djiFlightOb: Observable<NullableObject<DJIFlight>> = djiFlightSub
    val rcOb: Observable<NullableObject<RemoteController>> = rcSub
    val fcOb: Observable<NullableObject<FlightController>> = fcSub
    val camerasOb: Observable<List<Camera>> = camerasSub

    private val managerCallback = object : DJISDKManager.SDKManagerCallback {

        override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
            KLog.i(TAG, "onInitProcess event:$event")
        }

        override fun onRegister(error: DJIError) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                KLog.d(TAG, "dji sdk register success")
                isRegisterSubject.onNext(true)
                connectProduct()
            } else {
                KLog.d(TAG, "dji sdk register failed,error:${error.description}")
                isRegisterSubject.onNext(false)
            }
        }

        override fun onComponentChange(key: BaseProduct.ComponentKey?, old: BaseComponent?, new: BaseComponent?) {
            KLog.i(TAG, "onComponentChange new:$new old:$old")
            if (new === old) return

            if (new is RemoteController) {
                KLog.d(TAG, "RemoteController found isConnected:${new.isConnected}")
                rcSub.onNext(NullableObject(new))
                new.setComponentListener {
                    KLog.d(TAG, "RemoteController change $it")
                    if (it) {
                        rcSub.onNext(NullableObject(new))
                    } else {
                        rcSub.onNext(NullableObject(null))
                    }
                }

            } else if (new is FlightController) {
                KLog.d(TAG, "FlightController found")
                fcSub.onNext(NullableObject(new))
                new.setComponentListener {
                    KLog.d(TAG, "FlightController change $it")
                    if (it) {
                        fcSub.onNext(NullableObject(new))
                    } else {

                        fcSub.onNext(NullableObject(null))

                    }
                }

            } else if (new is Camera) {
                KLog.d(TAG, "$new Camera found isTLS:${new.isTimeLapseSupported} isMDMS:${new.isMediaDownloadModeSupported} isPS:${new.isPlaybackSupported}")
                cameraListChange()
                new.setComponentListener {
                    KLog.d(TAG, "Camera change $it")
                    cameraListChange()
                }
            }
        }

        override fun onProductConnect(product: BaseProduct?) {
            KLog.i(TAG, "onProductConnect:$product")
            if (product is Aircraft) {
                aircraftSub.onNext(NullableObject(product))
                KLog.d(TAG, "Aircraft found")

                val rc = product.remoteController
                KLog.d(TAG, "RC:$rc")
                rcSub.onNext(NullableObject(rc))
                rc?.setComponentListener {
                    if (it) {
                        rcSub.onNext(NullableObject(rc))
                    } else {
                        rcSub.onNext(NullableObject(null))
                    }
                }

                val fc = product.flightController
                KLog.d(TAG, "FC:$fc")
                fcSub.onNext(NullableObject(fc))
                fc?.setComponentListener {
                    if (it) {
                        fcSub.onNext(NullableObject(fc))
                    } else {
                        fcSub.onNext(NullableObject(null))
                    }
                }

                KLog.d(TAG, "cameras size:${product.cameras?.size}")
                product.cameras?.forEach { camera ->
                    camera.setComponentListener {
                        cameraListChange()
                    }
                }
                cameraListChange()

            } else if (product is HandHeld) {
                KLog.d(TAG, "HandHeld found")
            }
        }

        override fun onProductDisconnect() {
            KLog.i(TAG, "onProductDisconnect")
            aircraftSub.onNext(NullableObject(null))
            connectProduct()
        }

    }

    init {
        val fob: Observable<Pair<NullableObject<Aircraft>, NullableObject<FlightController>>> = Observable.combineLatest(
                aircraftSub,
                fcSub,
                BiFunction { t1, t2 ->
                    Pair(t1, t2)
                }
        )

        disposable += fob
                .observeOn(Schedulers.computation())
                .subscribe {
                    val aircraft = it.first.value
                    val fc = it.second.value
                    KLog.d(TAG,"fob aircraft:$aircraft fc:$fc")
                    djiFlightSub.value?.value?.destory()
                    if (aircraft != null && fc != null) {
                        djiFlightSub.onNext(NullableObject(DJIFlight(aircraft, fc)))
                    } else {
                        djiFlightSub.onNext(NullableObject(null))
                    }
                }

    }

    private fun startSDKRegistration(context: Context) {
        KLog.i(TAG, "startSDKRegistration")
        try {

            if (!hasRegistered()) {
                DJISDKManager.getInstance().registerApp(context, managerCallback)
                KLog.d(TAG, "start register dji sdk")

            } else {
                DJISDKManager.getInstance().registerApp(context, managerCallback)
                KLog.d(TAG, "dji sdk has registered")
                isRegisterSubject.onNext(true)
                connectProduct()

            }

        } catch (e: Exception) {
            KLog.e(TAG, "DJI SDK register failed", e)
        }
    }

    private fun connectProduct() {
        DJISDKManager.getInstance().startConnectionToProduct()
    }

    private fun cameraListChange() {
        val flight = aircraftSub.value?.value
        if (flight == null || flight.cameras == null) {
            camerasSub.onNext(listOf())
        } else {
            camerasSub.onNext(flight.cameras)
        }
    }

    fun destory() {
        disposable.clear()
        djiFlightSub.value?.value?.destory()
    }

    fun checkAndRequestPermissions(activity: Activity) {
        KLog.i(TAG, "checkAndRequestPermissions")
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(activity, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration(activity)

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, missingPermission.toArray(Array(missingPermission.size, { "" })), REQUEST_PERMISSION_CODE)
        }
    }

    fun onRequestPermissionsResult(context: Context, requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        KLog.i(TAG, "onRequestPermissionsResult")
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