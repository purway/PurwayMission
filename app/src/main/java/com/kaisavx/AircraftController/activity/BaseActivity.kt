package com.kaisavx.AircraftController.activity

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import com.kaisavx.AircraftController.R

abstract class BaseActivity : AppCompatActivity() {


    private var startingActivity = false

    private var loadingDialog: Dialog? = null
    private var isLoading: Boolean = false

    val waitDialog by lazy {
        //log(this , "waitDialog create:$this")
        val dialog = ProgressDialog(this)
        dialog.setMessage(resources.getString(R.string.msg_waitting))
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)

        val name = NavUtils.getParentActivityName(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(name != null)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    }

    fun startActivityOnce(intent: Intent) {
        if (!startingActivity) {
            startActivity(intent)
            startingActivity = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        startingActivity = false
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun showLoading(loading: Boolean) {
        if (loading == this.isLoading) {
            return
        }
        if (loading) {
            loadingDialog?.dismiss()
            loadingDialog = ProgressDialog.show(this, null, "加载中")
        } else {
            loadingDialog?.dismiss()
        }
        this.isLoading = loading
    }


}