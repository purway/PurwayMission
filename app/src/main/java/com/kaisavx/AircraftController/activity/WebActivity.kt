package com.kaisavx.AircraftController.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kaisavx.AircraftController.R
import kotlinx.android.synthetic.main.activity_web.*

/**
 * Created by Abner on 2017/6/12.
 */

class WebActivity: BaseActivity() {
    companion object {
        fun intent(context: Context, title: String, link: String): Intent {
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtra("title", title)
            intent.putExtra("link", link)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        title = intent.getStringExtra("title")
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(intent.getStringExtra("link"))
    }
}
