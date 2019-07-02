package com.kaisavx.AircraftController.module.dji

import android.content.Context
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.NetUtil
import com.kaisavx.AircraftController.util.plusAssign
import dji.common.error.DJIError
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks
import dji.sdk.useraccount.UserAccountManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class DJIAccountManager {
    companion object {
        private val TAG = "DJIAccountManager"
    }

    private val disposable = CompositeDisposable()

    private val accountSubject = PublishSubject.create<String>()
    private val accountStateSubject = BehaviorSubject.create<UserAccountState>()
    private val loginErrorSubject = PublishSubject.create<DJIError>()
    private val showLoginSubject = PublishSubject.create<Boolean>()

    val accountOb: Observable<String> = accountSubject
    val accountStateOb: Observable<UserAccountState> = accountStateSubject
    val loginErrorOb: Observable<DJIError> = loginErrorSubject
    val showLoginOb: Observable<Boolean> = showLoginSubject

    init {

    }

    fun checkLogin() {
        if (!DJIManager.hasRegistered()) return

        disposable += Observable.interval(500, TimeUnit.MILLISECONDS)
                .takeUntil(accountStateSubject)
                .observeOn(Schedulers.computation())
                .subscribe {
                    UserAccountManager.getInstance().let { manager ->
                        val state = manager.userAccountState
                        KLog.i(TAG, "check user state:$state")
                        if (state != UserAccountState.UNKNOWN) {
                            accountStateSubject.onNext(state)
                        }
                    }
                }
    }

    fun getLoginState() {
        KLog.i(TAG, "getLoginState")
        if (!DJIManager.hasRegistered()) return
        UserAccountManager.getInstance().let { manager ->
            val state = manager.userAccountState
            accountStateSubject.onNext(state)
        }
    }

    fun login() {
        KLog.i(TAG, "login")
        if (!DJIManager.hasRegistered()) return
        UserAccountManager.getInstance().let { manager ->
            val state = manager.userAccountState
            KLog.i(TAG, "user account state:$state")
            if (state != UserAccountState.NOT_AUTHORIZED &&
                    state != UserAccountState.AUTHORIZED) {
                Thread {
                    if (NetUtil.ping("www.baidu.com")) {
                        showLoginSubject.onNext(true)
                    } else {
                        loginErrorSubject.onNext(DJIError.NO_NETWORK)
                    }
                }.start()
            } else {
                manager.getLoggedInDJIUserAccountName(object : CommonCallbacks.CompletionCallbackWith<String> {
                    override fun onSuccess(account: String) {
                        KLog.i(TAG, "getLoggedInDJIUserAccountName:$account")
                        accountSubject.onNext(account)
                    }

                    override fun onFailure(error: DJIError?) {
                        error?.let {
                            KLog.e(TAG, "getLoggedInDJIUserAccountName failure:${it.description}")
                        }
                    }
                })
            }
        }
    }

    fun showLoginDialog(context: Context) {
        KLog.i(TAG, "showLoginDialog")
        if (!DJIManager.hasRegistered()) return
        UserAccountManager.getInstance().let { manager ->
            manager.logIntoDJIUserAccount(context, object : CommonCallbacks.CompletionCallbackWith<UserAccountState> {
                override fun onSuccess(state: UserAccountState?) {
                    manager.getLoggedInDJIUserAccountName(object : CommonCallbacks.CompletionCallbackWith<String> {
                        override fun onSuccess(account: String) {
                            KLog.i(TAG, "getLoggedInDJIUserAccountName:$account")
                            accountSubject.onNext(account)
                        }

                        override fun onFailure(error: DJIError?) {
                            error?.let {
                                KLog.e(TAG, "getLoggedInDJIUserAccountName failure:${it.description}")
                            }
                        }
                    })
                }

                override fun onFailure(error: DJIError?) {
                    error?.let {
                        loginErrorSubject.onNext(it)
                        KLog.e(TAG, "logIntoDJIUserAccount failure:${it.description}")
                    }
                }
            })
        }
    }

    fun logout() {
        KLog.i(TAG, "logout")
        if (!DJIManager.hasRegistered()) return
        UserAccountManager.getInstance().loginOut { error ->
            error?.let {
                KLog.e(TAG, "loginOut failure:${it.description}")
            }

        }
    }

    fun destory() {
        disposable.clear()
    }
}