package com.kaisavx.AircraftController.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.Menu
import android.widget.BaseAdapter
import android.widget.TextView
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.mamager.FlyRecordManager
import com.kaisavx.AircraftController.model.FlyRecord
import com.kaisavx.AircraftController.model.Permission
import com.kaisavx.AircraftController.model.RequestWithUserId
import com.kaisavx.AircraftController.model.User
import com.kaisavx.AircraftController.util.CommandDialog
import com.kaisavx.AircraftController.util.Share
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_fly_record.*
import java.text.SimpleDateFormat
import java.util.*

class FlyRecordListActivity : BaseActivity() {

    private val disposable = CompositeDisposable()

    private val flyRecordAdapter by lazy {
        FlyRecordAdapter(this)
    }
    private val commandDialog by lazy{CommandDialog(this)}

    override fun onCreate(savedInstanceState: Bundle?) {
        logMethod(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fly_record)
        //test()
        initView()
        initDisposable()
    }

    override fun onResume() {
        logMethod(this)
        FlyRecordManager.reload()
        flyRecordAdapter.notifyDataSetChanged()
        super.onResume()
    }

    override fun onPause() {
        logMethod(this)
        super.onPause()
    }

    override fun onRestart() {
        logMethod(this)
        super.onRestart()
    }

    override fun onDestroy() {
        logMethod(this)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val permission = Permission(Share.getPermission(this))

        if (permission.isFlydataSync() && Share.getIsFlydataSync(this)) {
            menuInflater.inflate(R.menu.activity_fly_record_list_action, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sync -> {

                val userId = Share.getUserId(this)
                val userPswd = Share.getUserPassword(this)
                val permission = Permission(Share.getPermission(this))
                if (!permission.isFlydataSync() ||
                        !Share.getIsFlydataSync(this) ||
                        userId == null ||
                        userPswd == null) {
                    return true
                }

                val user = User(userId, null, userPswd)
                val requestWithUserId = RequestWithUserId(user)
                requestWithUserId.skip = 0
                requestWithUserId.limit = 100
                commandDialog.setWaitShow(true)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }
/*
    fun test() {
        val flyRecord = FlyRecord(0)
        flyRecord.name = "test2"
        flyRecord.aircraftNo ="13456"
        flyRecord.startTime = System.currentTimeMillis()
        flyRecord.stopTime = System.currentTimeMillis() + 1000

        val point1 = FlyPoint(
                null,
                null,
                10f,
                0f,
                0f,
                0f,
                System.currentTimeMillis()
        )

        flyRecord.flyPointList.add(point1)

        val point2 = FlyPoint(
                null,
                null,
                10f,
                0f,
                0f,
                0f,
                System.currentTimeMillis()
        )

        flyRecord.flyPointList.add(point2)

        val firstPoint = FlyPoint(
                22.56,
                113.97,
                10f,
                0f,
                0f,
                0f,
                System.currentTimeMillis()
        )
        flyRecord.flyPointList.add(firstPoint)

        val secondPoint = FlyPoint(
                22.5,
                113.9,
                10f,
                0f,
                0f,
                0f,
                System.currentTimeMillis()
        )
        flyRecord.flyPointList.add(secondPoint)

        val point3 = FlyPoint(
                null,
                null,
                10f,
                0f,
                0f,
                0f,
                System.currentTimeMillis()
        )

        flyRecord.flyPointList.add(point3)

        flyRecord.save()

    }
*/
    fun initView() {
        listView.adapter = flyRecordAdapter
        listView.setOnItemClickListener { parent, view, position, id ->
            log(this, "position:$position")
            intent = Intent(this, FlyRecordDetailActivity::class.java)
            intent.putExtra(FlyRecordDetailActivity.INTENT_CODE.INDEX.value, position)
            startActivityOnce(intent)
        }

        listView.setOnItemLongClickListener { parent, view, position, id ->

            AlertDialog.Builder(this)
                    .setTitle(R.string.msg_delete)
                    .setPositiveButton(R.string.btn_sure,{dialog, which ->
                        val flyRecord = flyRecordAdapter.getItem(position)
                        val userId = Share.getUserId(this)
                        val userPswd = Share.getUserPassword(this)
                        val permission = Permission(Share.getPermission(this))
                        if (!permission.isFlydataSync() ||
                                !Share.getIsFlydataSync(this) ||
                                userId == null ||
                                userPswd == null ||
                                flyRecord.id == null) {
                            FlyRecordManager.removeFlyRecord(flyRecord)
                            flyRecordAdapter.notifyDataSetChanged()
                            dialog.dismiss()
                             return@setPositiveButton
                        }

                        val user = User(userId, null, userPswd)
                        val requestWithUserId = RequestWithUserId(user)
                        requestWithUserId.flyRecordId=flyRecord.id
                        commandDialog.setWaitShow(true)
                        dialog.dismiss()
                    })
                    .setNegativeButton(R.string.btn_cancel,{dialog, which ->
                        dialog.dismiss()
                    })
                    .create()
                    .show()

            true
        }
    }

    private fun initDisposable() {
    }


    inner class FlyRecordAdapter(val context: Context) : BaseAdapter() {
        override fun getCount(): Int {
            return FlyRecordManager.flyRecords.count()
        }

        override fun getItem(position: Int): FlyRecord {
            return FlyRecordManager.flyRecords.reversed()[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_fly_record, null)

            val flyRecord = getItem(position)
            val viewHolder: ViewHolder
            if (view.tag == null) {
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            if (flyRecord.address.isEmpty()) {
                viewHolder.txtAddress.setText(String.format("%.6f %.6f", flyRecord.addressLatitude, flyRecord.addressLongitude))
            } else {
                viewHolder.txtAddress.setText(flyRecord.address)
            }
            if(flyRecord.name.isEmpty()){
                viewHolder.txtName.setText(viewHolder.txtAddress.text)
            }else{
                viewHolder.txtName.setText(flyRecord.name)
            }


            val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE).format(Date(flyRecord.startTime))
            val stopTime = SimpleDateFormat("HH:mm", Locale.CHINESE).format(Date(flyRecord.stopTime))
            viewHolder.txtTime.setText("$startTime ~ $stopTime")

            return view
        }

        inner class ViewHolder(val view: View) {
            val txtName by lazy { view.findViewById<TextView>(R.id.txtName) }
            val txtAddress by lazy { view.findViewById<TextView>(R.id.txtAddress) }
            val txtTime by lazy { view.findViewById<TextView>(R.id.txtTime) }
        }
    }
}