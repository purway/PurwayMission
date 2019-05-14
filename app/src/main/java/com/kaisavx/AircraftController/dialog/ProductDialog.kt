package com.kaisavx.AircraftController.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.model.Product

class ProductDialog(context: Context): Dialog(context, R.style.Dialog){
    private var titleId:Int?=null

    private var product: Product?=null

    private var txtName:TextView?=null
    private var txtCompany:TextView?=null

    private var imageLive: ImageView?=null
    private var imageVideoData:ImageView?=null
    private var imageMediaUpload:ImageView?=null
    private var imageWaypointSync:ImageView?=null
    private var imageFlydataSync:ImageView?=null
    private var imageRealtimeControl:ImageView?=null
    private var imageGasEnable:ImageView?=null
    private var imageWaterEnable:ImageView?=null


    private var closeClick:((dialog:Dialog)->Unit)?= null
    private var reactivationClick:((dialog:Dialog)->Unit)?=null

    private val drawableSupport = context.resources.getDrawable(R.drawable.icon_support)
    private val drawableUnsupport = context.resources.getDrawable(R.drawable.icon_unsupport)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    fun initView(){
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_product_detail ,null)
        setContentView(view)
        val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        txtName = view.findViewById(R.id.txtName)
        txtCompany = view.findViewById(R.id.txtCompany)

        imageLive = view.findViewById(R.id.imageLive)
        imageVideoData = view.findViewById(R.id.imageVideoData)
        imageMediaUpload = view.findViewById(R.id.imageMediaUpload)
        imageWaypointSync = view.findViewById(R.id.imageWaypointSync)
        imageFlydataSync = view.findViewById(R.id.imageFlydataSync)
        imageRealtimeControl = view.findViewById(R.id.imageRealtimeControl)
        imageGasEnable = view.findViewById(R.id.imageGasEnable)
        imageWaterEnable = view.findViewById(R.id.imageWaterEnable)

        val btnReactivation = view.findViewById<Button>(R.id.btnReactivation)

        titleId?.let {
            txtTitle.setText(it)
        }

        btnClose.setOnClickListener {
            if (closeClick != null) {
                closeClick?.invoke(this)
            } else {
                dismiss()
            }
        }

        btnReactivation.setOnClickListener {
            reactivationClick?.invoke(this)
        }

        val latMain = view.findViewById<ViewGroup>(R.id.latMain)
        val layoutParams = window.attributes //获取当前对话框的参数值
        layoutParams.width = latMain.layoutParams.width
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window.setAttributes(layoutParams)
    }

    fun setProduct(product: Product){
        txtName?.setText(product.name)
        txtCompany?.setText(product.company)
        for(i in 0 .. 8){
            val v = product.permission.shr(i).and(0x1)
            val imageView =
            when(i){
                0->imageLive
                1->imageVideoData
                2->imageMediaUpload
                3->imageWaypointSync
                4->imageFlydataSync
                5->imageRealtimeControl
                6->imageGasEnable
                7->imageWaterEnable
                else->null
            }
            if(v>0){
                imageView?.setImageDrawable(drawableSupport)
            }else{
                imageView?.setImageDrawable(drawableUnsupport)
            }
        }
    }

    class Builder(context: Context){
        private val dialog = ProductDialog(context)

        fun setTitle(titleId: Int): Builder {
            dialog.titleId = titleId
            return this
        }

        fun setOnCloseClickListener(l: ((dialog: Dialog) -> Unit)): Builder {
            dialog.closeClick = l
            return this
        }

        fun setOnReactivationClickListener(l:((dialog:Dialog)->Unit)):Builder{
            dialog.reactivationClick = l
            return this
        }

        fun create():ProductDialog{
            return dialog
        }
    }

}