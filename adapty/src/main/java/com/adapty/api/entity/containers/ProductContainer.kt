package com.adapty.api.entity.containers

import com.google.gson.annotations.SerializedName

class ProductContainer {
    @SerializedName("title")
    var title: String? = null

    @SerializedName("vendor_product_id")
    var vendorProductId: String? = null
}