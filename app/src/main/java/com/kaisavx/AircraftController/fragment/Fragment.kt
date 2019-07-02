package com.kaisavx.AircraftController.fragment

import android.content.Intent
import android.support.v4.app.Fragment
import com.kaisavx.AircraftController.activity.BaseActivity
import io.reactivex.subjects.BehaviorSubject

/**
 * Created by Abner on 2017/5/17.
 */
open class Fragment : Fragment() {
    val isVisibleSubject:BehaviorSubject<Boolean> = BehaviorSubject.create()
    fun startActivityForOnce(intent: Intent) {
        val activity = activity as BaseActivity
        activity.startActivityOnce(intent)
    }

    fun hide(){
        isVisibleSubject.onNext(false)
        fragmentManager?.beginTransaction()?.hide(this)?.commit()
    }

    fun show(){
        isVisibleSubject.onNext(true)
        fragmentManager?.beginTransaction()?.show(this)?.commit()
    }
}