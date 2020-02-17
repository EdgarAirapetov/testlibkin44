package com.adapty.api

interface ICallback {

    fun success(response: Any?, reqID: Int)

    fun fail(msg: String, reqID: Int)
}