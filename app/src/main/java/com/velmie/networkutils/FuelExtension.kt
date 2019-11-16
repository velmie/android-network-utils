package com.velmie.networkutils

import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.response
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber

inline fun <reified T : Any> Request.responseObject() = response(gsonDeserializer<T>()).third

inline fun <reified T : Any> Request.responseResult() =
    response().run {
        if (second.statusCode in 200 until 300 && second.contentLength <= 2) {
            Pair(null, null)
        } else if (second.statusCode >= 500) {
            Pair(null, third.component2() as? Exception)
        } else {
            var data: T? = null
            try {
                data = Gson().fromJson<T>(
                    second.body().asString(second.headers[Headers.CONTENT_TYPE].lastOrNull()),
                    object : TypeToken<T>() {}.type)
            } catch (e: RuntimeException) {
                Timber.e(e)
            }
            if (data != null) {
                Pair(data, null)
            } else {
                Pair(null, third.component2() as? Exception)
            }
        }
    }
