package com.adapty.api

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.adapty.api.requests.BaseRequest
import com.adapty.api.requests.CreateProfileRequest
import com.adapty.api.requests.SyncMetaInstallRequest
import com.adapty.api.requests.UpdateProfileRequest
import com.adapty.api.responses.BaseResponse
import com.adapty.api.responses.CreateProfileResponse
import com.adapty.api.responses.SyncMetaInstallResponse
import com.adapty.api.responses.UpdateProfileResponse
import com.adapty.utils.PreferenceManager
import com.google.gson.Gson
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


const val PROD_URL = "https://api.adapty.io/api/v1/"
const val STAGE_URL = "https://api-stage.adapty.io/api/v1/"
const val DEV_URL = "https://api-dev.adapty.io/api/v1/"

const val PROD_API_KEY = "public_live_FWxDeuI4.z7ivzyEWRFQHkS1B3zbs"
const val STAGE_API_KEY = "public_live_STcyfoCh.MORWzZnQuvaHbYqBSDiq"
const val DEV_API_KEY = "public_live_7Ei6YwqY.8fRoPRhM2lngcCVXEPFU"

const val AUTHORIZATION_KEY = "Authorization"
const val API_KEY_PREFIX = "Api-Key "

class ApiClient(var context: Context) {

    private val SERVER_URL = DEV_URL
    private val CURRENT_API_KEY = DEV_API_KEY
    private val preferenceManager = PreferenceManager(context)

    companion object {
        val CREATE_PROFILE_REQ_ID = 0
        val UPDATE_PROFILE_REQ_ID = 1
        val SYNC_META_REQ_ID = 2
        val POST = "POST"
        val PATCH = "PATCH"
        val GET = "GET"
    }

    fun createProfile(request: CreateProfileRequest, iCallback : ICallback?) {
        post(generateUrl(CREATE_PROFILE_REQ_ID), request, CreateProfileResponse(), CREATE_PROFILE_REQ_ID, iCallback)
    }

    fun updateProfile(request: UpdateProfileRequest, iCallback : ICallback?) {
        patch(generateUrl(UPDATE_PROFILE_REQ_ID), request, UpdateProfileResponse(), UPDATE_PROFILE_REQ_ID, iCallback)
    }

    fun syncMeta(request: SyncMetaInstallRequest, iCallback : ICallback?) {
        post(generateUrl(SYNC_META_REQ_ID), request, SyncMetaInstallResponse(), SYNC_META_REQ_ID, iCallback)
    }

    private fun request(type: String, url: String, request: Any, oresponse: Any?, reqID: Int, iCallback : ICallback?) {
        val gson = Gson()

        Thread(Runnable {

            var rString = ""

            try {

                Log.d("$type URL", url)

                val req = gson.toJson(request)

                Log.d("$type REQ", req)

                val myUrl = URL(url)

                val conn = myUrl.openConnection() as HttpURLConnection

                conn.readTimeout = 10000 * 6
                conn.connectTimeout = 15000 * 4
                conn.requestMethod = type

                conn.setRequestProperty("Content-type", "application/vnd.api+json")

                conn.setRequestProperty("ADAPTY-SDK-PROFILE-ID", preferenceManager.profileID)
                conn.setRequestProperty("ADAPTY-SDK-PLATFORM", "Android")
                conn.setRequestProperty(AUTHORIZATION_KEY, API_KEY_PREFIX.plus(CURRENT_API_KEY))

                conn.doInput = true

                val os = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(req)
                writer.flush()
                writer.close()
                os.close()

                conn.connect()

                val response = conn.responseCode
                Log.d("$type RES", "The response is: $response")

                if (response == HttpURLConnection.HTTP_OK
                    || response == HttpURLConnection.HTTP_CREATED
                    || response == HttpURLConnection.HTTP_ACCEPTED
                    || response == HttpURLConnection.HTTP_NO_CONTENT
                    || response == 207
                    || response == 206
                ) {

                    val inputStream = conn.inputStream

                    rString = toStringUtf8(inputStream)
                    Log.d("response", rString)

                } else {
                    rString = toStringUtf8(conn.errorStream)
                    Log.d("error", rString)
                    val re = gson.fromJson(rString, BaseRequest::class.java)

                    fail("fail1", reqID, iCallback)
                    return@Runnable
                }
            } catch (e: Exception) {
                e.printStackTrace()

                if (rString.isEmpty()) {
                    fail("Server error", reqID, iCallback)
                    return@Runnable
                }
            }

            var responseObj: Any? = null

            try {
                if (oresponse != null) {
                    responseObj = gson.fromJson(rString, oresponse.javaClass)
                    if (responseObj is BaseResponse) {
                        fail("fail2", reqID, iCallback)
                        return@Runnable
                    }
                } else
                    responseObj = rString
                success(responseObj, reqID, iCallback)
            } catch (e: Exception) {
                e.printStackTrace()
                responseObj = rString
                success(responseObj, reqID, iCallback)
            }
        }).start()
    }

    private fun post(url: String, request: Any, oresponse: Any?, reqID: Int, iCallback : ICallback?) {
        request(POST, url, request, oresponse, reqID, iCallback)
    }

    private fun patch(url: String, request: Any, oresponse: Any?, reqID: Int, iCallback : ICallback?) {
        request(PATCH, url, request, oresponse, reqID, iCallback)
    }

    private fun success(response: Any?, reqID: Int, iCallback : ICallback?) {
        try {
            val mainHandler = Handler(context.mainLooper)
            val myRunnble = Runnable {
                    iCallback?.success(response, reqID)
            }
            mainHandler.post(myRunnble)
        } catch (e: Exception) {
            Log.e(TAG + " success", e.localizedMessage)
        }

    }

    private fun fail(error: String, reqID: Int, iCallback : ICallback?) {
        try {
            val mainHandlerE = Handler(context.getMainLooper())
            val myRunnbleE = Runnable {

                iCallback?.fail(error, reqID)
            }
            mainHandlerE.post(myRunnbleE)
        } catch (e: Exception) {
            Log.e(TAG + " fail", e.localizedMessage)
        }
    }

    private fun toStringUtf8(inputStream: InputStream): String{
        val r = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val total = StringBuilder()
        var line: String? = r.readLine()
        while (line != null) {
            total.append(line).append('\n')
            line = r.readLine()
        }
        return total.toString()
    }

    private fun generateUrl(reqId: Int): String{
        when (reqId) {
            CREATE_PROFILE_REQ_ID, UPDATE_PROFILE_REQ_ID ->
                return SERVER_URL + "sdk/analytics/profiles/" + preferenceManager.profileID + "/"
            SYNC_META_REQ_ID ->
                return SERVER_URL + "sdk/analytics/profiles/" + preferenceManager.profileID + "/installation-metas/" + preferenceManager.installationMetaID + "/"
            else -> return SERVER_URL
        }
    }
}
