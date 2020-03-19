package com.adaptytest

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.adapty.Adapty
import com.adapty.api.requests.CreateProfileRequest
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.purchase.INAPP
import com.adapty.purchase.SUBS
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_response.*

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

        Adapty.activate(applicationContext, "public_live_7Ei6YwqY.8fRoPRhM2lngcCVXEPFU", null) {error ->
            progressDialog?.dismiss()

            if (error != null)
                errorsTv.text = error
        }

        radioType.setOnCheckedChangeListener { radioGroup, checkedId ->
            purchaseType = when (checkedId) {
                R.id.type_subs -> SUBS
                R.id.type_in_app -> INAPP
                else -> SUBS
            }
        }

        radioSubs.setOnCheckedChangeListener { p0, checkedId ->
            selectedSubs = when (checkedId) {
                R.id.first_subs -> firstSubs
                R.id.second_subs -> secondSubs
                else -> firstSubs
            }

            product_et.setText(selectedSubs)
        }

        first_subs.isChecked = true
        type_subs.isChecked = true

        makePurchase.setOnClickListener {
            val productId = product_et.text.toString()
            if (productId.isEmpty()) {
                Toast.makeText(this, "Input or choose product_id", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Adapty.makePurchase(
                this,
                purchaseType,
                product_et.text.toString()
            ) { purchase, response, error ->
                if (error == null) {
                    errorsTv.text = "Success"
                    receipt_et.setText(purchase?.purchaseToken)
                    return@makePurchase
                }

                errorsTv.text = error
            }

        }

        validateReceipt.setOnClickListener {
            if (receipt_et.length() == 0)
                return@setOnClickListener

            Adapty.validatePurchase(
                purchaseType,
                product_et.text.toString(),
                receipt_et.text.toString()
            ) { response: ValidateReceiptResponse?, error: String? ->
                if (error == null) {
                    errorsTv.text = "Success"
                    return@validatePurchase
                }

                errorsTv.text = error
            }
        }

        restore.setOnClickListener {
            Adapty.restorePurchases(this) { response, error ->
                if (error == null) {
                    errorsTv.text = "Success"
                    Toast.makeText(this, Gson().toJson(response), Toast.LENGTH_LONG).show()
                    return@restorePurchases
                }

                errorsTv.text = error
            }
        }

        purchaser_info.setOnClickListener {
            Adapty.getPurchaserInfo { purchaserInfo, error ->
                purchaserInfo?.let {
                    errorsTv.text = "Success"
                    ResponseActivity.openResponseActivity(this, Gson().toJson(it))
                } ?: kotlin.run {
                    errorsTv.text = error ?: "Empty response attributes"
                }
            }
        }

        containers_get.setOnClickListener {
            Adapty.getPurchaseContainers { containers, products, error ->
                if (error == null) {
                    ResponseContainersActivity.openResponseActivity(this, containers, products, "")
                } else
                    errorsTv.text = error
            }
        }
    }
}
