package com.kaisavx.AircraftController.controller

import android.content.Context
import android.graphics.Point
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.*
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.util.log
import dji.common.mission.waypoint.Waypoint
import dji.common.model.LocationCoordinate2D

/**
 * Created by windless on 2017/9/18.
 */
class WayHolder(val marker: Marker, var wayPoint: Waypoint)

class MapMissionController(val map: AMap, val context: Context) : AMap.OnMapTouchListener {
    private var wayMarkers = listOf<WayHolder>()
    private var areaMarkers = listOf<WayHolder>()
    private var movingMarker: WayHolder? = null

    private var selectMarker:Marker?=null
    private var polygon: Polygon? = null
    private var route: Polyline? = null


    private var touchDownPoint: Point? = null

    private var interval = 20

    private var type: ControlType = ControlType.Area
    private var isTouchable = false

    var onWayPointUpdated: ((List<Waypoint>) -> Unit)? = null
    
    var onAreaPointUpdated:((List<LatLng>) -> Unit) ?= null
    var onMarkerUpdated: ((List<LatLng>) -> Unit)? = null
    
    var onMarkerSelect:((WayHolder?) ->Unit) ?= null
    var onNewWaypoint:((LatLng) -> Waypoint)?=null


    var moveCount = 0

    enum class ControlType {
        Path, Area
    }

    enum class State {
        None, MovingMarker, AddMarker, AddArea
    }

    private var state = State.None

    init {
        map.setOnMapTouchListener(this)
        map.setOnMapLongClickListener {
            removeMarker(it)
        }


    }

    fun setType(type: ControlType) {
        this.type = type
    }

    fun setTouchable(working: Boolean) {
        isTouchable = working
    }

    fun removeMarker(location: LatLng) {
        if (type != ControlType.Path) {
            return
        }

        wayMarkers.find {
            val markerPoint = map.projection.toScreenLocation(it.marker.position)
            val point = map.projection.toScreenLocation(location)

            Math.abs(markerPoint.x - point.x) < 20 && Math.abs(markerPoint.y - point.y) < 20
        }?.let {
            it.marker.remove()
            wayMarkers -= it

            onWayPointUpdated?.invoke(wayMarkers.map { it.wayPoint })
            updatePath()
            removeSelectMarker()
            movingMarker = null
            onMarkerSelect?.invoke(movingMarker)
        }

        
    }

    override fun onTouch(event: MotionEvent) {
        //logMethod(this)
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
               // log(this , "ACTION_DOWN")
                moveCount = 0
                if (state != State.None) {
                    return
                }

                if (!isTouchable) {
                    return
                }

                touchDownPoint = Point(x.toInt(), y.toInt())

                if (type == ControlType.Area) {
                    val marker = getAreaMarker(x, y)
                    if (marker != null) {
                        state = State.MovingMarker
                        map.uiSettings.isScrollGesturesEnabled = false
                        movingMarker = marker

                        return
                    }
                    if (polygon == null) {
                        state = State.AddArea
                    }
                } else {
                    val marker = getWayMarker(x, y)
                    if (marker != null) {
                        state = State.MovingMarker
                        map.uiSettings.isScrollGesturesEnabled = false
                        movingMarker = marker

                        return
                    }
                    state = State.AddMarker
                }

            }
            MotionEvent.ACTION_MOVE -> {
                //log(this , "ACTION_MOVE")
                if(moveCount++ < 5)return
                val tp = touchDownPoint ?: return

                if (state == State.AddMarker) {
                    if (Math.abs(tp.x - x) > 10 || Math.abs(tp.y - y) > 10) {
                        state = State.None
                    }
                }

                if (state != State.MovingMarker) {
                    return
                }

                val marker = movingMarker ?: return
                if (type == ControlType.Area) {

                    polygon?.let {
                        val points = areaMarkers.map { map.projection.toScreenLocation(it.marker.position) }.toMutableList()
                        points[areaMarkers.indexOf(marker)] = Point(x.toInt(), y.toInt())
                        if (isConvex(points)) {
                            val markerPosition = map.projection.fromScreenLocation(Point(x.toInt(), y.toInt()))
                            val position = map.projection.fromScreenLocation(Point(x.toInt(), y.toInt()))
                            marker.marker.position = markerPosition
                            updatePolygon()
                        }
                    }
                    onMarkerUpdated?.invoke(areaMarkers.map { it.marker.position })
                } else {
                    val markerPosition = map.projection.fromScreenLocation(Point(x.toInt(), y.toInt()))
                    val position = map.projection.fromScreenLocation(Point(x.toInt(), y.toInt()))

                    movingMarker?.marker?.position = markerPosition
                    movingMarker?.wayPoint?.coordinate = LocationCoordinate2D(position.latitude , position.longitude)
                    onMarkerSelect?.invoke(movingMarker)
                    setSelectMarker(position)
                    updatePath()
                }

            }
            MotionEvent.ACTION_UP -> {
                //log(this , "ACTION_UP")
                if (type == ControlType.Area) {

                }else {
                    when (state) {
                        State.AddMarker -> {
                            touchDownPoint?.let {
                                val position = map.projection.fromScreenLocation(it)
                                /*
                                val wayPoint = Waypoint(ll.latitude, ll.longitude, 75f)
                                wayPoint.heading = 0
                                wayPoint.cornerRadiusInMeters = 0.2f
                                wayPoint.turnMode = WaypointTurnMode.CLOCKWISE
                                wayPoint.gimbalPitch = -90f
                                wayPoint.speed = 5f
                                wayPoint.shootPhotoTimeInterval = 2f
                                wayPoint.shootPhotoDistanceInterval = 0f
                                */
                                val wayPoint = onNewWaypoint?.invoke(position)
                                wayPoint?.let {
                                    addWayMarker(it)
                                }
                                //val holder = addWayMarker(wayPoint)
                                //onMarkerSelect?.invoke(holder)
                                //setSelectMarker(ll)
                                onMarkerSelect?.invoke(null)
                                removeSelectMarker()
                            }
                        }
                        else -> {
                            movingMarker?.let {
                                onMarkerSelect?.invoke(it)
                                setSelectMarker(it.marker.position)
                            }
                        }
                    }
                }
                state = State.None
                movingMarker = null
                map.uiSettings.isScrollGesturesEnabled = true
            }
        }
    }

    fun setSelectMarker(location: LatLng){
        //logMethod(this)

        if(selectMarker == null) {
            log(this , "selectMarker is null")
            val options = MarkerOptions()
                    .anchor(0.5f, 0.5f)
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.item_select))
                    .zIndex(5f)
            selectMarker = map.addMarker(options)
            selectMarker?.isClickable = false
        }
        selectMarker?.position = location
        selectMarker?.isVisible=true
    }

    fun removeSelectMarker(){
        //logMethod(this)
        selectMarker?.isVisible= false
    }

    private fun addWayMarker(wayPoint:Waypoint):WayHolder {
        //val icon = drawTextToBitmap(context, R.drawable.item_air_data_point_red, "S", 20)
        val firstOption = MarkerOptions()
                .anchor(0.5f,0.5f)
                .position(LatLng(wayPoint.coordinate.latitude , wayPoint.coordinate.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.item_group))
                .zIndex(3f)
        val options = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .position(LatLng(wayPoint.coordinate.latitude , wayPoint.coordinate.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.item_air_data_point_red))
                .zIndex(3f)


        var marker:Marker? = null
        if(wayMarkers.size == 0){
            marker = map.addMarker(firstOption)
        }else{
            marker = map.addMarker(options)
        }


        marker.isClickable = false

        val holder = WayHolder(marker, wayPoint)

        wayMarkers += holder

        updatePath()

        return holder
    }
    
    private fun addAreaMarker(point:LatLng){
        val options = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .position(point)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.item_air_data_point_red))
                .zIndex(3F)
        val marker = map.addMarker(options)
        marker.isClickable = false
        
        areaMarkers += WayHolder(marker , Waypoint(point.latitude , point.longitude , 0f))

    }

    private fun isConvex(points: List<Point>): Boolean {
        for (point in points) {
            val otherPoints = points.filter { it != point }
            if (pointInTriangle(point, otherPoints[0], otherPoints[1], otherPoints[2])) {
                return false
            }
        }
        return true
    }

    private fun getWayMarker(x: Float, y: Float): WayHolder? {
        return wayMarkers.find {
            val point = map.projection.toScreenLocation(it.marker.position)
            Math.abs(x - point.x) < 50 && Math.abs(y - point.y) < 50
        }
    }

    private fun getAreaMarker(x:Float,y:Float):WayHolder?{
        return areaMarkers.find {
            val point = map.projection.toScreenLocation(it.marker.position)
            Math.abs(x - point.x) < 50 && Math.abs(y - point.y) < 50
        }
    }

    fun clear() {
        wayMarkers.forEach { it.marker.remove() }
        areaMarkers.forEach { it.marker.remove() }
        wayMarkers = listOf()
        areaMarkers = listOf()
        route?.remove()
        route = null
        polygon?.remove()
        polygon = null
    }

    fun setControlVisible(visible: Boolean) {
        wayMarkers.forEach { it.marker.isVisible = visible }
        if(wayMarkers.size>0){
            wayMarkers[0].marker.isVisible = true
        }
        areaMarkers.forEach { it.marker.isVisible = visible }
        polygon?.isVisible = visible
    }

    private fun sign(p1: Point, p2: Point, p3: Point): Int =
            (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)

    private fun pointInTriangle(pt: Point, v1: Point, v2: Point, v3: Point): Boolean {
        val b1 = sign(pt, v1, v2) < 0.0f
        val b2 = sign(pt, v2, v3) < 0.0f
        val b3 = sign(pt, v3, v1) < 0.0f

        return ((b1 == b2) && (b2 == b3))
    }

    private fun intersection(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val x3 = p3.x
        val y3 = p3.y
        val x4 = p4.x
        val y4 = p4.y

        val d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (d == 0) return null

        val xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d
        val yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d

        return Point(xi, yi)
    }

    private fun getIntervalInScreen(): Int {
        val points = areaMarkers.map { map.projection.toScreenLocation(it.marker.position) }.sortedBy { it.y }
        val topPoint = points.first()
        val bottomPoint = Point(topPoint.x, points.last().y)

        val distance = AMapUtils.calculateLineDistance(
                map.projection.fromScreenLocation(topPoint),
                map.projection.fromScreenLocation(bottomPoint)
        )
        val distanceInScreen = bottomPoint.y - topPoint.y
        val rate = distanceInScreen.toFloat() / distance
        return (interval * rate).toInt()
    }

    private fun createRoutePoints(markerPoints: List<LatLng>): List<LatLng> {
        val points = markerPoints.map { map.projection.toScreenLocation(it) }
        if (points.isEmpty()) {
            return listOf()
        }
        val pointsSortByX = points.sortedBy { it.x }
        val pointsSortByY = points.sortedBy { it.y }

        val minX = pointsSortByX.first().x
        val maxX = pointsSortByX.last().x
        val minY = pointsSortByY.first().y
        val maxY = pointsSortByY.last().y

        val topPoint = pointsSortByY.first()

        for(i in points.indices){
            if(points[i] == topPoint){
                areaMarkers[i].marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.item_group))
            }else{
                areaMarkers[i].marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.item_air_data_point_red))
            }
        }

        val bottomPoint = pointsSortByY.last()

        val topPointIndex = points.indexOf(topPoint)
        val nextPoints = listOf(points[(topPointIndex - 1 + 4) % 4], points[(topPointIndex + 1) % 4]).sortedBy { it.x }
        val leftPoint = nextPoints[0]
        val rightPoint = nextPoints[1]

        var leftLine = Pair(topPoint, leftPoint)
        var rightLine = Pair(topPoint, rightPoint)

        val intervalInScreen = getIntervalInScreen()

        val routePoints = arrayListOf<Point>()
        var routeY = minY
        var i = 0
        //routePoints.add(topPoint)
        while (routeY <= maxY) {
            val verticalLine = Pair(Point(minX, routeY), Point(maxX, routeY))
            if (routeY >= leftLine.second.y) {
                leftLine = Pair(leftLine.second, getNextPoint(points, leftLine))
            }
            if (routeY >= rightLine.second.y) {
                rightLine = Pair(rightLine.second, getNextPoint(points, rightLine))
            }

            val leftIntersection = intersection(verticalLine.first, verticalLine.second, leftLine.first, leftLine.second)
            val rightIntersection = intersection(verticalLine.first, verticalLine.second, rightLine.first, rightLine.second)

            if (leftIntersection != null && rightIntersection != null) {
                if (i % 2 == 0) {
                    routePoints.add(leftIntersection)
                    if(leftIntersection.x != rightIntersection.x || leftIntersection.y != rightIntersection.y) {
                        routePoints.add(rightIntersection)
                    }
                } else {
                    routePoints.add(rightIntersection)
                    if(leftIntersection.x != rightIntersection.x || leftIntersection.y != rightIntersection.y) {
                        routePoints.add(leftIntersection)
                    }
                }
            }

            routeY += intervalInScreen
            i++
        }
        routePoints.add(bottomPoint)

        return routePoints.map { map.projection.fromScreenLocation(it) }
    }

    private fun getNextPoint(points: List<Point>, line: Pair<Point, Point>): Point {
        val index = points.indexOf(line.second)
        val nextPoint = points[(index - 1 + 4) % 4]
        if (nextPoint != line.first) {
            return nextPoint
        }
        return points[(index + 1) % 4]
    }
/*
    fun setMarkerPoints(points: List<LatLng>) {
        drawMarkers(points)
        when (type) {
            ControlType.Area -> {
                drawArea(points)
            }
            ControlType.Path -> {
                drawPath(points)
            }
        }
    }
*/
    private fun updatePolygon() {
        if (type != ControlType.Area) {
            return
        }
    
        val markerPoints = areaMarkers.map { it.marker.position }
        polygon?.points = markerPoints

        getRoute().points = createRoutePoints(markerPoints)
        onAreaPointUpdated?.invoke(getRoute().points)
    
    }

    fun updatePath() {
        if (type != ControlType.Path) {
            return
        }

        getRoute().points = wayMarkers.map { it.marker.position }
        onWayPointUpdated?.invoke(wayMarkers.map { it.wayPoint })
    }
    

    fun setWayMakerPoints(wayPoints: List<Waypoint>){
        setSelectMarker(LatLng(0.0,0.0))
        selectMarker?.isVisible=false
        wayMarkers.forEach{it.marker.remove()}
        
        wayPoints.forEach { addWayMarker(it) }
    }
    
    fun setAreaMakerPoints(points:List<LatLng>){
        areaMarkers.forEach { it.marker.remove() }
        points.forEach {addAreaMarker(it) }

        drawArea(points)

        onMarkerUpdated?.invoke(areaMarkers.map { it.marker.position })
        onAreaPointUpdated?.invoke(getRoute().points)
        
    }
    
    fun setInterval(interval: Int, isDrawRoute: Boolean = false) {
        this.interval = interval
        if (type == ControlType.Area && isDrawRoute) {
            drawRoute(createRoutePoints(areaMarkers.map { it.marker.position }))
            onAreaPointUpdated?.invoke(getRoute().points)
        }
    }

    private fun drawArea(markerPoints: List<LatLng>) {
        if (polygon == null) {
            val options = PolygonOptions()
            options.fillColor(ContextCompat.getColor(context, R.color.map_area))
            options.strokeWidth(0F)
            options.zIndex(1F)
            polygon = map.addPolygon(options)
        }

        polygon?.points = markerPoints

        drawRoute(createRoutePoints(markerPoints))
    }

    private fun getRoute(): Polyline {
        if (route == null) {
            val options = PolylineOptions()
            options.color(ContextCompat.getColor(context, R.color.red))
            options.width(5F)
            options.zIndex(2F)
            route = map.addPolyline(options)
        }
        return route!!
    }

    private fun drawRoute(points: List<LatLng>) {
        getRoute().points = points
        //onWayPointUpdated?.invoke(points)
    }
}
