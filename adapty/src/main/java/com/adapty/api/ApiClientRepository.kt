package com.adapty.api

import com.adapty.Adapty.Companion.applicationContext
import com.adapty.api.entity.profile.DataProfileReq
import com.adapty.api.entity.syncmeta.DataSyncMetaReq
import com.adapty.api.requests.CreateProfileRequest
import com.adapty.api.requests.SyncMetaInstallRequest
import com.adapty.api.requests.UpdateProfileRequest
import com.adapty.api.responses.SyncMetaInstallResponse
import com.adapty.utils.PreferenceManager
import com.adapty.utils.UUIDTimeBased

class ApiClientRepository(var preferenceManager: PreferenceManager) {

    private var apiClient = ApiClient(applicationContext)

    fun createProfile(iCallback: ICallback) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val profileRequest = CreateProfileRequest()
        profileRequest.data = DataProfileReq()
        profileRequest.data?.id = uuid
        profileRequest.data?.type = "adapty_analytics_profile"

        apiClient.createProfile(profileRequest, iCallback)
    }

    fun updateProfile(iCallback: ICallback) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
        }

        val profileRequest = UpdateProfileRequest()
        profileRequest.data = DataProfileReq()
        profileRequest.data?.id = uuid
        profileRequest.data?.type = "adapty_analytics_profile"

        apiClient.updateProfile(profileRequest, iCallback)
    }

    fun syncMetaInstall(iCallback: ICallback? = null) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val syncMetaRequest = SyncMetaInstallRequest()
        syncMetaRequest.data = DataSyncMetaReq()
        syncMetaRequest.data?.id = uuid
        syncMetaRequest.data?.type = "adapty_analytics_profile_installation_meta"

        apiClient.syncMeta(syncMetaRequest, iCallback)
    }

    companion object Factory {

        private lateinit var instance: ApiClientRepository

        @Synchronized
        fun getInstance(preferenceManager: PreferenceManager): ApiClientRepository {
            if (!::instance.isInitialized)
                instance = ApiClientRepository(preferenceManager)

            return instance
        }
    }
}