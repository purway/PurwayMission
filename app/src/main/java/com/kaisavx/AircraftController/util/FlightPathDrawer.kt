package com.kaisavx.AircraftController.util

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.kaisavx.AircraftController.R


/**
 * Created by Abner on 2017/5/26.
 */

class FlightPathDrawer(val map: AMap, val context: Context) {
    private var lastPoint: LatLng? = null
    private var flightMarker: Marker? = null
    private var selfMarker: Marker? = null
    private var homeMarker: Marker? = null
    private var wayPointOptionMarker: Marker? = null
    private var path: Polyline? = null

    private var flyRoute:Polyline ?= null

    private var recordMarkers: ArrayList<Marker> = arrayListOf()
    private var sampleMarkers: ArrayList<Marker> = arrayListOf()

    private var selectPointMarker: Marker? = null

    var onSelectedMarkerClicked: (() -> Unit)? = null
    var onRecordMarkerClicked: ((Int) -> Unit)? = null
    var onSampleMarkerClicked: ((Int) -> Unit)? = null

    init {
        map.setOnMarkerClickListener { marker ->
            if (marker == selectPointMarker) {
                onSelectedMarkerClicked?.invoke()
                false
            } else {
                val recordIndex = recordMarkers.indexOf(marker)
                val sampleIndex = sampleMarkers.indexOf(marker)
                when {
                    recordIndex != -1 -> {
                        onRecordMarkerClicked?.invoke(recordIndex)
                        false
                    }
                    sampleIndex != -1 -> {
                        onSampleMarkerClicked?.invoke(sampleIndex)
                        false
                    }
                    else -> true
                }
            }
        }
    }

    fun initRoute(route: List<LatLng>) {
        lastPoint = route.lastOrNull()

        createPathIfNull()

        path?.points = route
    }

    fun addPoint(point: LatLng) {
        createPathIfNull()
        val p = path
        if (p != null) {
            if (lastPoint == point) {
                Log.d("Drawer", "last point")
                return
            }
            Log.d("Drawer", "add point")
            lastPoint = point
            p.points = p.points + listOf(point)
        }
    }

    fun addAirPoint(point: LatLng, number: Int): Marker {
        val icon = drawTextToBitmap(context, R.drawable.ic_record, number.toString(), 10)
        val options = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(point)
                .zIndex(99f)
        val marker = map.addMarker(options)
        recordMarkers.add(marker)
        return marker
    }

    fun addSamplePoint(point: LatLng, number: Int): Marker {
        val text = if (number == 0) "" else number.toString()
        val icon = drawTextToBitmap(context, R.drawable.ic_sample, text, 12)
        val options = MarkerOptions()
                .anchor(0.5f, 0.0f)
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(point)
                .zIndex(99f)
        val marker = map.addMarker(options)
        sampleMarkers.add(marker)
        return marker
    }

    fun setSelectPoint(point: LatLng) {
        selectPointMarker?.remove()

        val options = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_selected_point))
                .position(LatLng(point.latitude, point.longitude))
                .zIndex(99f)
        selectPointMarker = map.addMarker(options)
    }


    fun setHomeMaker(point: LatLng) {
        if (homeMarker == null) {
            val options = MarkerOptions()
                    .anchor(0.5f, 1.0f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home))
                    .position(point)
                    .zIndex(99f)
            homeMarker = map.addMarker(options)
        } else {
            homeMarker?.position = point
        }
    }

    fun removeHomeMaker() {
        homeMarker?.remove()
    }

    fun setWaypointOptionMaker(point: LatLng, altitude: Float) {
        val view = View.inflate(context, R.layout.maker_waypoint, null)
        val textLatitude = view.findViewById<TextView>(R.id.textLatitude)
        val textLongitude = view.findViewById<TextView>(R.id.textLongitude)
        val textHeihgt = view.findViewById<TextView>(R.id.textHeight)
        textLatitude.text = "${point.latitude}"
        textLongitude.text = "${point.longitude}"
        textHeihgt.text = "$altitude"
        val icon = BitmapDescriptorFactory.fromView(view)
        if (wayPointOptionMarker == null) {
            val options = MarkerOptions()
                    .anchor(0.5f, 1.0f)
                    .icon(icon)
                    .position(point)
                    .zIndex(100f)
            wayPointOptionMarker = map.addMarker(options)
        } else {
            wayPointOptionMarker?.position = point
            wayPointOptionMarker?.setIcon(icon)
            wayPointOptionMarker?.showInfoWindow()
        }
    }

    fun hideWaypointOptionMaker() {
        wayPointOptionMarker?.hideInfoWindow()
    }

    fun setFlightMarker(point: LatLng, focus: Boolean = false) {
        if (flightMarker == null) {
            val options = MarkerOptions()
            options.anchor(0.5f, 0.5f)

            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_flight))
            options.position(point)
            options.zIndex(100F)
            flightMarker = map.addMarker(options)
        } else {
            flightMarker?.position = point
        }

        if (focus) {
            val position = CameraPosition(point, 18f, 0f, 0f)
            map.animateCamera(CameraUpdateFactory.newCameraPosition(position))
        }
    }

    fun setFlightAngle(angle: Float) {
        flightMarker?.rotateAngle = angle
    }

    fun setFlyRoute(routeList:List<LatLng>){
        if(flyRoute == null){
            val options = PolylineOptions()
            options.color(ContextCompat.getColor(context,R.color.collect_disable))
            options.width(5F)
            options.zIndex(5F)
            flyRoute = map.addPolyline(options)
        }
        flyRoute?.points = routeList
    }

    fun clearFlyRoute(){
        flyRoute?.remove()
        flyRoute = null
    }

    fun clear() {
        flightMarker?.remove()
        flightMarker = null

        path?.remove()
        path = null
    }

    fun clearSelectPoint() {
        selectPointMarker?.remove()
    }

    private fun createPathIfNull() {
        if (path == null) {
            val options = PolylineOptions()
                    .width(6F)
                    .color(ContextCompat.getColor(context, R.color.red))
                    .zIndex(70F)
            path = map.addPolyline(options)
        }
    }
/*
    fun setAirRecordPoints(points: List<AirSensorPoint>) {
        recordMarkers.forEach { it.remove() }
        recordMarkers.clear()
        points.forEachIndexed { i, point ->
            addAirPoint(point.location, i + 1)
        }
    }

    fun setAirSamplePoints(points: List<AirSensorPoint>) {
        sampleMarkers.forEach { it.remove() }
        sampleMarkers.clear()
        points.forEach { point ->
            addSamplePoint(point.location, 0)
        }
    }
    */
}
