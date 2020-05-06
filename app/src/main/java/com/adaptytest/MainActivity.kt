package com.adaptytest

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.adapty.Adapty
import com.adapty.api.entity.purchaserInfo.OnPurchaserInfoUpdatedListener
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.purchase.SUBS
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val firstSubs = "adapty_test_1"
    val secondSubs = "adapty_test_2"
    private var selectedSubs = firstSubs
    private var purchaseType = SUBS
    private var progressDialog: ProgressDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressDialog = ProgressDialog(this)

        progressDialog?.show()

        Adapty.activate(
            this,
            "public_live_7Ei6YwqY.8fRoPRhM2lngcCVXEPFU",
            null
        ) { error ->
            progressDialog?.dismiss()

            profile_id_tv.text = Adapty.getProfileId()

            if (error != null)
                errorsTv.text = error
        }

        Adapty.getPurchaserInfo { purchaserInfo, state, error ->  }
        Adapty.restorePurchases(this) {_, _ ->}
        Adapty.logout {  }
//        Adapty.getPurchaseContainers(this) {_, _, _, _ -> }

        Adapty.setOnPurchaserInfoUpdatedListener(object : OnPurchaserInfoUpdatedListener {
            override fun didReceiveUpdatedPurchaserInfo(purchaserInfo: PurchaserInfoModel) {
                profile_id_tv.text = Adapty.getProfileId()
                Toast.makeText(this@MainActivity, "Updated purchase info", Toast.LENGTH_LONG).show()
            }
        })

        restore.setOnClickListener {
            progressDialog?.show()
            Adapty.restorePurchases(this) { response, error ->
                progressDialog?.dismiss()
                if (error == null) {
                    errorsTv.text = Gson().toJson(response)
                    return@restorePurchases
                }

                errorsTv.text = error

                profile_id_tv.text = Adapty.getProfileId()
            }
        }

        purchaser_info.setOnClickListener {
            progressDialog?.show()
            Adapty.getPurchaserInfo { purchaserInfo, state, error ->
                progressDialog?.dismiss()
                profile_id_tv.text = Adapty.getProfileId()
                purchaserInfo?.let {
                    errorsTv.text = "state: $state \n ${Gson().toJson(it)}"
                } ?: kotlin.run {
                    errorsTv.text = error ?: "Empty response attributes"
                }
            }
        }

        containers_get_cached.setOnClickListener {
            progressDialog?.show()
            Adapty.getPurchaseContainers(this) { containers, products, state, error ->
                progressDialog?.dismiss()
                profile_id_tv.text = Adapty.getProfileId()
                if (error == null) {
                    if (state != "synced")
                        ContainersActivity.openActivity(
                            this,
                            Gson().toJson(containers),
                            Gson().toJson(products)
                        )
                } else
                    errorsTv.text = error
            }
        }

        containers_get_synced.setOnClickListener {
            progressDialog?.show()
            Adapty.getPurchaseContainers(this) { containers, products, state, error ->
                profile_id_tv.text = Adapty.getProfileId()
                if (error == null) {
                    if (state == "synced") {
                        progressDialog?.dismiss()
                        ContainersActivity.openActivity(
                            this,
                            Gson().toJson(containers),
                            Gson().toJson(products)
                        )
                    }
                } else {
                    errorsTv.text = error
                    progressDialog?.dismiss()
                }
            }
        }

        identify.setOnClickListener {
            val cui = customer_user_id_et.text.toString()
            progressDialog?.show()
            Adapty.identify(cui) { error: String? ->
                progressDialog?.dismiss()
                profile_id_tv.text = Adapty.getProfileId()
//                errorsTv.text = error ?: "Successfully identified user"

                Adapty.getPurchaserInfo()?.let {
                    errorsTv.text =
                        error ?: "Successfully identified user\n\n ${Gson().toJson(it)}"
                } ?: kotlin.run {
                    errorsTv.text = error ?: "Successfully identified user"
                }
            }
        }

        logout.setOnClickListener {
            progressDialog?.show()
            Adapty.logout { error ->
                progressDialog?.dismiss()
                profile_id_tv.text = Adapty.getProfileId()
                Adapty.getPurchaserInfo()?.let {
                    errorsTv.text = error ?: "Successfully log out\n\n ${Gson().toJson(it)}"
                } ?: kotlin.run {
                    errorsTv.text = error ?: "Successfully log out"
                }

            }
        }
    }
}
