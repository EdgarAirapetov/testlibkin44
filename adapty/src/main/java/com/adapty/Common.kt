package com.adapty

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

@SuppressLint("HardwareIds")
fun getUUID(context: Context) : String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}