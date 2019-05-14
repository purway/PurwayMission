package com.kaisavx.AircraftController.activity

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.Menu
import android.widget.*
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.adapter.SettingAdapter
import com.kaisavx.AircraftController.dialog.DetailAlterDialog
import com.kaisavx.AircraftController.dialog.ProductDialog
import com.kaisavx.AircraftController.dialog.UserActivationDialog
import com.kaisavx.AircraftController.dialog.UserDetailDialog
import com.kaisavx.AircraftController.mamager.DJIManager2
import com.kaisavx.AircraftController.util.Share
import com.kaisavx.AircraftController.util.VersionUtil
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_detail_alter.*
import java.util.HashMap
import kotlin.collections.ArrayList


class MainActivity : BaseActivity() {

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
                .setOnAlterDetailClickListener{
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
                .setNegativeButton(R.string.btn_sure, { dialog, which ->
                    dialog.dismiss()
                })
                .create()
    }

    //region Life-cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logMethod(this)
        setContentView(R.layout.activity_main)

        DJIManager2.checkAndRequestPermissions(this)
        initView()
        initDisposable()

        //test()

    }

    override fun onStart() {
        super.onStart()
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
        disposable.clear()
        super.onDestroy()
        logMethod(this)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logMethod(this)

        DJIManager2.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
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

    override fun onBackPressed() {
        logMethod(this)
        finish()
    }

    private fun initView() {
        btnFlyNow.setOnClickListener {
            intent = Intent(this, FlightActivity::class.java)
            startActivityOnce(intent)
        }

        btnFlyRecord.setOnClickListener {
            intent = Intent(this , FlyRecordListActivity::class.java)
            startActivityOnce(intent)
        }

        initPopupView()
    }

    private fun initPopupView() {

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

    private fun initDisposable() {
        disposable.add(waitSubject
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe {
                    if(it){
                        waitDialog.show()
                    }else{
                        waitDialog.dismiss()
                    }
                })

        disposable.add(DJIManager2.isRegisterSubject
                .observeOn(Schedulers.computation())
                .distinctUntilChanged()
                .subscribe {
                    if (it) {
                        waitSubject.onNext(true)
                        DJIManager2.checkLogin()
                    } else {
                        djiRegisterDialog.show()
                    }
                })
        disposable.add(DJIManager2.userAccountStateSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    log(this,"userAccountState:$it")
                    login()
                })
        disposable.add(DJIManager2.isConnectedSubject
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe {
                    if (it) {
                        deviceState.setText(R.string.device_connected)
                        deviceState.setTextColor(Color.GREEN)
                    } else {
                        deviceState.setText(R.string.device_disconnect)
                        deviceState.setTextColor(Color.RED)
                    }
                })

        disposable.add(DJIManager2.accountSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    log(this,"account:$it")
                    waitSubject.onNext(false)
                    showLoginDialog.dismiss()
                    updateUser(it)
                })

        disposable.add(DJIManager2.flightControllerSubejct
                .subscribe {
                    DJIManager2.configFlightController(this, it)
                })

    }

    private fun showAlterDialog(){
        detailAlterDialog.show()
        val name = Share.getUserName(this)
        val company = Share.getUserCompany(this)
        detailAlterDialog.edtName.setText(name)
        detailAlterDialog.edtCompany.setText(company)
    }

    private fun updateUser(account:String?){
        Share.setUserAccount(this,account)
        if(account==null){
            textUserName?.text = resources.getString(R.string.user_logout)
            settingList.removeAt(settingList.size - 1)
            settingList.add(loginHashMap)
            settingAdapter.notifyDataSetChanged()
        }else{
            textUserName?.text = account
            settingList.removeAt(settingList.size - 1)
            settingList.add(logoutHashMap)
            settingAdapter.notifyDataSetChanged()
        }
    }

    private fun login(){
        DJIManager2.login(this,{
            runOnUiThread {
                showLoginDialog.show()
            }
        })
    }

    private fun logout() {
        runOnUiThread {
            updateUser(null)
        }
        DJIManager2.logout()
    }
}