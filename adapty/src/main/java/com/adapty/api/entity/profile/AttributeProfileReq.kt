package com.adapty.api.entity.profile

import com.google.gson.annotations.SerializedName

open class AttributeProfileReq {
    @SerializedName("customer_user_id")
    var customer_user_id: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("phone_number")
    var phone_number: String? = null

    @SerializedName("idfa")
    var idfa: String? = null

    @SerializedName("facebook_user_id")
    var facebook_user_id: String? = null

    @SerializedName("first_name")
    var first_name: String? = null

    @SerializedName("last_name")
    var last_name: String? = null

    @SerializedName("gender")
    var gender: String? = null

    @SerializedName("birthday")
    var birthday: String? = null
}