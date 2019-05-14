package com.kaisavx.AircraftController.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kaisavx.AircraftController.R
import com.kaisavx.AircraftController.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_text.*

/**
 * Created by Abner on 2017/6/12.
 */
class TextActivity : BaseActivity() {
    companion object {
        fun intent(context: Context, title: String, textRes: Int): Intent {
            val intent = Intent(context, TextActivity::class.java)
            intent.putExtra("title", title)
            intent.putExtra("text", textRes)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)

        txt.setText(intent.getIntExtra("text", 0))
        title = intent.getStringExtra("title")
    }
}
