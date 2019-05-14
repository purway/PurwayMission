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
import android.widget.TextView
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.model.User

class UserDetailDialog(context:Context):Dialog(context){
    private var titleId:Int?=null

    private var user:User?=null
    private var txtTitle:TextView?=null
    private var btnClose:Button?=null
    private var txtName: TextView?=null
    //private var txtPhone:TextView?=null
    private var txtAccount:TextView?=null
    //private var btnAlterPswd: Button?=null
    private var txtCompany:TextView?=null
    private var btnActivation:Button?=null
    private var layoutName: ViewGroup?=null
    private var layoutCompany:ViewGroup?=null

    private var closeClick:((dialog:Dialog)->Unit)?=null
    //private var alterPswdClick:((dialog:Dialog)->Unit)?=null
    private var alterDetailClick:((dialog:Dialog)->Unit)?=null
    private var activationClick:((dialog:Dialog)->Unit)?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun show() {
        super.show()
        user?.let {
            setUser(it)
        }
    }

    fun setUser(user:User){
        this.user = user

        user.name?.let {
            txtName?.setText(it)
        }
        /*
        if(user.phone!=null && user.prefixer!=null){
            txtPhone?.setText("${user.prefixer}-${user.phone}")
        }
        */
        user.djiAccount?.let {
            txtAccount?.setText(it)
        }
        user.company?.let{
            txtCompany?.setText(it)
        }
        if(user.productId!=null){
            btnActivation?.setTextColor(Color.GREEN)
            btnActivation?.setText(R.string.user_option_is_activation)
        }else{
            btnActivation?.setTextColor(Color.RED)
            btnActivation?.setText(R.string.user_option_no_activation)
        }
    }

    private fun initView(){
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_user_detail,null)
        setContentView(view)

        txtTitle = view.findViewById(R.id.txtTitle)
        btnClose = view.findViewById(R.id.btnClose)
        txtName = view.findViewById(R.id.txtName)
        txtAccount = view.findViewById(R.id.txtAccount)
        txtCompany = view.findViewById(R.id.txtCompany)
        btnActivation = view.findViewById(R.id.btnActivation)
        layoutName = view.findViewById(R.id.layoutName)
        layoutCompany = view.findViewById(R.id.layoutCompany)

        titleId?.let {
            txtTitle?.setText(it)
        }

        btnClose?.setOnClickListener {
            if (closeClick != null) {
                closeClick?.invoke(this)
            } else {
                dismiss()
            }
        }

        layoutName?.setOnClickListener {
            alterDetailClick?.invoke(this)
        }

        layoutCompany?.setOnClickListener {
            alterDetailClick?.invoke(this)
        }
/*
        btnAlterPswd?.setOnClickListener {
            alterPswdClick?.invoke(this)
        }
        */
        btnActivation?.setOnClickListener {
            activationClick?.invoke(this)
        }

        val latMain = view.findViewById<ViewGroup>(R.id.latMain)
        val layoutParams = window.attributes //获取当前对话框的参数值
        layoutParams.width = latMain.layoutParams.width
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window.setAttributes(layoutParams)

    }

    class Builder(context: Context){
        private val dialog = UserDetailDialog(context)

        fun setTitle(titleId:Int):Builder{
            dialog.titleId = titleId
            return this
        }

        fun setOnCloseClickListener(l: ((dialog: Dialog) -> Unit)): Builder {
            dialog.closeClick = l
            return this
        }

        fun setOnAlterDetailClickListener(l:((dialog:Dialog)->Unit)):Builder{
            dialog.alterDetailClick = l
            return this
        }
/*
        fun setOnAlterPswdClickListener(l:((dialog:Dialog)->Unit)):Builder{
            dialog.alterPswdClick = l
            return this
        }
*/
        fun setOnActivationClickListener(l:((dialog:Dialog)->Unit)):Builder{
            dialog.activationClick = l
            return this
        }

        fun create():UserDetailDialog{
            return dialog
        }

    }
}