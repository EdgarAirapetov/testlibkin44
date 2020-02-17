package com.adaptytest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.adapty.Adapty

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Adapty.init(applicationContext)


    }
}
