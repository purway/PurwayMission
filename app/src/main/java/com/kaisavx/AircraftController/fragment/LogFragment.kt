package com.kaisavx.AircraftController.fragment

import android.os.Bundle
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.util.KLog
import com.kaisavx.AircraftController.util.KLog.logSubject
import com.kaisavx.AircraftController.util.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_log.*
import java.util.concurrent.LinkedBlockingDeque

class LogFragment : Fragment() {
    companion object {
        private val TAG = "LogFragment"
        private val MAX = 200
    }

    private val disposable = CompositeDisposable()

    private val logList = LinkedBlockingDeque<KLog.LogMessage>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupView()
        setupDisposable()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    private fun setupView() {
        btnShow.setOnClickListener {
            latLog.visibility = View.VISIBLE
            btnShow.visibility = View.GONE
        }
        btnHide.setOnClickListener {
            latLog.visibility = View.GONE
            btnShow.visibility = View.VISIBLE
        }
        btnClear.setOnClickListener {
            logList.clear()
            txtLog.scrollTo(0,0)
            setLogList(logList.toList())
        }

        txtLog.movementMethod = ScrollingMovementMethod.getInstance()
    }

    private fun setupDisposable() {
        disposable += logSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {msg->
                if(logList.size>=MAX){
                    logList.take()
                }
                logList.put(msg)
                //setLogList(logList.toList())

                txtLog.append(Html.fromHtml(msg.toString()))
                val offset = txtLog.lineCount * txtLog.lineHeight
                if (offset > txtLog.height) {
                    txtLog.scrollTo(0, offset - txtLog.height)
                }
            }
    }

    private fun setLogList(list: List<KLog.LogMessage>) {
        txtLog.text = ""
        list.forEach { msg ->
            txtLog.append(Html.fromHtml(msg.toString()))
        }

        val offset = txtLog.lineCount * txtLog.lineHeight
        if (offset > txtLog.height) {
            txtLog.scrollTo(0, offset - txtLog.height)
        }
    }

}