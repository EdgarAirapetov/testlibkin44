package com.adapty

import android.content.Context
import com.adapty.api.ApiClientRepository
import com.adapty.api.ICallback
import com.adapty.utils.PreferenceManager
import java.net.UnknownHostException
import android.hardware.usb.UsbDevice.getDeviceId
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.adapty.api.responses.CreateProfileResponse
import com.adapty.api.responses.SyncMetaInstallResponse
import java.util.*


class Adapty {

    companion object {
        lateinit var applicationContext: Context
        lateinit var preferenceManager: PreferenceManager

        fun init(applicationContext: Context) {
            this.applicationContext = applicationContext
            this.preferenceManager = PreferenceManager(applicationContext)

            ApiClientRepository.getInstance(preferenceManager).createProfile(object : ICallback {
                override fun success(response: Any?, reqID: Int) {
                    if (response is CreateProfileResponse) {
                        response.data?.attributes?.profile_id?.let {
                            preferenceManager.profileID = it
                        }
                        response.data?.attributes?.customer_user_id?.let {
                            preferenceManager.customerUserID = it
                        }
                    }

                    sendSyncMetaInstallRequest()
                }

                override fun fail(msg: String, reqID: Int) {

                }

            })
        }


        fun sendSyncMetaInstallRequest() {
            ApiClientRepository.getInstance(preferenceManager).syncMetaInstall(object : ICallback {
                override fun success(response: Any?, reqID: Int) {

                }

                override fun fail(msg: String, reqID: Int) {

                }

            })
        }
    }
}