package com.adapty.api.entity.profile

import com.google.gson.annotations.SerializedName
import java.util.*

class AttributeProfileRes : AttributeProfileReq() {
    @SerializedName("profile_id")
    var profile_id: String? = null

    @SerializedName("app_id")
    var app_id: String? = null

    @SerializedName("amplitude_user_id")
    var amplitude_user_id: String? = null

    @SerializedName("mixpanel_user_id")
    var mixpanel_user_id: String? = null

    @SerializedName("cognito_id")
    var cognito_id: String? = null

    @SerializedName("created_at")
    var created_at: Date? = null

    @SerializedName("updated_at")
    var updated_at: Date? = null
}