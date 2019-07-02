package com.kaisavx.AircraftController.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.module.dji.DJIManager
import com.kaisavx.AircraftController.module.dji.DJIUtil
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.NullableObject
import com.kaisavx.AircraftController.util.plusAssign
import dji.common.product.Model
import dji.keysdk.KeyManager
import dji.keysdk.RemoteControllerKey
import dji.sdk.camera.Camera
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_connect.*
import java.util.concurrent.TimeUnit

class ConnectFragment : Fragment() {
    companion object {
        private val TAG = "ConnectFragment"
    }

    private val disposable = CompositeDisposable()
    private val connectingDispoasble = CompositeDisposable()
    private val deviceDisposable = CompositeDisposable()

    private val rcTipsDialog by lazy {
        AlertDialog.Builder(context)
                .setTitle(R.string.title_tips)
                .setMessage(R.string.dialog_rc_disconnect)
                .setNegativeButton(R.string.btn_known, null)
                .create()
    }

    private val flightTipsDialog by lazy {
        AlertDialog.Builder(context)
                .setTitle(R.string.title_tips)
                .setMessage(R.string.dialog_flight_disconnect)
                .setNegativeButton(R.string.btn_known, null)
                .create()
    }

    private val deviceTipsDialog by lazy {
        AlertDialog.Builder(context)
                .setTitle(R.string.title_tips)
                .setMessage("message")
                .setNegativeButton(R.string.btn_known, null)
                .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        KLog.i(TAG, "onCreateView")
        return inflater.inflate(R.layout.fragment_connect, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        KLog.i(TAG, "onActivityCreated")
        super.onActivityCreated(savedInstanceState)
        setupView()
    }

    override fun onDestroy() {
        KLog.i(TAG, "onDestroy")
        super.onDestroy()
        disposable.clear()
        deviceDisposable.clear()
        connectingDispoasble.clear()
    }

    private fun setupView() {
        btnRCTips.setOnClickListener {
            rcTipsDialog.show()
        }

        btnFlightTips.setOnClickListener {
            flightTipsDialog.show()
        }

        btnDeviceTips.setOnClickListener {
            deviceTipsDialog.show()
        }
    }

    fun setupDJIManager(manager: DJIManager) {
        KLog.i(TAG, "setupDJIManager")
        disposable.clear()

        disposable += manager.rcOb
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val rc = it.value
                    KLog.d(TAG, "rcOb subcribe rc:$rc")
                    if (rc != null) {
                        rcTipsDialog.dismiss()
                        latRCDisconnect.visibility = View.GONE
                        latRCConnected.visibility = View.VISIBLE
                        latFlightStatus.visibility = View.VISIBLE
                        txtRCConnected.setText(R.string.rc_connected)
                        val displayNameKey = RemoteControllerKey.create(RemoteControllerKey.DISPLAY_NAME)
                        val o = KeyManager.getInstance().getValue(displayNameKey)
                        if (o is String) {
                            val name = o.toString()
                            KLog.d(TAG, "rc name:$name")
                            txtRCConnected.text = DJIUtil.getRCDisplayName(name)
                        }
                    } else {
                        latRCDisconnect.visibility = View.VISIBLE
                        latRCConnected.visibility = View.GONE
                        latFlightStatus.visibility = View.GONE
                    }
                }

        val fob: Observable<Triple<NullableObject<Aircraft>, NullableObject<FlightController>, List<Camera>>> = Observable.combineLatest(
                manager.aircraftOb,
                manager.fcOb,
                manager.camerasOb,
                Function3 { f, l, d -> Triple(f, l, d) }
        )
        disposable += fob
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val flight = it.first.value
                    val fc = it.second.value
                    val cameras = it.third
                    KLog.d(TAG, "fob subscribe flight:$flight fc:$fc camears:$cameras")
                    if (flight != null && flight.model != null && fc != null) {
                        flightTipsDialog.dismiss()
                        latFlightConnected.visibility = View.VISIBLE
                        latFlightDisconnect.visibility = View.GONE

                        val model = flight.model
                        KLog.d(TAG, "flight name:${model?.displayName}")
                        model ?: return@subscribe
                        var name = "${DJIUtil.getFlightDisplayName(model)}"
                        when (model) {
                            Model.MATRICE_100,

                            Model.MATRICE_200,
                            Model.MATRICE_210,
                            Model.MATRICE_210_RTK,

                            Model.MATRICE_PM420,
                            Model.MATRICE_PM420PRO,
                            Model.MATRICE_PM420PRO_RTK,

                            Model.MATRICE_600,
                            Model.MATRICE_600_PRO,

                            Model.INSPIRE_1,
                            Model.INSPIRE_1_PRO,
                            Model.INSPIRE_1_RAW,

                            Model.INSPIRE_2,
                            Model.A3,
                            Model.N3 -> {
                                cameras.forEach { camera ->
                                    camera.displayName?.let { displayName ->

                                        DJIUtil.getCameraDisplayName(displayName)?.let { cameraName ->
                                            name += " + $cameraName"
                                        }
                                    }
                                    KLog.d(TAG, "camera name:${camera.displayName}")
                                }
                            }
                        }

                        txtFlightConnected.text = name
                    } else {
                        latFlightConnected.visibility = View.GONE
                        latFlightDisconnect.visibility = View.VISIBLE
                    }
                }

        disposable += manager.fcOb
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val fc = it.value
                    KLog.d(TAG, "fcOb  fc:$fc")
                    if (fc != null) {
                        latDeviceStatus.visibility = View.VISIBLE
                        startDeviceConnect()
                    } else {
                        latDeviceStatus.visibility = View.GONE
                        stopDeviceConnect()
                    }
                }

        disposable += manager.djiFlightOb
                .observeOn(Schedulers.computation())
                .subscribe {
                    val flight = it.value
                    deviceDisposable.clear()
                    if (flight != null) {
                        deviceDisposable += flight.onBoardProcessor.deviceOb
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    val device = it.value
                                    if (device != null) {
                                        txtDeviceConnected.text = device.getDisplayName()
                                        connectingDispoasble.clear()
                                        latDeviceConnecting.visibility = View.GONE
                                        latDeviceDisconnect.visibility = View.GONE
                                        latDeviceConnected.visibility = View.VISIBLE
                                    } else {
                                        latDeviceConnecting.visibility = View.GONE
                                        latDeviceDisconnect.visibility = View.VISIBLE
                                        latDeviceConnected.visibility = View.GONE
                                    }
                                }
                    }
                }
    }

    private fun startDeviceConnect() {
        KLog.i(TAG, "startDeviceConnect")
        var connectCount = 60
        latDeviceConnecting.visibility = View.VISIBLE
        latDeviceDisconnect.visibility = View.GONE
        latDeviceConnected.visibility = View.GONE
        txtDeviceConnecting.text = getString(R.string.device_connecting, connectCount)
        connectingDispoasble.clear()
        connectingDispoasble += Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (--connectCount == 0) {
                        stopDeviceConnect()
                    } else {
                        txtDeviceConnecting.text = getString(R.string.device_connecting, connectCount)
                    }
                }
    }

    private fun stopDeviceConnect() {
        KLog.i(TAG, "stopDeviceConnect")
        connectingDispoasble.clear()
        latDeviceConnecting.visibility = View.GONE
        latDeviceDisconnect.visibility = View.VISIBLE
        latDeviceConnected.visibility = View.GONE
    }
}