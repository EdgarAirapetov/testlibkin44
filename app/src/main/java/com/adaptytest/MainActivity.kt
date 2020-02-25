package com.adaptytest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.adapty.Adapty
import com.adapty.api.AdaptyCallback
import com.adapty.api.AdaptyPurchaseCallback
import com.adapty.api.AdaptyRestoreCallback
import com.adapty.api.AdaptyValidateCallback
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.purchase.INAPP
import com.adapty.purchase.InAppPurchases
import com.adapty.purchase.SUBS
import com.android.billingclient.api.Purchase
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val firstSubs = "adapty_test_1"
    val secondSubs = "adapty_test_2"
    private var selectedSubs = firstSubs
    private var purchaseType = SUBS


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Adapty.activate(applicationContext, "public_live_7Ei6YwqY.8fRoPRhM2lngcCVXEPFU")

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
                product_et.text.toString(),
                object : AdaptyPurchaseCallback {
                    override fun onResult(response: Purchase?, error: String?) {
                        if (error == null) {
                            errorsTv.text = "Success"
                            receipt_et.setText(response?.purchaseToken)
                            return
                        }

                        errorsTv.text = error
                    }
                })
        }

        validateReceipt.setOnClickListener {
            if (receipt_et.length() == 0)
                return@setOnClickListener

            Adapty.validateReceipt(product_et.text.toString(), receipt_et.text.toString(), object : AdaptyValidateCallback {
                override fun onResult(response: ValidateReceiptResponse?, error: String?) {
                    if (error == null) {
                        errorsTv.text = "Success"
                        return
                    }

                    errorsTv.text = error
                }
            })
        }

        restore.setOnClickListener {
            Adapty.restore(this, purchaseType, object : AdaptyRestoreCallback {
                override fun onResult(error: String?) {
                    if (error == null) {
                        errorsTv.text = "Success"
                        return
                    }

                    errorsTv.text = error
                }

            })
        }
    }
}
