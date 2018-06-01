package com.kaisavx.AircraftController.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.kaisavx.AircraftController.BuildConfig
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.service.FlightService
import com.kaisavx.AircraftController.util.TCPLog
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.async
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    val TCP_LOG_PORT = 3000
    private var tcpLog : TCPLog?=null

    private val disposable = CompositeDisposable()

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

    private var missingPermission = ArrayList<String>()

    private val registerSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    val isRegister: Observable<Boolean> = registerSubject

    private val managerCallback = object : DJISDKManager.SDKManagerCallback {
        override fun onRegister(error: DJIError) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                log(this, "dji sdk success")
                DJISDKManager.getInstance().startConnectionToProduct()
            } else {
                log(this, "dji sdk failed, error: $error")

            }

            registerSubject.onNext(true)

        }

        override fun onProductChange(old: BaseProduct?, new: BaseProduct?) {
            log(this, "onProductChange, old: $old, new: $new")
            if (new != null) {
                new.setBaseProductListener(baseProductListener)
                runOnUiThread{textView.setText("connected")}
            }else{

            }
        }
    }

    private val baseProductListener = object : BaseProduct.BaseProductListener{
        override fun onComponentChange(key: BaseProduct.ComponentKey?, old: BaseComponent?, new: BaseComponent?) {
            log(this, "onComponentChange new:$new")
            new?.setComponentListener(componentListener)
        }

        override fun onConnectivityChange(flag: Boolean) {
            log(this , "onConnectivityChange $flag")
            runOnUiThread {
                if (flag) {
                    textView.setText("connected")
                } else {
                    textView.setText("disconnected")
                }
            }
        }
    }

    private val componentListener = object : BaseComponent.ComponentListener{
        override fun onConnectivityChange(p0: Boolean) {
            log(this , "onConnectivityChange $p0")
        }
    }

    //region Life-cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logMethod(this)
        setContentView(R.layout.activity_main)

        val fileDir = File(Environment.getExternalStorageDirectory(), "/AircraftController/xlog").absolutePath
        val filePath = fileDir + "/" + SimpleDateFormat("yyyy-MM-dd").format(Date())

        checkAndRequestPermissions()

        btn.setOnClickListener {
            intent = Intent (this , FlightActivity::class.java)
            startActivityOnce(intent)
        }

        if (BuildConfig.DEBUG){
            tcpLog = TCPLog(TCP_LOG_PORT,filePath)
            async { tcpLog?.createServer() }
        }

    }

    override fun onResume() {
        super.onResume()
        logMethod(this)
    }

    override fun onPause() {
        super.onPause()
        logMethod(this)
    }

    override fun onStop() {
        super.onStop()
        logMethod(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        logMethod(this)
        tcpLog?.destoryServer()
        disposable.clear()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logMethod(this)
        if(requestCode == REQUEST_PERMISSION_CODE){
            /*
            val i = permissions.indexOf(Manifest.permission.READ_PHONE_STATE)
            if (i >= 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    log(this, "request permission")
                    startSDKRegistration()
                }
            }
            */
            for(i in grantResults.size-1 .. 0){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){

                    missingPermission.remove(permissions[i])
                }
            }
        }

        if(missingPermission.isEmpty()){
            startSDKRegistration()
        }else{

        }

    }

    //function
    private fun checkAndRequestPermissions(){
        logMethod(this)
        for(eachPermission in REQUIRED_PERMISSION_LIST){
            if(ContextCompat.checkSelfPermission(this , eachPermission) != PackageManager.PERMISSION_GRANTED){
                missingPermission.add(eachPermission)
            }
        }

        if(missingPermission.isEmpty()){
            startSDKRegistration()
            val service = FlightService()
            disposable.add(service.isRegister
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it) {
                            textView.setText("connected")
                        }else{
                            textView.setText("disconnected")
                        }})

        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            log(this,"Need to grant the permissions!")
            ActivityCompat.requestPermissions(this,missingPermission.toArray(Array<String>(missingPermission.size,{""})), REQUEST_PERMISSION_CODE)
        }
    }

    private fun startSDKRegistration(){
        try {
            if (!DJISDKManager.getInstance().hasSDKRegistered()) {
                log(this, "start register dji sdk")
                DJISDKManager.getInstance().registerApp(this, managerCallback)
            }else{
                log(this, "dji sdk has registered")
                DJISDKManager.getInstance().product?.let {
                    log(this, "product exist create DJIFlight")

                }
                registerSubject.onNext(true)
            }

        }catch(e:Exception){
            log(this, "DJI SDK register failed, $e", e)

        }



    }

}
