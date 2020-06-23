package com.adapty

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.adapty.api.*
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.adapty.api.entity.purchaserInfo.*
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.utils.PreferenceManager
import com.adapty.api.responses.CreateProfileResponse
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.SyncMetaInstallResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.purchase.InAppPurchases
import com.adapty.purchase.InAppPurchasesInfo
import com.adapty.utils.KinesisManager
import com.adapty.utils.LogHelper
//import com.adapty.utils.KinesisManager
import com.adapty.utils.generatePurchaserInfoModel
import com.android.billingclient.api.Purchase
import kotlin.collections.ArrayList

class Adapty {

    companion object {
        lateinit var context: Context
        private lateinit var preferenceManager: PreferenceManager
        private var onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener? = null
        private var requestQueue: ArrayList<() -> Unit> = arrayListOf()
        private var handlerPurchasesHistory = Handler()
        private var runnablePurchasesHistory: Runnable? = null
        private var SYNC_PURCHASES_INTERVAL = (10 * 1000).toLong()
        private var isActivated = false
        private var kinesisManager: KinesisManager? = null
        var handler: Handler? = null

        fun activate(
            context: Context,
            appKey: String
        ) =
            activate(context, appKey, null, null)

        fun activate(
            context: Context,
            appKey: String,
            customerUserId: String?
        ) {
            activate(context, appKey, customerUserId, null)
        }

        private fun activate(
            context: Context,
            appKey: String,
            customerUserId: String?,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            LogHelper.logVerbose("activate($appKey, ${customerUserId ?: ""})")
            val currentLooper = null
            if (isActivated)
                return

            isActivated = true

            this.context = context
            this.preferenceManager = PreferenceManager(this.context)
            this.preferenceManager.appKey = appKey

            addToQueue {
                activateInQueue(context, appKey, customerUserId, currentLooper, adaptyCallback)
            }
        }

        private fun activateInQueue(
            context: Context,
            appKey: String,
            customerUserId: String?,
            currentLooper: Handler?,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            this.context = context
            this.preferenceManager = PreferenceManager(this.context)
            this.preferenceManager.appKey = appKey

            if (preferenceManager.profileID.isEmpty()) {
                ApiClientRepository.getInstance(preferenceManager)
                    .createProfile(customerUserId, currentLooper, object : AdaptySystemCallback {
                        override fun success(response: Any?, reqID: Int) {
                            if (response is CreateProfileResponse) {
                                response.data?.attributes?.apply {
                                    profileId?.let {
                                        preferenceManager.profileID = it
                                    }
                                    this.customerUserId?.let {
                                        preferenceManager.customerUserID = it
                                    }

                                    checkChangesPurchaserInfo(this)
                                }
                            }

                            adaptyCallback?.invoke(null)

                            nextQueue()

                            getStartedPurchaseContainers(context)

                            sendSyncMetaInstallRequest(context)

                            syncPurchasesBody(Companion.context, currentLooper, null)

//                            periodicSyncPurchases(context)

                        }

                        override fun fail(msg: String, reqID: Int) {
                            adaptyCallback?.invoke(msg)
                            nextQueue()
                        }
                    })
            } else {
                makeStartRequests(adaptyCallback)
            }
        }

        private fun nextQueue() {
            if (requestQueue.isNotEmpty())
                requestQueue.removeAt(0)

            if (requestQueue.isNotEmpty())
                requestQueue.first().invoke()
        }

        private fun addToQueue(action: () -> Unit) {
            requestQueue.add { action() }

            if (requestQueue.size == 1)
                requestQueue[0].invoke()
        }

        private fun makeStartRequests(adaptyCallback: ((String?) -> Unit)?) {
            sendSyncMetaInstallRequest(context)

            getStartedPurchaseContainers(context)

            var isCallbackSent = false
            val currentLooper = null
            getPurchaserInfo(false, currentLooper) { info, state, error ->
                if (!isCallbackSent) {
                    isCallbackSent = true
                    adaptyCallback?.invoke(error)
                    nextQueue()
                    return@getPurchaserInfo
                }

                if (isPurchaserInfoChanged(info))
                    info?.let {
                        onPurchaserInfoUpdatedListener?.didReceiveUpdatedPurchaserInfo(it)
                    }
            }

            syncPurchasesBody(context, currentLooper, null)
        }

        private fun checkChangesPurchaserInfo(res: AttributePurchaserInfoRes) {
            val purchaserInfo = generatePurchaserInfoModel(res)
            if (isPurchaserInfoChanged(purchaserInfo)) {
                onPurchaserInfoUpdatedListener?.didReceiveUpdatedPurchaserInfo(purchaserInfo)
            }
            preferenceManager.purchaserInfo = purchaserInfo
        }

        private fun isPurchaserInfoChanged(info: PurchaserInfoModel?): Boolean {
            val cachedInfo = preferenceManager.purchaserInfo
            if (cachedInfo == null && info != null)
                return true

            cachedInfo?.let { cached ->
                info?.let { synced ->
                    if (cached == synced)
                        return false

                    return true
                }
            }

            return false
        }

        fun sendSyncMetaInstallRequest(applicationContext: Context) {
            LogHelper.logVerbose("sendSyncMetaInstallRequest()")
            val currentLooper = null
            ApiClientRepository.getInstance(preferenceManager)
                .syncMetaInstall(applicationContext, currentLooper, object : AdaptySystemCallback {
                    override fun success(response: Any?, reqID: Int) {
                        if (response is SyncMetaInstallResponse) {
                            response.data?.id?.let {
                                preferenceManager.installationMetaID = it
                            }

                            response.data?.attributes?.iamAccessKeyId?.let {
                                preferenceManager.iamAccessKeyId = it
                            }
                            response.data?.attributes?.iamSecretKey?.let {
                                preferenceManager.iamSecretKey = it
                            }
                            response.data?.attributes?.iamSessionToken?.let {
                                preferenceManager.iamSessionToken = it
                            }

                            setupTrackingEvent()
                        }
                    }

                    override fun fail(msg: String, reqID: Int) {
                    }

                })
        }

        private val handlerEvent = Handler()
        private const val TRACKING_INTERVAL = (60 * 1000).toLong()

        private fun setupTrackingEvent() {
            handlerEvent.removeCallbacksAndMessages(null)
            handlerEvent.post {
                if (kinesisManager == null) kinesisManager = KinesisManager(preferenceManager)
                kinesisManager?.trackEvent()
                handlerEvent.postDelayed({
                    setupTrackingEvent()
                }, TRACKING_INTERVAL)
            }
        }

        fun identify(customerUserId: String?, adaptyCallback: (String?) -> Unit) {
            LogHelper.logVerbose("identify()")
            val currentLooper = null
            addToQueue { identifyInQueue(customerUserId, currentLooper, adaptyCallback) }
        }

        private fun identifyInQueue(
            customerUserId: String?,
            currentLooper: Handler?,
            adaptyCallback: (String?) -> Unit
        ) {
            if (!customerUserId.isNullOrEmpty() && preferenceManager.customerUserID.isNotEmpty()) {
                if (customerUserId == preferenceManager.customerUserID) {
                    adaptyCallback.invoke(null)
                    nextQueue()
                    return
                }
            }

            ApiClientRepository.getInstance(preferenceManager)
                .createProfile(customerUserId, currentLooper, object : AdaptySystemCallback {
                    override fun success(response: Any?, reqID: Int) {
                        if (response is CreateProfileResponse) {
                            response.data?.attributes?.apply {
                                profileId?.let {
                                    preferenceManager.profileID = it
                                }
                                this.customerUserId?.let {
                                    preferenceManager.customerUserID = it
                                }

                                checkChangesPurchaserInfo(this)
                            }
                        }

                        adaptyCallback.invoke(null)

                        nextQueue()

                        preferenceManager.products = arrayListOf()
                        preferenceManager.containers = null

                        getStartedPurchaseContainers(context)

                        sendSyncMetaInstallRequest(context)

                        syncPurchasesBody(context, currentLooper, null)
                    }

                    override fun fail(msg: String, reqID: Int) {
                        adaptyCallback.invoke(msg)

                        nextQueue()
                    }

                })
        }

        fun updateProfile(
            email: String?,
            phoneNumber: String?,
            facebookUserId: String?,
            mixpanelUserId: String?,
            amplitudeUserId: String?,
            amplitudeDeviceId: String?,
            firstName: String?,
            lastName: String?,
            gender: String?,
            birthday: String?, adaptyCallback: (String?) -> Unit
        ) {
            LogHelper.logVerbose("updateProfile()")
            val currentLooper = null
            addToQueue {
                ApiClientRepository.getInstance(preferenceManager).updateProfile(
                    email,
                    phoneNumber,
                    facebookUserId,
                    mixpanelUserId,
                    amplitudeUserId,
                    amplitudeDeviceId,
                    firstName,
                    lastName,
                    gender,
                    birthday,
                    currentLooper,
                    object : AdaptyProfileCallback {
                        override fun onResult(error: String?) {
                            adaptyCallback.invoke(error)
                            nextQueue()
                        }

                    }
                )
            }
        }

        private fun getPurchaserInfo(
            needQueue: Boolean,
            currentLooper: Handler?,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, state: String, error: String?) -> Unit
        ) {
            val info = preferenceManager.purchaserInfo
            info?.let {
                adaptyCallback.invoke(it, "cached", null)
            }

            ApiClientRepository.getInstance(preferenceManager).getProfile(currentLooper,
                object : AdaptyPurchaserInfoCallback {
                    override fun onResult(response: AttributePurchaserInfoRes?, error: String?) {
                        response?.let {
                            val purchaserInfo = generatePurchaserInfoModel(it)
                            adaptyCallback.invoke(purchaserInfo, "synced", error)
                            preferenceManager.purchaserInfo = purchaserInfo
                        } ?: kotlin.run {
                            adaptyCallback.invoke(null, "synced", error)
                        }

                        if (needQueue) {
                            nextQueue()
                        }
                    }
                }
            )
        }

        fun getPurchaserInfo(
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, state: String, error: String?) -> Unit
        ) {
            var handler: Handler? = null
            if (Thread.currentThread() != Looper.getMainLooper().thread) {
                if (Looper.myLooper() == null)
                    Looper.prepare()
//            val currentLooper = null
                handler = Handler()
            }
            addToQueue { getPurchaserInfo(true, handler, adaptyCallback) }
            if (Thread.currentThread() != Looper.getMainLooper().thread)
                Looper.loop()
        }

        private fun getStartedPurchaseContainers(context: Context) {
            val currentLooper = null
            getPurchaseContainersInQueue(
                context,
                false,
                currentLooper
            ) { containers, products, state, error -> }
        }

        fun getPurchaseContainers(
            activity: Activity,
            adaptyCallback: (containers: ArrayList<DataContainer>, products: ArrayList<Product>, state: String, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("getPurchaseContainers()")
            val currentLooper = null
            addToQueue {
                getPurchaseContainersInQueue(activity, true, currentLooper, adaptyCallback)
            }
        }

        private fun getPurchaseContainersInQueue(
            context: Context,
            needQueue: Boolean,
            currentLooper: Handler?,
            adaptyCallback: (containers: ArrayList<DataContainer>, products: ArrayList<Product>, state: String, error: String?) -> Unit
        ) {
            val cntrs = preferenceManager.containers
            cntrs?.let {
                adaptyCallback.invoke(it, preferenceManager.products, "cached", null)
            }

            ApiClientRepository.getInstance(preferenceManager).getPurchaseContainers(currentLooper,
                object : AdaptyPurchaseContainersCallback {
                    override fun onResult(
                        containers: ArrayList<DataContainer>,
                        products: ArrayList<Product>,
                        error: String?
                    ) {

                        if (!error.isNullOrEmpty()) {
                            adaptyCallback.invoke(arrayListOf(), arrayListOf(), "synced", error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        val data = ArrayList<Any>()

                        var isContainersEmpty = true
                        for (c in containers) {
                            c.attributes?.products?.let {
                                if (it.isNotEmpty()) {
                                    isContainersEmpty = false
                                    data.add(c)
                                }
                            }
                        }

                        if (isContainersEmpty && products.isEmpty()) {
                            preferenceManager.apply {
                                this.containers = containers
                                this.products = products
                            }
                            adaptyCallback.invoke(containers, products, "synced", error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        if (products.isNotEmpty())
                            data.add(products)

                        InAppPurchasesInfo(
                            context,
                            data,
                            object : AdaptyPurchaseContainersInfoCallback {
                                override fun onResult(data: ArrayList<Any>, error: String?) {
                                    if (error != null) {
                                        adaptyCallback.invoke(containers, products, "synced", error)
                                        if (needQueue)
                                            nextQueue()
                                        return
                                    }

                                    val cArray = ArrayList<DataContainer>()
                                    val pArray = ArrayList<Product>()

                                    for (d in data) {
                                        if (d is DataContainer)
                                            cArray.add(d)
                                        else if (d is ArrayList<*>)
                                            pArray.addAll(d as ArrayList<Product>)
                                    }

                                    val ar = arrayListOf<DataContainer>()

                                    for (c in containers) {
                                        var isContains = false
                                        for (d in cArray) {
                                            if (c.id == d.id)
                                                isContains = true
                                        }
                                        if (!isContains)
                                            ar.add(c)
                                    }

                                    ar.addAll(0, cArray)

                                    preferenceManager.apply {
                                        this.containers = ar
                                        this.products = pArray
                                    }

                                    adaptyCallback.invoke(ar, pArray, "synced", null)
                                    if (needQueue)
                                        nextQueue()
                                }
                            })
                    }

                }
            )
        }

        fun makePurchase(
            activity: Activity,
            product: Product,
            variationId: String? = null,
            adaptyCallback: (Purchase?, ValidateReceiptResponse?, String?) -> Unit
        ) {
            LogHelper.logVerbose("makePurchase()")
            val currentLooper = null
            addToQueue {
                InAppPurchases(
                    context,
                    activity,
                    false,
                    false,
                    preferenceManager,
                    product,
                    variationId,
                    null,
                    currentLooper,
                    object : AdaptyPurchaseCallback {
                        override fun onResult(
                            purchase: Purchase?,
                            response: ValidateReceiptResponse?,
                            error: String?
                        ) {
                            adaptyCallback.invoke(purchase, response, error)
                            nextQueue()
                        }
                    })
            }
        }

        fun syncPurchases(adaptyCallback: (error: String?) -> Unit) {
            val currentLooper = null
            addToQueue {
                syncPurchasesBody(context, currentLooper, adaptyCallback)
            }
        }

        private fun syncPurchasesBody(
            context: Context,
            currentLooper: Handler?,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            if (!::preferenceManager.isInitialized)
                preferenceManager = PreferenceManager(context)

            InAppPurchases(context,
                null,
                true,
                false,
                preferenceManager,
                Product(),
                null,
                ApiClientRepository.getInstance(preferenceManager),
                currentLooper,
                object : AdaptyRestoreCallback {
                    override fun onResult(response: RestoreReceiptResponse?, error: String?) {
                        if (adaptyCallback != null) {
                            if (error.isNullOrEmpty())
                                adaptyCallback.invoke(null)
                            else
                                adaptyCallback.invoke(error)
                            nextQueue()
                        }
                    }
                })
        }

        private fun periodicSyncPurchases(
            context: Context,
            currentLooper: Handler?
        ) {
            if (!::preferenceManager.isInitialized)
                preferenceManager = PreferenceManager(context)

            handlerPurchasesHistory.removeCallbacksAndMessages(null)
            runnablePurchasesHistory = Runnable {
                InAppPurchases(context,
                    null,
                    true,
                    true,
                    preferenceManager,
                    Product(),
                    null,
                    ApiClientRepository.getInstance(preferenceManager),
                    currentLooper,
                    object : AdaptyRestoreCallback {
                        override fun onResult(response: RestoreReceiptResponse?, error: String?) {
                            runnablePurchasesHistory?.let {
                                handlerPurchasesHistory.postDelayed(it, SYNC_PURCHASES_INTERVAL)
                            }
                        }
                    })
            }

            handlerPurchasesHistory.postDelayed(runnablePurchasesHistory, SYNC_PURCHASES_INTERVAL)
        }

        private fun restorePurchases(
            activity: Activity,
            currentLooper: Handler?,
            adaptyCallback: (RestoreReceiptResponse?, String?) -> Unit
        ) {

            LogHelper.logVerbose("restorePurchases()")

            addToQueue {
                if (!::preferenceManager.isInitialized)
                    preferenceManager = PreferenceManager(activity)

                InAppPurchases(
                    context,
                    null,
                    true,
                    false,
                    preferenceManager,
                    Product(),
                    null,
                    ApiClientRepository.getInstance(preferenceManager),
                    currentLooper,
                    object : AdaptyRestoreCallback {
                        override fun onResult(response: RestoreReceiptResponse?, error: String?) {
                            adaptyCallback.invoke(response, error)

                            nextQueue()

                            response?.data?.attributes?.apply {
                                checkChangesPurchaserInfo(this)
                            }
                        }
                    })
            }
        }

        fun restorePurchases(
            activity: Activity,
            adaptyCallback: (RestoreReceiptResponse?, String?) -> Unit
        ) {
            val currentLooper = null
            restorePurchases(activity, currentLooper, adaptyCallback)
        }

        fun validatePurchase(
            purchaseType: String,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: Product? = null,
            adaptyCallback: (ValidateReceiptResponse?, error: String?) -> Unit
        ) {
            val currentLooper = null
            validate(
                purchaseType,
                productId,
                purchaseToken,
                purchaseOrderId,
                product,
                currentLooper,
                adaptyCallback
            )
        }

        fun validatePurchase(
            purchaseType: String,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: Product? = null,
            currentLooper: Handler?,
            adaptyCallback: (ValidateReceiptResponse?, error: String?) -> Unit
        ) {
            validate(
                purchaseType,
                productId,
                purchaseToken,
                purchaseOrderId,
                product,
                currentLooper,
                adaptyCallback
            )
        }

        private fun validate(
            purchaseType: String,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: Product? = null,
            currentLooper: Handler?,
            adaptyCallback: (ValidateReceiptResponse?, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("validatePurchase()")
            if (purchaseOrderId == null && product == null) {
                addToQueue {
                    ApiClientRepository.getInstance(preferenceManager)
                        .validatePurchase(
                            purchaseType,
                            productId,
                            purchaseToken,
                            purchaseOrderId,
                            product,
                            currentLooper,
                            object : AdaptyValidateCallback {
                                override fun onResult(
                                    response: ValidateReceiptResponse?,
                                    error: String?
                                ) {
                                    adaptyCallback.invoke(response, error)

                                    response?.data?.attributes?.apply {
                                        checkChangesPurchaserInfo(this)
                                    }
                                    nextQueue()
                                }
                            })
                }
            } else {
                ApiClientRepository.getInstance(preferenceManager)
                    .validatePurchase(
                        purchaseType,
                        productId,
                        purchaseToken,
                        purchaseOrderId,
                        product,
                        currentLooper,
                        object : AdaptyValidateCallback {
                            override fun onResult(
                                response: ValidateReceiptResponse?,
                                error: String?
                            ) {
                                adaptyCallback.invoke(response, error)

                                response?.data?.attributes?.apply {
                                    checkChangesPurchaserInfo(this)
                                }
                            }
                        })
            }

        }

        fun updateAttribution(
            attribution: Any,
            source: String
        ) {
            val currentLooper = null
            updateAttribution(attribution, source, null, currentLooper)
        }

        fun updateAttribution(
            attribution: Any,
            source: String,
            networkUserId: String?,
            currentLooper: Handler?
        ) {
            LogHelper.logVerbose("updateAttribution()")
            addToQueue {
                ApiClientRepository.getInstance(preferenceManager).updateAttribution(
                    attribution,
                    source,
                    networkUserId,
                    currentLooper,
                    object : AdaptyProfileCallback {
                        override fun onResult(error: String?) {
                            nextQueue()
                        }

                    })
            }
        }

        fun logout(adaptyCallback: (String?) -> Unit) {
            LogHelper.logVerbose("logout()")
            val currentLooper = null
            addToQueue { logoutInQueue(currentLooper, adaptyCallback) }
        }

        private fun logoutInQueue(currentLooper: Handler?, adaptyCallback: (String?) -> Unit) {
            if (!::context.isInitialized) {
                adaptyCallback.invoke("Adapty was not initialized")
                nextQueue()
                return
            }

            if (!::preferenceManager.isInitialized) {
                preferenceManager = PreferenceManager(context)
            }

            preferenceManager.customerUserID = ""
            preferenceManager.installationMetaID = ""
            preferenceManager.profileID = ""
            preferenceManager.containers = null
            preferenceManager.products = arrayListOf()

            activateInQueue(context, preferenceManager.appKey, null, currentLooper, adaptyCallback)
        }

        fun setOnPurchaserInfoUpdatedListener(onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener?) {
            this.onPurchaserInfoUpdatedListener = onPurchaserInfoUpdatedListener
        }

        fun getProfileId(): String {
            return preferenceManager.profileID
        }

        fun getPurchaserInfo1(): PurchaserInfoModel? {
            return preferenceManager.purchaserInfo
        }
    }
}