package com.adapty.utils

import android.content.Context

const val PROFILE_ID = "PROFILE_ID"
const val CUSTOMER_USER_ID = "CUSTOMER_USER_ID"
const val INSTALLATION_META_ID = "CUSTOMER_USER_ID"

class PreferenceManager (context: Context) {

    private val PRIVATE_MODE = 0
    private val pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
    private val editor = pref.edit()

    var profileID: String
        get() {
            return pref.getString(PROFILE_ID, "").toString()
        }
        set(profileID) {
            editor.putString(PROFILE_ID, profileID)
            editor.commit()
        }

    var customerUserID: String
        get() {
            return pref.getString(CUSTOMER_USER_ID, "").toString()
        }
        set(customerUserID) {
            editor.putString(CUSTOMER_USER_ID, customerUserID)
            editor.commit()
        }

    var installationMetaID: String
        get() {
            return pref.getString(INSTALLATION_META_ID, "").toString()
        }
        set(installationMetaID) {
            editor.putString(INSTALLATION_META_ID, installationMetaID)
            editor.commit()
        }

    companion object {
        const val PREF_NAME = "AdaptyPrefs"
    }

}