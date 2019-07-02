package com.kaisavx.AircraftController.activity

import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.view.Menu
import android.widget.*
import com.kaisavx.AircraftController.BuildConfig
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.adapter.SettingAdapter
import com.kaisavx.AircraftController.dialog.DetailAlterDialog
import com.kaisavx.AircraftController.dialog.ProductDialog
import com.kaisavx.AircraftController.dialog.UserActivationDialog
import com.kaisavx.AircraftController.dialog.UserDetailDialog
import com.kaisavx.AircraftController.fragment.ConnectFragment
import com.kaisavx.AircraftController.fragment.LogFragment
import com.kaisavx.AircraftController.module.dji.DJIAccountManager
import com.kaisavx.AircraftController.module.dji.DJIManager
import com.kaisavx.AircraftController.service.FlightService
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.Share
import com.kaisavx.AircraftController.util.VersionUtil
import com.kaisavx.AircraftController.util.plusAssign
import dji.common.error.DJIError
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_detail_alter.*
import java.util.HashMap
import kotlin.collections.ArrayList


class MainActivity : BaseActivity() {
    companion object {
        private val TAG = "MainActivity"
    }

    private val disposable = CompositeDisposable()

    private val settingList = ArrayList<HashMap<String, Any>>()
    private val settingAdapter by lazy { SettingAdapter(this, settingList) }

    private val waitSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    private val loginHashMap by lazy {
        hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_IMAGE_TEXT,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.setting_login)
        )
    }

    private val logoutHashMap by lazy {
        hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_IMAGE_TEXT,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.setting_logout)
        )
    }

    private val popupView by lazy {
        layoutInflater.inflate(R.layout.pop_user_option, null)
    }

    private val popupWindow by lazy {

        val layoutUser = popupView.findViewById<LinearLayout>(R.id.layoutUser)
        layoutUser.measure(0, 0)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
        popupWindow.isFocusable = true
        popupWindow.isTouchable = true
        popupWindow.isOutsideTouchable = true

        popupWindow.animationStyle = R.style.mypopwindow_anim_style
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        popupWindow.setOnDismissListener {
            val lp = window.attributes
            lp.alpha = 1f
            window.attributes = lp
        }

        popupWindow.setTouchInterceptor { v, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                popupWindow.dismiss()
                true
            } else {
                false
            }
        }
        popupWindow
    }

    private val djiAccountManager = DJIAccountManager()

    private val showLoginDialog by lazy {
        AlertDialog.Builder(this)
                .setMessage(R.string.msg_must_login_error)
                .setPositiveButton(R.string.btn_login, { dialogInterface, i ->
                    dialogInterface.dismiss()
                    login()
                })
                .setNegativeButton(R.string.btn_cancel, { dialogInterface, i ->
                    dialogInterface.dismiss()
                })
                .create()
    }
    private val networkErrorDialog by lazy {
        AlertDialog.Builder(this)
                .setMessage(R.string.msg_network_error)
                .setPositiveButton(R.string.btn_open, { dialog, which ->
                    dialog.dismiss()
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
    }

    private val textUserName by lazy {
        popupView.findViewById<TextView>(R.id.textUserName)
    }

    private val activationCick: ((Dialog) -> Unit) = {
        val productId = Share.getUserProductId(this)
        if (productId == null) {
            userActivationDialog.show()
        } else {
            val id = Share.getUserId(this)
            val password = Share.getUserPassword(this)
            if (id != null && password != null) {
            }
        }
    }

    private val userDetialDialog by lazy {
        UserDetailDialog.Builder(this)
                .setTitle(R.string.dialog_user_detail_title)
                .setOnAlterDetailClickListener {
                    showAlterDialog()
                }
                /*
                .setOnAlterPswdClickListener {
                    userAlterDialog.show()
                    userAlterDialog.sprPrefixer?.isSelected = false
                    userAlterDialog.edtPhone?.isEnabled = false
                    val phone = Share.getUserPhone(this)
                    userAlterDialog.edtPhone?.setText(phone)
                }
                */
                .setOnActivationClickListener(activationCick)
                .create()
    }

    private val detailAlterDialog by lazy {
        DetailAlterDialog.Builder(this)
                .setTitle(R.string.dialog_detail_alter_title)
                .setOnSureClickListener { dialog, name, company ->
                    val id = Share.getUserId(this)
                    val password = Share.getUserPassword(this)
                    if (id != null && password != null) {
                    }
                }
                .create()
    }

    private val userActivationDialog by lazy {
        UserActivationDialog.Builder(this)
                .setTitle(R.string.dialog_user_activation_title)
                .setOnSureClickListener { dialog, code ->
                    val id = Share.getUserId(this)
                    val password = Share.getUserPassword(this)
                    if (id != null && password != null) {
                    }
                }
                .create()
    }

    private val productDialog by lazy {
        ProductDialog.Builder(this)
                .setTitle(R.string.dialog_product_detail_title)
                .setOnReactivationClickListener {
                    it.dismiss()
                    userActivationDialog.show()
                }
                .create()
    }

    private val djiRegisterDialog by lazy {
        AlertDialog.Builder(this)
                .setTitle(R.string.alert_register_failed)
                .setPositiveButton(R.string.btn_open, { dialog, which ->
                    dialog.dismiss()
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
    }

    private val fgmLog by lazy {
        supportFragmentManager.findFragmentById(R.id.fgmLog) as LogFragment
    }
    private val fgmConnect by lazy{
        supportFragmentManager.findFragmentById(R.id.fmgConnect) as ConnectFragment
    }

    private var djiManager: DJIManager? = null

    private val flightConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            val service = (binder as FlightService.LocalBinder).service
            setupDJIManager(service.djiManager)
        }

        override fun onServiceDisconnected(className: ComponentName) {

        }
    }

    //region Life-cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        KLog.i( TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setupView()
        setupDisposable()
        setupDJIAccountManager()

        bindService(Intent(this, FlightService::class.java), flightConnection, Context.BIND_AUTO_CREATE)

        if (!BuildConfig.DEBUG) {
            fgmLog.hide()
        }

    }

    override fun onStart() {
        KLog.i( TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        KLog.i( TAG, "onResume")
        super.onResume()
        checkRegister()
    }

    override fun onPause() {
        KLog.i( TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        KLog.i( TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        KLog.i( TAG, "onDestroy")
        djiAccountManager.destory()
        disposable.clear()
        super.onDestroy()
        unbindService(flightConnection)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        KLog.i( TAG, "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        djiManager?.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.activity_main_action, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                //Toast.makeText(this,"action_setting",Toast.LENGTH_SHORT).show()

                val lp = window.attributes
                lp.alpha = 0.5f
                window.attributes = lp
                popupWindow.showAtLocation(layoutMain, Gravity.END, 0, 0)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

    }
/*
    override fun onBackPressed() {
        finish()
    }
*/
    private fun setupView() {
        btnFlyNow.setOnClickListener {
            intent = Intent(this, FlightActivity::class.java)
            startActivityOnce(intent)
        }

        btnFlyRecord.setOnClickListener {
            intent = Intent(this, FlyRecordListActivity::class.java)
            startActivityOnce(intent)
        }

        setupPopupView()
    }

    private fun setupPopupView() {

        val layoutUser = popupView.findViewById<ViewGroup>(R.id.layoutUser)
        layoutUser.setOnClickListener {
            val user = Share.getUser(this)
            user?.let {
                userDetialDialog.setUser(it)
                userDetialDialog.show()
            }
        }

        val listViewSetting = popupView.findViewById<ListView>(R.id.listViewSetting)
        listViewSetting.adapter = settingAdapter

        listViewSetting.setOnItemClickListener { parent, view, position, id ->
            val map = settingList[position]
            when (map[SettingAdapter.KEY_TITLE]) {
                resources.getString(R.string.setting_fly_book) -> {
                    startActivityOnce(MenusActivity.intent(this, MenusActivity.MENU_TYPE_BOOK, resources.getString(R.string.setting_fly_book)))
                }
                resources.getString(R.string.setting_fly_record) -> {
                    startActivityOnce(Intent(this, FlyRecordListActivity::class.java))
                }
                resources.getString(R.string.setting_version_check) -> {
                    Toast.makeText(this, "code:${VersionUtil.getLocalVersion(this)} name:${VersionUtil.getLocalVersionName(this)}", Toast.LENGTH_LONG).show()
                }
                resources.getString(R.string.setting_call_us) -> {
                    startActivityOnce(Intent(this, CallUsActivity::class.java))
                }
                resources.getString(R.string.setting_login) -> {
                    login()
                }
                resources.getString(R.string.setting_logout) -> {
                    logout()
                }

            }
        }

        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_IMAGE_TEXT,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.setting_fly_book)
        ))

        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_IMAGE_TEXT,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.setting_fly_record)
        ))

        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_IMAGE_TEXT,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.setting_version_check),
                SettingAdapter.KEY_DATA_TEXT to VersionUtil.getLocalVersionName(this)
        ))

        settingList.add(hashMapOf(
                SettingAdapter.KEY_TYPE to SettingAdapter.ITEM_IMAGE_TEXT,
                SettingAdapter.KEY_TITLE to resources.getString(R.string.setting_call_us)
        ))
        settingList.add(logoutHashMap)

        val userAccount = Share.getUserAccount(this)
        updateUser(userAccount)

    }

    private fun setupDisposable() {
        disposable.add(waitSubject
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe {
                    if (it) {
                        waitDialog.show()
                    } else {
                        waitDialog.dismiss()
                    }
                })
    }

    private fun setupDJIAccountManager() {
        disposable += djiAccountManager.accountOb
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { account ->
                    waitSubject.onNext(false)
                    updateUser(account)
                }

        disposable += djiAccountManager.accountStateOb
                .observeOn(Schedulers.computation())
                .subscribe {
                    login()
                }

        disposable += djiAccountManager.loginErrorOb
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { error ->
                    waitSubject.onNext(false)
                    if (error != null && error == DJIError.NO_NETWORK) {
                        networkErrorDialog.show()
                    } else {
                        showLoginDialog.show()
                    }
                }

        disposable += djiAccountManager.showLoginOb
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    waitSubject.onNext(false)
                    djiAccountManager.showLoginDialog(this)
                }

    }

    private fun setupDJIManager(manager: DJIManager) {
        djiManager = manager
        disposable += manager.isRegisterOb
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it) {
                        waitSubject.onNext(true)
                        djiAccountManager.checkLogin()
                    } else {
                        djiRegisterDialog.show()
                    }
                }
        manager.checkAndRequestPermissions(this)

        fgmConnect.setupDJIManager(manager)
    }

    private fun checkRegister() {
        djiManager?.let {
            KLog.i( TAG, "checkRegister")
            if (!DJIManager.hasRegistered()) {
                it.checkAndRequestPermissions(this)
            } else {
                waitSubject.onNext(true)
                djiAccountManager.getLoginState()
            }
        }
    }

    private fun showAlterDialog() {
        detailAlterDialog.show()
        val name = Share.getUserName(this)
        val company = Share.getUserCompany(this)
        detailAlterDialog.edtName.setText(name)
        detailAlterDialog.edtCompany.setText(company)
    }

    private fun updateUser(account: String?) {
        Share.setUserAccount(this, account)
        if (account == null) {
            textUserName?.text = resources.getString(R.string.user_logout)
            settingList.removeAt(settingList.size - 1)
            settingList.add(loginHashMap)
            settingAdapter.notifyDataSetChanged()
        } else {
            textUserName?.text = account
            settingList.removeAt(settingList.size - 1)
            settingList.add(logoutHashMap)
            settingAdapter.notifyDataSetChanged()
        }
    }

    private fun login() {
        djiAccountManager.login()
    }

    private fun logout() {
        djiAccountManager.logout()
        runOnUiThread {
            updateUser(null)
        }
    }
}