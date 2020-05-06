package com.adaptytest

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.adapty.Adapty
import com.adapty.api.entity.containers.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_containers.*
import kotlinx.android.synthetic.main.activity_response.*

class ResponseActivity : AppCompatActivity(), OnProductClickListener {

    companion object {
        const val RESPONSE = "RESPONSE"

        fun openResponseActivity(context: Context, response: String) {
            val intent = Intent(context, ResponseActivity::class.java)
            intent.putExtra(RESPONSE, response)
            context.startActivity(intent)
        }
    }

    lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response)

        progressDialog = ProgressDialog(this)

        val extras = intent.extras
        extras?.let {
            val myType2 = object : TypeToken<ArrayList<Product>>() {}.type
            val products =
                Gson().fromJson<ArrayList<Product>>(it.getString(RESPONSE), myType2)

            if (products.isNullOrEmpty())
                no_products.visibility = View.VISIBLE

            products_rv.layoutManager = LinearLayoutManager(this)
            products_rv.adapter = ProductAdapter(products, this, this)
        }
    }

    override fun onMakePurchaseClicked(product: Product) {
        progressDialog.show()
        Adapty.makePurchase(this, product) { purchase, response, error ->
            progressDialog.dismiss()
            if (error == null) {
                Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
//                receipt_et.setText(purchase?.purchaseToken)
                return@makePurchase
            }

            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }
}
