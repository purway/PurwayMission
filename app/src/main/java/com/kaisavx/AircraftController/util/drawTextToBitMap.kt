package com.kaisavx.AircraftController.util

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG

/**
 * Created by Abner on 2017/5/26.
 */
fun drawTextToBitmap(context: Context, resId: Int, text: String, textSize: Int): Bitmap {
    val resources = context.resources
    val scale = resources.displayMetrics.density
    val originBitmap = BitmapFactory.decodeResource(resources, resId)

    log(context,"${originBitmap.width} , ${originBitmap.height} , $scale")
    val bitmapConfig = originBitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888
    val bitmap = originBitmap.copy(bitmapConfig, true)

    val canvas = Canvas(bitmap)
    val paint = Paint(ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(255, 0, 0)
    paint.textSize = textSize * scale
    paint.textAlign = Paint.Align.LEFT

    // draw text to the Canvas center
    val bounds = Rect()
    canvas.getClipBounds(bounds)
    val cHeight = bounds.height()
    val cWidth = bounds.width()

    paint.getTextBounds(text, 0, text.length, bounds)
    val x = cWidth / 2f - bounds.width() / 2f - bounds.left
    val y = cHeight / 2f + bounds.height() / 2f - bounds.bottom

    canvas.drawText(text, x, y, paint)
    log(context,"${bitmap.width} , ${bitmap.height}")

    val matrix = Matrix()
    matrix.postScale(1/scale,1/scale)

    val newBitmap = Bitmap.createBitmap(bitmap , 0 , 0 , bitmap.width,bitmap.height,matrix,false)

    if(!originBitmap.isRecycled){
        originBitmap.recycle()
    }
    if(!bitmap.isRecycled){
        bitmap.recycle()
    }

    return newBitmap
}
