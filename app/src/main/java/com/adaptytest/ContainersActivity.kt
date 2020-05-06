package com.adaptytest

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.adapty.Adapty
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_containers.*
import kotlinx.android.synthetic.main.activity_response.*

class ContainersActivity : AppCompatActivity(), OnProductClickListener {

    companion object {
        const val DATA = "DATA"
        const val META = "META"

        fun openActivity(context: Context, data: String, meta: String) {
            val intent = Intent(context, ContainersActivity::class.java)
            intent.putExtra(DATA, data)
            intent.putExtra(META, meta)
            context.startActivity(intent)
        }
    }

    private var adapterData: ContainerAdapter? = null
    private var adapterMeta: ProductAdapter? = null
    private var dataArray: ArrayList<DataContainer>? = null
    private var metaArray: ArrayList<Product>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_containers)

        progress.setOnTouchListener { v, event ->  return@setOnTouchListener true}

        val extras = intent.extras
        extras?.let {
            val myType = object : TypeToken<ArrayList<DataContainer>>() {}.type
            val containers =
                Gson().fromJson<ArrayList<DataContainer>>(
                    it.getString(DATA), myType
                )

            val myType2 = object : TypeToken<ArrayList<Product>>() {}.type
            val products =
                Gson().fromJson<ArrayList<Product>>(it.getString(META), myType2)
//
//            val dataProducts = arrayListOf<Product>()
//            for (c in containers)
//                c.attributes?.products?.let {
//                    dataProducts.addAll(it)
//                }

            adapterData = ContainerAdapter(containers, this, this)

            data_rv.layoutManager = LinearLayoutManager(this, VERTICAL, false)
            data_rv.adapter = adapterData

            data_rv.addItemDecoration(
                com.adaptytest.DividerItemDecoration(this, R.drawable.horizontal_divider)
            )

            if (!containers.isNullOrEmpty()) {
                meta_tv.visibility = View.GONE
                meta_rv.visibility = View.GONE
            } else {
                adapterMeta = ProductAdapter(products, this, this)

                meta_rv.layoutManager = LinearLayoutManager(this, VERTICAL, false)
                meta_rv.adapter = adapterMeta

                meta_rv.addItemDecoration(
                    com.adaptytest.DividerItemDecoration(this, R.drawable.horizontal_divider)
                )
            }
        }
    }

    override fun onMakePurchaseClicked(product: Product) {
        progress.visibility = VISIBLE
        Adapty.makePurchase(this, product) { purchase, response, error ->
            progress.visibility = GONE
            if (error == null) {
                Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
//                receipt_et.setText(purchase?.purchaseToken)
                return@makePurchase
            }

            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }
}
