package com.adapty.api

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.adapty.Adapty
import com.adapty.api.requests.*
import com.adapty.api.responses.*
import com.adapty.utils.ADAPTY_SDK_VERSION_INT
import com.adapty.utils.LogHelper
import com.adapty.utils.PreferenceManager
import com.google.gson.Gson
import java.io.*
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

const val AUTHORIZATION_KEY = "Authorization"
const val API_KEY_PREFIX = "Api-Key "
const val TAG = "[Adapty]"
const val TIMEOUT = 30 * 1000

class ApiClient(private var context: Context) {

    private val serverUrl = "https://api-dev-3e4bc4bb0378bf.adapty.io/api/v1/"

    //    private val serverUrl = "https://www.google.com:81/"
    private val preferenceManager = PreferenceManager(context)

    companion object {
        const val CREATE_PROFILE_REQ_ID = 0
        const val UPDATE_PROFILE_REQ_ID = 1
        const val SYNC_META_REQ_ID = 2
        const val VALIDATE_PURCHASE_REQ_ID = 3
        const val RESTORE_PURCHASE_REQ_ID = 4
        const val GET_PROFILE_REQ_ID = 5
        const val GET_CONTAINERS_REQ_ID = 6
        const val UPDATE_ATTRIBUTION_REQ_ID = 7
        const val POST = "POST"
        const val PUT = "PUT"
        const val PATCH = "PATCH"
        const val GET = "GET"
    }

    fun createProfile(
        request: CreateProfileRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(CREATE_PROFILE_REQ_ID),
            request,
            CreateProfileResponse(),
            CREATE_PROFILE_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun updateProfile(
        request: UpdateProfileRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        patch(
            generateUrl(UPDATE_PROFILE_REQ_ID),
            request,
            UpdateProfileResponse(),
            UPDATE_PROFILE_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun getProfile(
        request: PurchaserInfoRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        get(
            generateUrl(GET_PROFILE_REQ_ID),
            request,
            PurchaserInfoResponse(),
            GET_PROFILE_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun getPurchaseContainers(
        request: PurchaseContainersRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        get(
            generateUrl(GET_CONTAINERS_REQ_ID),
            request,
            PurchaseContainersResponse(),
            GET_CONTAINERS_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun syncMeta(
        request: SyncMetaInstallRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(SYNC_META_REQ_ID),
            request,
            SyncMetaInstallResponse(),
            SYNC_META_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun validatePurchase(
        request: ValidateReceiptRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(VALIDATE_PURCHASE_REQ_ID),
            request,
            ValidateReceiptResponse(),
            VALIDATE_PURCHASE_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun restorePurchase(
        request: RestoreReceiptRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(RESTORE_PURCHASE_REQ_ID),
            request,
            RestoreReceiptResponse(),
            RESTORE_PURCHASE_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    fun updateAttribution(
        request: UpdateAttributionRequest,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(UPDATE_ATTRIBUTION_REQ_ID),
            request,
            Any(),
            UPDATE_ATTRIBUTION_REQ_ID,
            currentLooper,
            adaptyCallback
        )
    }

    val gson = Gson()

    private fun request(
        type: String,
        url: String,
        request: Any,
        oresponse: Any?,
        reqID: Int,
        currentLooper: Handler? = null,
        adaptyCallback: AdaptyCallback?
    ) {

        Thread(Runnable {

            var rString = ""

            try {

                val req = gson.toJson(request)

                val myUrl = URL(url)

                val conn = myUrl.openConnection() as HttpURLConnection

                conn.readTimeout = TIMEOUT
                conn.connectTimeout = TIMEOUT
                conn.requestMethod = type

                conn.setRequestProperty("Content-type", "application/vnd.api+json")

                conn.setRequestProperty("ADAPTY-SDK-PROFILE-ID", preferenceManager.profileID)
                conn.setRequestProperty("ADAPTY-SDK-PLATFORM", "Android")
                conn.setRequestProperty("ADAPTY-SDK-VERSION", com.adapty.BuildConfig.VERSION_NAME)
                conn.setRequestProperty(
                    "ADAPTY-SDK-VERSION-BUILD",
                    ADAPTY_SDK_VERSION_INT.toString()
                )
                conn.setRequestProperty(
                    AUTHORIZATION_KEY,
                    API_KEY_PREFIX.plus(preferenceManager.appKey)
                )

                conn.setRequestProperty("Connection", "close")
                System.setProperty("java.net.preferIPv4Stack", "true")
                System.setProperty("http.keepAlive", "false")

                conn.doInput = true

                if (type != GET) {
                    conn.doOutput = true
                    val os = conn.outputStream
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(req)
                    writer.flush()
                    writer.close()
                    os.close()
                }

                conn.connect()

                val response = conn.responseCode

                if (response == HttpURLConnection.HTTP_OK
                    || response == HttpURLConnection.HTTP_CREATED
                    || response == HttpURLConnection.HTTP_ACCEPTED
                    || response == HttpURLConnection.HTTP_NO_CONTENT
                    || response == 207
                    || response == 206
                ) {

                    val inputStream = conn.inputStream

                    rString = toStringUtf8(inputStream)
                    LogHelper.logVerbose("Response $myUrl: $rString")

                } else {
                    rString = toStringUtf8(conn.errorStream)
                    fail(
                        "Request is unsuccessful. Url: $myUrl Response Code: $response, Message: $rString",
                        reqID,
                        currentLooper,
                        adaptyCallback
                    )
                    return@Runnable
                }

            } catch (e: Exception) {
                e.printStackTrace()

                fail(
                    "Request Exception. ${e.message} ${e.localizedMessage} Message: ${rString ?: ""}",
                    reqID,
                    currentLooper,
                    adaptyCallback
                )
                return@Runnable
            }

            var responseObj: Any?

            try {
                responseObj = if (oresponse != null) {
                    gson.fromJson(rString, oresponse.javaClass)
                } else
                    rString
                success(responseObj, reqID, currentLooper, adaptyCallback)
            } catch (e: Exception) {
                e.printStackTrace()
                responseObj = rString
                success(responseObj, reqID, currentLooper, adaptyCallback)
            }
        }).start()
    }

    private fun post(
        url: String,
        request: Any,
        oresponse: Any?,
        reqID: Int,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        request(POST, url, request, oresponse, reqID, currentLooper, adaptyCallback)
    }

    private fun patch(
        url: String,
        request: Any,
        oresponse: Any?,
        reqID: Int,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        request(PATCH, url, request, oresponse, reqID, currentLooper, adaptyCallback)
    }

    private fun get(
        url: String,
        request: Any,
        oresponse: Any?,
        reqID: Int,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        request(GET, url, request, oresponse, reqID, currentLooper, adaptyCallback)
    }

    private fun success(
        response: Any?,
        reqID: Int,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        LogHelper.logVerbose("Response success $reqID")
        try {
            val mainHandler = currentLooper ?: Handler(context.mainLooper)
            val myRunnable = Runnable {
                adaptyCallback?.let {
                    when (it) {
                        is AdaptySystemCallback -> {
                            it.success(response, reqID)
                        }
                        is AdaptyProfileCallback -> {
                            it.onResult(null)
                        }
                        is AdaptyValidateCallback -> {
                            it.onResult((response as ValidateReceiptResponse), null)
                        }
                        is AdaptyRestoreCallback -> {
                            it.onResult((response as RestoreReceiptResponse), null)
                        }
                        is AdaptyPurchaserInfoCallback -> {
                            val res = (response as PurchaserInfoResponse).data?.attributes
                            it.onResult(res, null)
                        }
                        is AdaptyPurchaseContainersCallback -> {
                            var data = (response as PurchaseContainersResponse).data
                            if (data == null)
                                data = arrayListOf()

                            var meta = (response).meta?.products
                            if (meta == null)
                                meta = arrayListOf()
                            it.onResult(data, meta, null)
                        }
                    }
                }
//                if (Looper.myLooper() != Looper.getMainLooper())
//                    Looper.myLooper()?.quit()
            }
            mainHandler.post(myRunnable)
        } catch (e: Exception) {
            LogHelper.logError("Callback success error $reqID: ${e.message} ${e.localizedMessage}")
        }
    }

    private fun fail(
        error: String,
        reqID: Int,
        currentLooper: Handler?,
        adaptyCallback: AdaptyCallback?
    ) {
        LogHelper.logError("Request failed $reqID $error")
        try {

            val mainHandlerE = currentLooper ?: Handler(context.mainLooper)
            val myRunnableE = Runnable {
                adaptyCallback?.let {
                    when (it) {
                        is AdaptySystemCallback -> {
                            it.fail(error, reqID)
                        }
                        is AdaptyProfileCallback -> {
                            it.onResult(error)
                        }
                        is AdaptyValidateCallback -> {
                            it.onResult(null, error)
                        }
                        is AdaptyRestoreCallback -> {
                            it.onResult(null, error)
                        }
                        is AdaptyPurchaserInfoCallback -> {
                            it.onResult(null, error)
                        }
                        is AdaptyPurchaseContainersCallback -> {
                            it.onResult(arrayListOf(), arrayListOf(), error)
                        }
                    }
                }
                if (Looper.myLooper() != Looper.getMainLooper())
                    Looper.myLooper()?.quit()
                Looper.myLooper() = null
            }
            mainHandlerE.post(myRunnableE)
        } catch (e: Exception) {
            LogHelper.logError("Callback Fail error $reqID: ${e.message} ${e.localizedMessage}")
        }
    }

    private fun toStringUtf8(inputStream: InputStream): String {
        val r = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val total = StringBuilder()
        var line: String? = r.readLine()
        while (line != null) {
            total.append(line).append('\n')
            line = r.readLine()
        }
        return total.toString()
    }

    private fun generateUrl(reqId: Int): String {
        return when (reqId) {
            CREATE_PROFILE_REQ_ID, UPDATE_PROFILE_REQ_ID, GET_PROFILE_REQ_ID ->
                serverUrl + "sdk/analytics/profiles/" + preferenceManager.profileID + "/"
            SYNC_META_REQ_ID ->
                serverUrl + "sdk/analytics/profiles/" + preferenceManager.profileID + "/installation-metas/" + preferenceManager.installationMetaID + "/"
            VALIDATE_PURCHASE_REQ_ID ->
                serverUrl + "sdk/in-apps/google/token/validate/"
            RESTORE_PURCHASE_REQ_ID ->
                serverUrl + "sdk/in-apps/google/token/restore/"
            GET_CONTAINERS_REQ_ID ->
                serverUrl + "sdk/in-apps/purchase-containers/?profile_id=" + preferenceManager.profileID
            UPDATE_ATTRIBUTION_REQ_ID ->
                serverUrl + "sdk/analytics/profiles/" + preferenceManager.profileID + "/attribution/"
            else -> serverUrl
        }
    }
}
