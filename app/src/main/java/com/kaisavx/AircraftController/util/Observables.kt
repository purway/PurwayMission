package com.kaisavx.AircraftController.util

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3

/**
 * Created by windless on 2017/12/27.
 */
class ObservableUtils {
    companion object {
        fun <T1, T2> pair(ob1: Observable<T1>, ob2: Observable<T2>): Observable<Pair<T1, T2>> =
                Observable.combineLatest(ob1, ob2, BiFunction { t1, t2 -> Pair(t1, t2) })

        fun <T1, T2, T3> triple(ob1: Observable<T1>, ob2: Observable<T2>, ob3: Observable<T3>): Observable<Triple<T1, T2, T3>> =
                Observable.combineLatest(ob1, ob2, ob3, Function3 { t1, t2, t3 -> Triple(t1, t2, t3) })
    }
}