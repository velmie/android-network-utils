package com.velmie.networkutils.core

import com.velmie.parser.entity.parserResponse.ParserMessageEntity

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
</T> */
sealed class Resource<out T> constructor(open val status: Status, open val data: T?)

class Loading<T>(override val data: T?, val progress: Float = 0f) :
    Resource<T>(Status.LOADING, data)

class Success<T>(override val data: T?) : Resource<T>(Status.SUCCESS, data)

class Error<T>(
    override val data: T?,
    val exception: Exception? = null,
    val errorMessage: String? = null,
    val errors: List<ParserMessageEntity<Int>>? = null
) : Resource<T>(Status.ERROR, data)
