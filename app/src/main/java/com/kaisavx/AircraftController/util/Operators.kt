package com.kaisavx.AircraftController.util

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Created by Abner on 2017/6/9.
 */

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    this.add(disposable)
}

fun ByteArray.toHex() =
        this.joinToString(separator = "") { it.toInt().and(0xff).toString(16).padStart(2, '0') }
