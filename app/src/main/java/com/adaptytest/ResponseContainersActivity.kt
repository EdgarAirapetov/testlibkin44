package com.adaptytest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_response_containers.*
import java.lang.Exception


class ResponseContainersActivity : AppCompatActivity() {

    companion object {
        const val RESPONSE = "RESPONSE"
        const val CONTAINERS = "CONTAINERS"
        const val PRODUCTS = "PRODUCTS"

        fun openResponseActivity(context: Context, containers: ArrayList<DataContainer>, products: ArrayList<Product>, error: String) {
            val intent = Intent(context, ResponseContainersActivity::class.java)
            intent.putExtra(CONTAINERS, Gson().toJson(containers))
            intent.putExtra(PRODUCTS, Gson().toJson(products))
            intent.putExtra(RESPONSE, error)
            context.startActivity(intent)
        }
    }

    var containers: String = ""
    var products: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_response_containers)

        val extras = intent.extras
        extras?.let {

            containers = it.getString(CONTAINERS, "")
            products = it.getString(PRODUCTS, "")
            response.text = "$containers,\n$products"

            try {
                val myType = object : TypeToken<ArrayList<DataContainer>>() {}.type
                val containers =
                    Gson().fromJson<ArrayList<DataContainer>>(it.getString(CONTAINERS), myType)
                containers.first().attributes?.apply {
                    developerIdTv.text = developerId
                    revisionTv.text = revision.toString()
                    isWinbackTv.text = isWinback.toString()
                    variationIdTv.text = variationId
                    productsTv.text = Gson().toJson(products)
                }

            } catch (e: Exception) {

            }

            try {
                val myType2 = object : TypeToken<ArrayList<Product>>() {}.type
                val products =
                    Gson().fromJson<ArrayList<Product>>(it.getString(PRODUCTS), myType2)

                products.first().apply {
                    productTitleTv.text = localizedTitle
                    vendorProductIdTv.text = vendorProductId
                }

            } catch (e : Exception) {

            }
        }
    }
}
