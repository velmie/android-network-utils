package com.velmie.networkutils.core

import com.velmie.parser.entity.parserResponse.ParserMessageEntity

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
</T> */
sealed class Resource<out T> constructor(open val data: T?)

class Loading<T>(override val data: T?, val progress: Float = 0f) :
    Resource<T>(data)

data class Success<T>(override val data: T?) : Resource<T>(data)

class Error<T>(
    override val data: T?,
    val exception: Exception? = null,
    val errorMessage: String? = null,
    val errors: List<ParserMessageEntity<Int>>? = null
) : Resource<T>(data)
