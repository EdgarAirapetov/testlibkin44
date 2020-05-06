package com.adaptytest

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.google.gson.Gson
import kotlinx.android.synthetic.main.item_product.view.*

class ContainerAdapter(var items: ArrayList<DataContainer>, val context: Context, val callback: OnProductClickListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val container = items[position]
        holder.itemView.apply {
            title.text = container.attributes?.developerId.toString() ?: ""

            setOnClickListener { ResponseActivity.openResponseActivity(context, Gson().toJson(container.attributes?.products)) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            layoutInflater.inflate(
                R.layout.item_container,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun addItems(items: ArrayList<DataContainer>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var view = view
    }

}