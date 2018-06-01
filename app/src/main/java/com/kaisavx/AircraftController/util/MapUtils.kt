package com.kaisavx.AircraftController.util

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.kaisavx.AircraftController.App
import io.reactivex.Observable
import java.io.File
import java.io.FileOutputStream

/**
 * Created by windless on 2017/10/12.
 */
fun getLocationsCenter(locations: List<LatLng>): LatLng {
    if (locations.size == 1) {
        return locations.first()
    }

    var x = 0.0
    var y = 0.0
    var z = 0.0

    locations.forEach {
        val latitude = it.latitude * Math.PI / 180
        val longitude = it.longitude * Math.PI / 180

        x += Math.cos(latitude) * Math.cos(longitude)
        y += Math.cos(latitude) * Math.sin(longitude)
        z += Math.sin(latitude)
    }

    x /= locations.size
    y /= locations.size
    z /= locations.size

    val centerLongitude = Math.atan2(y, x)
    val centerSquareRoot = Math.sqrt(x * x + y * y)
    val centerLatitude = Math.atan2(z, centerSquareRoot)
    return LatLng(centerLatitude * 180 / Math.PI, centerLongitude * 180 / Math.PI)
}

fun getMapScreenShootFile(fileName: String): File {
    val dir = Environment.getExternalStorageDirectory()
    val mapScreenShootDir = File(File(dir, "jellyfish"), "maps")
    return File(mapScreenShootDir, fileName)
}

fun saveMapScreenShoot(map: AMap, fileName: String) {
    val dir = Environment.getExternalStorageDirectory()
    val mapScreenShootDir = File(File(dir, "jellyfish"), "maps")
    if (!mapScreenShootDir.exists()) {
        mapScreenShootDir.mkdirs()
    }

    val filePath = File(mapScreenShootDir, fileName)

    map.getMapScreenShot(object : AMap.OnMapScreenShotListener {
        override fun onMapScreenShot(bitmap: Bitmap?) {
            if (bitmap == null) {
                return
            }
            if (filePath.exists()) {
                filePath.delete()
            }
            val fos = FileOutputStream(filePath)
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 50, fos)
            if (ok) {
                try {
                    fos.flush()
                } catch (e: Exception) {
                    Log.d("MapUtils", "save map screen shoot failed", e)
                }
                try {
                    fos.close()
                } catch (ignored: Exception) {
                }
            }
        }

        override fun onMapScreenShot(bitmap: Bitmap?, status: Int) {
            Log.d("MapUtils", "$status")
        }
    })
}

fun getAddress(location: LatLng): Observable<String> {
    return Observable.create {
        val search = GeocodeSearch(App.context)
        search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onRegeocodeSearched(result: RegeocodeResult?, p1: Int) {
                if (result != null) {
                    it.onNext(result.regeocodeAddress.formatAddress)
                }
                it.onComplete()
            }

            override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) {
            }
        })
        search.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(location.latitude, location.longitude), 100f, GeocodeSearch.AMAP))
    }
}

val x_PI = 3.14159265358979324 * 3000.0 / 180.0
val PI = 3.1415926535897932384626
val a = 6378245.0
val ee = 0.00669342162296594323

fun wgs84togcj02(latlng: LatLng): LatLng {
    val lat = latlng.latitude
    val lng = latlng.longitude
    if (outOfChina(lng, lat)) {
        return latlng
    } else {
        var dlat = transformlat(lng - 105.0, lat - 35.0)
        var dlng = transformlng(lng - 105.0, lat - 35.0)
        val radlat = lat / 180.0 * PI
        var magic = Math.sin(radlat)
        magic = 1 - ee * magic * magic
        val sqrtmagic = Math.sqrt(magic)
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI)
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI)
        val mglat = lat + dlat
        val mglng = lng + dlng
        return LatLng(mglat, mglng)
    }
}

fun gcj02towgs84(latlng: LatLng): LatLng {
    val lat = latlng.latitude
    val lng = latlng.longitude
    if (outOfChina(lng, lat)) {
        return latlng
    } else {
        var dlat = transformlat(lng - 105.0, lat - 35.0)
        var dlng = transformlng(lng - 105.0, lat - 35.0)
        val radlat = lat / 180.0 * PI
        var magic = Math.sin(radlat)
        magic = 1 - ee * magic * magic
        val sqrtmagic = Math.sqrt(magic)
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI)
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI)
        val mglat = lat + dlat
        val mglng = lng + dlng
        return LatLng(
                lat * 2 - mglat,
                lng * 2 - mglng
        )
    }
}


private fun outOfChina(lng: Double, lat: Double): Boolean {
    return (lng < 72.004 || lng > 137.8347) && (lat < 0.8293 || lat > 55.8271)
}

private fun transformlat(lng: Double, lat: Double): Double {
    var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat
    + 0.2 * Math.sqrt(Math.abs(lng))
    ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
    ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0
    ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0
    return ret
}

private fun transformlng(lng: Double, lat: Double): Double {
    var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat
    + 0.1 * Math.sqrt(Math.abs(lng))
    ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
    ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0
    ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0
    return ret
}
