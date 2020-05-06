package com.adaptytest

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adapty.api.entity.containers.Product
import kotlinx.android.synthetic.main.item_product.view.*

class ProductAdapter(var items: ArrayList<Product>, val context: Context, val callback: OnProductClickListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val product = items[position]
        holder.itemView.apply {
            title.text = product.localizedTitle.toString()
            product_id_tv.text = product.vendorProductId
            original_price_tv.text = product.price
//            if (product.discountPrice.isNullOrEmpty())
                discount_layout.visibility = View.GONE
//            else {
//                discount_layout.visibility = View.VISIBLE
//                discount_price_tv.text = product.discountPrice
//            }
            price_locale_tv.text = product.currencyCode
            type_tv.text = product.skuDetails?.type ?: ""

            make_purchase_btn.setOnClickListener {
                callback.onMakePurchaseClicked(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            layoutInflater.inflate(
                R.layout.item_product,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun addItems(items: ArrayList<Product>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var view = view
    }

}