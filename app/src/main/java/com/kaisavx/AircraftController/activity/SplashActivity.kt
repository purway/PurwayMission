package com.kaisavx.AircraftController.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Window
import android.view.WindowManager
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.util.logMethod
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity: Activity(){

    private val REQUEST_CODE = 0x0001
    private val DELAY_TIME = 3000L
    private var flag = true

    private val handler= Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        logMethod(this)
        setContentView(R.layout.activity_splash)
        handler.postDelayed({
            toMain()
        },DELAY_TIME)

        initView()
    }

    override fun onStart() {
        super.onStart()
        logMethod(this)
    }

    override fun onResume() {
        super.onResume()
        logMethod(this)
    }

    override fun onPause() {
        super.onPause()
        logMethod(this)
    }

    override fun onRestart() {
        super.onRestart()
        logMethod(this)
    }

    override fun onStop() {
        super.onStop()
        logMethod(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        logMethod(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    private fun initView(){
        imageView.setOnClickListener {
            toMain()
        }
    }

    private fun toMain(){
        val it = Intent(this, MainActivity::class.java)
        if (flag) {
            flag = false
            this.startActivityForResult(it, REQUEST_CODE)
        }
    }
}