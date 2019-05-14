package com.kaisavx.AircraftController.activity

import android.os.Bundle
import com.kaisavx.AircraftController.R

class FlyBookActivity:BaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fly_book)
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}