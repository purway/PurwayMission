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
import android.widget.EditText
import android.widget.TextView
import com.kaisavx.AircraftController.R

class UserActivationDialog(context: Context): Dialog(context, R.style.Dialog){
    private var titleId:Int?= null

    private var closeClick:((dialog:Dialog)->Unit)?=null
    private var sureClick:((dialog:Dialog , code:String)->Unit)?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView(){
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_user_activation ,null)
        setContentView(view)

        val btnClose = view.findViewById<Button>(R.id.btnClose)
        val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
        val edtActivation = view.findViewById<EditText>(R.id.edtActivation)
        val btnSure = view.findViewById<Button>(R.id.btnSure)

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

        btnSure.setOnClickListener {
            val code= edtActivation.text.toString()
            sureClick?.invoke(this ,code)
        }

        val latMain = view.findViewById<ViewGroup>(R.id.latMain)
        val layoutParams = window.attributes //获取当前对话框的参数值
        layoutParams.width = latMain.layoutParams.width
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window.setAttributes(layoutParams)
    }

    class Builder(context: Context){
        val dialog = UserActivationDialog(context)

        fun setTitle(titleId:Int):Builder{
            dialog.titleId = titleId
            return this
        }

        fun setOnCloseClickListener(l: ((dialog:Dialog) -> Unit)): Builder {
            dialog.closeClick = l
            return this
        }

        fun setOnSureClickListener(l:((dialog:Dialog , code:String)->Unit)):Builder{
            dialog.sureClick = l
            return this
        }

        fun create():UserActivationDialog{
            return dialog
        }
    }
}