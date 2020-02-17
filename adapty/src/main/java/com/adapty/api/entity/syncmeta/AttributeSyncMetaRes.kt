package com.adapty.api.entity.syncmeta

import com.google.gson.annotations.SerializedName

open class AttributeSyncMetaRes : AttributeSyncMetaReq() {
    @SerializedName("app_id")
    var app_id: String? = null

    @SerializedName("profile_id")
    var profile_id: String? = null

    @SerializedName("adapty_sdk_version_int")
    var adapty_sdk_version_int: String? = null

    @SerializedName("device_token")
    var device_token: String? = null

    @SerializedName("cognito_id")
    var cognito_id: String? = null

    @SerializedName("iam_access_key_id")
    var iam_access_key_id: String? = null

    @SerializedName("iam_secret_key")
    var iam_secret_key: String? = null

    @SerializedName("iam_session_token")
    var iam_session_token: String? = null

    @SerializedName("iam_expiration")
    var iam_expiration: String? = null

    @SerializedName("last_active_at")
    var last_active_at: String? = null

    @SerializedName("created_at")
    var created_at: String? = null

    @SerializedName("updated_at")
    var updated_at: String? = null
}