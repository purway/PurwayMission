package com.kaisavx.AircraftController.util

import android.content.Context
import io.realm.Realm
import java.util.concurrent.Executors

/**
 * Created by windless on 2017/11/9.
 */
object RealmKit {
    private val executor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        executor.submit {
            Realm.init(context)
        }
    }

    fun deinit() {
        executor.shutdown()
    }

    fun executeTransaction(transaction: (Realm) -> Unit) {
        executor.submit {
            Realm.getDefaultInstance().executeTransaction(transaction)
        }
    }
}