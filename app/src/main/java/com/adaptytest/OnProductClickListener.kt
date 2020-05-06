package com.adaptytest

import com.adapty.api.entity.containers.Product

interface OnProductClickListener {
    fun onMakePurchaseClicked(product: Product)
}