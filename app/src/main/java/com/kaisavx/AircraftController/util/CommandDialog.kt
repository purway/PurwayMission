package com.kaisavx.AircraftController.util

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import com.kaisavx.AircraftController.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CommandDialog(context: Context) {

    private val errorSubject: BehaviorSubject<String> = BehaviorSubject.create()
    private val isWaitingSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    private val disposable = CompositeDisposable()

    private val errorDialog  by lazy {
        log(this , "errorDialog create:$context")
        AlertDialog.Builder(context)
                .setNegativeButton(R.string.btn_sure, { dialog, which ->
                    dialog.dismiss()
                })
                .create()
    }

    private val waitDialog by lazy {
        log(this , "waitDialog create:$context")
        val dialog = ProgressDialog(context)
        dialog.setMessage(context.resources.getString(R.string.msg_waitting))
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog
    }

    init {
        disposable.add(errorSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    errorDialog.setTitle(it)
                    errorDialog.show()
                })
        disposable.add(isWaitingSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it) {
                        waitDialog.show()
                    } else {
                        waitDialog.dismiss()
                    }
                })
    }

    fun destory(){
        disposable.clear()
    }

    fun setWaitShow(isShow:Boolean){
        isWaitingSubject.onNext(isShow)
    }

    fun showError(error:String){
        errorSubject.onNext(error)
    }

}