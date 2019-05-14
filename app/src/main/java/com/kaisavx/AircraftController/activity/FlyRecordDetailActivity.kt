package com.kaisavx.AircraftController.activity

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.mamager.FlyRecordManager
import com.kaisavx.AircraftController.model.*
import com.kaisavx.AircraftController.util.CommandDialog
import com.kaisavx.AircraftController.util.Share
import com.kaisavx.AircraftController.util.log
import com.kaisavx.AircraftController.util.logMethod
import kotlinx.android.synthetic.main.activity_fly_record_detail.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FlyRecordDetailActivity : BaseActivity() {

    private val commandDialog by lazy { CommandDialog(this) }

    val flyPointList: ArrayList<LatLng> = arrayListOf()
    var flyRecord: FlyRecord? = null
    var selectIndex = -1

    enum class INTENT_CODE(val value: String) {
        INDEX("index"),
    }

    //private val pathDrawer by lazy { FlightPathDrawer(mapWidget.map, this) }

    private val pointMarkers = ArrayList<PointHolder>()

    private var selectPointMarker: Marker? = null

    private var flyRoute: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logMethod(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fly_record_detail)

        initView()
        initMap(savedInstanceState)
    }

    override fun onResume() {
        logMethod(this)
        super.onResume()

        mapWidget.onResume()
        val index = intent.getIntExtra(INTENT_CODE.INDEX.value, -1)
        log(this, "index:$index")
        if (index >= 0 && index < FlyRecordManager.flyRecords.size) {

            flyRecord = FlyRecordManager.flyRecords.reversed()[index]

            flyRecord?.let { record ->
                initFlyRecord(record)
                flyPointList.clear()
                if(!record.name.isEmpty()){
                    setTitle("${record.name}")
                }else if(!record.address.isEmpty()){
                    setTitle("${record.address}")
                }else{
                    setTitle(String.format("%.6f %.6f",record.addressLatitude , record.addressLongitude))
                }

                record.flyPointList.map {
                    if (it.latitude!=null &&
                            it.longitude!=null &&
                            !it.latitude.isNaN() &&
                            !it.longitude.isNaN()) {
                        flyPointList.add(LatLng(it.latitude, it.longitude))
                    }

                }
            }

            setFlyRoute(flyPointList)
            if(flyRecord== null ||
                    flyRecord?.addressLatitude==null ||
                    flyRecord?.addressLongitude==null ||
                    flyRecord?.addressLongitude!!.isNaN() ||
                    flyRecord?.addressLatitude!!.isNaN()){
                setSelfLocation()
            }else {

                val location = LatLng(flyRecord!!.addressLatitude!!,flyRecord!!.addressLongitude!!)
                mapWidget.map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 19f))
            }

        }else{
            setSelfLocation()
        }
    }

    override fun onPause() {
        logMethod(this)
        super.onPause()
        mapWidget.onPause()
    }

    override fun onDestroy() {
        logMethod(this)
        super.onDestroy()
        mapWidget.onDestroy()
        commandDialog.destory()
    }

    override fun onLowMemory() {
        logMethod(this)
        super.onLowMemory()
        mapWidget.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        logMethod(this)
        super.onSaveInstanceState(outState)
        mapWidget.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val permission = Permission(Share.getPermission(this))

        if (permission.isFlydataSync() && Share.getIsFlydataSync(this)) {
            menuInflater.inflate(R.menu.activity_fly_record_detail_action, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val userId = Share.getUserId(this)
        val userPswd = Share.getUserPassword(this)
        when (item.itemId) {
            R.id.action_upload -> {
                if (userId == null ||
                        userPswd == null ||
                        flyRecord == null) {
                    return true
                }
                val user = User(userId, null, userPswd)
                val requestWithUserId = RequestWithUserId(user)
                flyRecord?.userId = userId
                //flyRecord?.id=null
                requestWithUserId.flyRecord = flyRecord

                commandDialog.setWaitShow(true)
                return true
            }

            R.id.action_delete -> {
                if (userId == null ||
                        userPswd == null) {

                    return true
                }
                AlertDialog.Builder(this)
                        .setTitle(R.string.msg_delete)
                        .setPositiveButton(R.string.btn_sure, { dialog, which ->

                            if(flyRecord?.id == null){
                                flyRecord?.let {
                                    FlyRecordManager.removeFlyRecord(it)
                                }
                                finish()
                               return@setPositiveButton
                            }
                            val user = User(userId, null, userPswd)
                            val requestWithUserId = RequestWithUserId(user)
                            requestWithUserId.flyRecordId = flyRecord?.id
                            commandDialog.setWaitShow(true)

                            dialog.dismiss()
                        })
                        .setNegativeButton(R.string.btn_cancel, { dialog, which ->
                            dialog.dismiss()
                        })
                        .create()
                        .show()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

    }

    private fun initView() {
        btnTaskHide.setOnClickListener {
            closeDrawer()
        }

        btnLast.setOnClickListener {
            if (selectIndex - 1 >= 0) {
                selectIndex--
                setFlyPointIndex(selectIndex)
            }
        }

        btnNext.setOnClickListener {
            flyRecord?.let { record ->
                if (selectIndex + 1 != record.flyPointList.size) {
                    selectIndex++
                    setFlyPointIndex(selectIndex)
                }
            }
        }
    }

    private fun setSelfLocation(){
        var isSetup = true
        val locationClient = AMapLocationClient(applicationContext)
        locationClient.setLocationListener {
            if(it.latitude == 0.0 || it.longitude == 0.0){
                return@setLocationListener
            }

            if(isSetup){
                isSetup = false
                val location = LatLng(it.latitude , it.longitude)
                mapWidget.map.animateCamera(CameraUpdateFactory.newLatLngZoom(location , 19.0f))

                flyRecord?.flyPointList?.map {
                    addPoint(location , it)
                }
            }
        }
        val locationOption = AMapLocationClientOption()
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
        locationClient.setLocationOption(locationOption)
        locationClient.startLocation()
    }

    private fun openDrawer() {
        layoutDetail.animate()
                .translationX(0f)
                .setDuration(500)
                .start()
    }

    private fun closeDrawer() {
        layoutDetail.animate()
                .translationX(resources.getDimension(R.dimen.fly_record_detail))
                .setDuration(500)
                .start()
    }

    private fun initMap(savedInstanceState: Bundle?) {
        mapWidget.onCreate(savedInstanceState)
        mapWidget.map.isMyLocationEnabled = false

        mapWidget.map.setOnMarkerClickListener { marker ->
            logMethod(this)

            val pointHolder = pointMarkers.find { (it.marker == marker) }

            if (pointHolder == null) {
                log(this, "pointHolder is null")
            }
            pointHolder?.let {
                val index = pointMarkers.indexOf(it)
                log(this, "index:$index")
                selectIndex = index
                setFlyPointIndex(index)
            }
            true
        }

    }

    private fun initFlyRecord(flyRecord: FlyRecord) {
        if (flyRecord.flyPointList.size > 0) {
            val points = ArrayList<FlyPoint>()
            var lastLL = LatLng(Double.NaN, Double.NaN)
            flyRecord.flyPointList.forEach {
                if (it.latitude == null ||
                        it.longitude == null ||
                        it.latitude.isNaN() ||
                        it.longitude.isNaN()) {
                    points.add(it)
                } else {
                    lastLL = LatLng(it.latitude, it.longitude)
                    if (points.size > 0) {
                        points.map {
                            addPoint(lastLL, it)
                        }
                        points.clear()
                    }

                    addPoint(lastLL, it)
                }
            }
            if (points.size > 0) {
                if(!lastLL.latitude.isNaN() &&
                        !lastLL.longitude.isNaN()) {
                    points.map {
                        addPoint(lastLL, it)
                    }
                }else{

                }
                points.clear()
            }
        } else {

        }
    }

    private fun addPoint(point: LatLng, flyPoint: FlyPoint) {
        val option = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .position(point)
                .draggable(false)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.item_air_data_point_blue))
                .zIndex(10F)
        if (flyPoint.latitude == null ||
                flyPoint.longitude == null ||
                flyPoint.latitude.isNaN() ||
                flyPoint.longitude.isNaN()) {
            option.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_virtual))
        }
        val marker = mapWidget.map.addMarker(option)
        marker.isClickable = true
        val pointHolder = PointHolder(marker, flyPoint)
        pointMarkers.add(pointHolder)
    }

    private fun setFlyPointIndex(index: Int) {
        if (index < 0) {
            return
        }

        if (index == 0) {
            btnLast.visibility = View.GONE
        } else {
            btnLast.visibility = View.VISIBLE
        }

        if (index >= pointMarkers.size - 1) {
            btnNext.visibility = View.GONE
        } else {
            btnNext.visibility = View.VISIBLE
        }
        val flyPoint = pointMarkers[index].flyPoint
        setSelectPoint(pointMarkers[index].marker.position)
        txtIndex.setText("$index")
        txtTime.setText(SimpleDateFormat("HH:mm:ss:SSS", Locale.CHINESE).format(Date(flyPoint.time)))
        txtLatitude.setText(String.format("%.10f", flyPoint.latitude))
        txtLongitude.setText(String.format("%.10f", flyPoint.longitude))
        txtAltitude.setText("${flyPoint.altitude}M")
        val hSpeed = Math.sqrt(Math.pow(flyPoint.velocityX.toDouble(), 2.0) + Math.pow(flyPoint.velocityY.toDouble(), 2.0))

        txtHSpeed.setText(String.format("%.1f M/s", hSpeed))
        txtVSpeed.setText(String.format("%.1f M/s", -flyPoint.velocityZ))

        openDrawer()
    }

    fun setFlyRoute(routeList: List<LatLng>) {
        if (flyRoute == null) {
            val options = PolylineOptions()
            options.color(ContextCompat.getColor(this, R.color.collect_disable))
            options.width(5F)
            options.zIndex(5F)
            flyRoute = mapWidget.map.addPolyline(options)
        }
        flyRoute?.points = routeList
    }

    fun setSelectPoint(point: LatLng) {
        selectPointMarker?.remove()

        val options = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.item_select))
                .position(LatLng(point.latitude, point.longitude))
                .zIndex(99f)
        selectPointMarker = mapWidget.map.addMarker(options)
    }


    inner class PointHolder(val marker: Marker, val flyPoint: FlyPoint)
}