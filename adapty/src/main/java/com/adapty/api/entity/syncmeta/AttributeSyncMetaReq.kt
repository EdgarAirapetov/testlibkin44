package com.adapty.api.entity.syncmeta

import com.google.gson.annotations.SerializedName

open class AttributeSyncMetaReq {
    @SerializedName("adapty_sdk_version")
    var adapty_sdk_version: String? = null

    @SerializedName("adapty_sdk_version_build")
    var adapty_sdk_version_build: Int? = null

    @SerializedName("app_build")
    var app_build: String? = null

    @SerializedName("app_version")
    var app_version: String? = null

    @SerializedName("device")
    var device: String? = null

    @SerializedName("locale")
    var locale: String? = null

    @SerializedName("os")
    var os: String? = null

    @SerializedName("platform")
    var platform: String? = null

    @SerializedName("timezone")
    var timezone: String? = null

    @SerializedName("attribution_network")
    var attribution_network: String? = null

    @SerializedName("attribution_campaign")
    var attribution_campaign: String? = null

    @SerializedName("attribution_tracker_token")
    var attribution_tracker_token: String? = null

    @SerializedName("attribution_tracker_name")
    var attribution_tracker_name: String? = null

    @SerializedName("attribution_adgroup")
    var attribution_adgroup: String? = null

    @SerializedName("attribution_creative")
    var attribution_creative: String? = null

    @SerializedName("attribution_click_label")
    var attribution_click_label: String? = null

    @SerializedName("attribution_adid")
    var attribution_adid: String? = null
}