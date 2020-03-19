package com.adapty.api.entity.containers

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class MetaGetContainerRes {
    @SerializedName("products")
    var products: ArrayList<ProductContainer>? = null
}