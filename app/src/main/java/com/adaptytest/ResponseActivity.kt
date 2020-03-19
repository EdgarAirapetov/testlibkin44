package com.adaptytest

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_response.*

class ResponseActivity : AppCompatActivity() {

    companion object {
        const val RESPONSE = "RESPONSE"

        fun openResponseActivity(context: Context, response: String) {
            val intent = Intent(context, ResponseActivity::class.java)
            intent.putExtra(RESPONSE, response)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response)

        val extras = intent.extras
        extras?.let {
            response.text = it.getString(RESPONSE)
        }
    }
}
