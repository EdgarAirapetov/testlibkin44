package com.adapty.api.requests

import com.adapty.api.entity.attribution.DataUpdateAttributionReq
import com.adapty.api.entity.validate.DataRestoreReceiptReq
import com.adapty.api.entity.validate.DataValidateReceiptReq
import com.google.gson.annotations.SerializedName

class UpdateAttributionRequest {
    @SerializedName("data")
    var data: DataUpdateAttributionReq? = null

}