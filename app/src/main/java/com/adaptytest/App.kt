package com.adaptytest

import android.app.Application
import android.util.Log
import com.adapty.Adapty
import com.adapty.api.AttributionType
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import io.branch.referral.Branch


class App : Application() {

    private var API_KEY_APPSFLYER = "zhjg4espSrBC75mig2ZtzK"

    override fun onCreate() {
        super.onCreate()

//        Adapty.activate(
//            this,
//            "public_live_7Ei6YwqY.8fRoPRhM2lngcCVXEPFU",
//            null
//        )

        val conversionListener: AppsFlyerConversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {
//                Adapty.updateAttribution(conversionData, AttributionType.APPSFLYER)
            }

            override fun onConversionDataFail(errorMessage: String) {

            }

            override fun onAppOpenAttribution(conversionData: Map<String, String>) {
                conversionData.let {
//                    Adapty.updateAttribution(it, AttributionType.APPSFLYER)
                }
            }

            override fun onAttributionFailure(errorMessage: String) {

            }
        }
        AppsFlyerLib.getInstance().init(API_KEY_APPSFLYER, conversionListener, applicationContext)
        AppsFlyerLib.getInstance().startTracking(this)
        // Branch logging for debugging
        Branch.enableLogging()

        // Branch object initialization
        Branch.getAutoInstance(this)



    }
}