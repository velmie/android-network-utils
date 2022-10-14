package com.velmie.networkutils.core

import com.velmie.parser.ApiParser
import com.velmie.parser.entity.apiResponse.interfaces.ApiResponseInterface
import com.velmie.parser.entity.parserResponse.ApiParserEmptyResponse
import com.velmie.parser.entity.parserResponse.ApiParserErrorResponse
import com.velmie.parser.entity.parserResponse.ApiParserSuccessResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.IOException

// ResultType: Type for the Resource data.
// RequestType: Type for the API response.

abstract class CoroutineNetworkBoundResource<ResultType, RequestType>(private val apiParser: ApiParser<Int>) {

    private val resourceFlow = MutableStateFlow<Resource<ResultType>>(Loading(null))

    private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName(::javaClass.name))

    val flow = resourceFlow.asStateFlow()

    init {
        coroutineScope.launch {
            val data = withContext(Dispatchers.Default) {
                loadFromCache()
            }
            if (shouldFetch(data)) {
                fetchFromNetwork()
            } else {
                resourceFlow.value = Success(data)
            }
        }
    }

    private suspend fun fetchFromNetwork() {
        try {
            val (apiResponse, error) = createCall {
                resourceFlow.value = Loading(null, it)
            }
            if (error == null) {
                when (val parserResponse = apiParser.parse(apiResponse)) {
                    is ApiParserSuccessResponse -> {
                        resourceFlow.value =
                            Success(saveCallResult(processResponse(parserResponse)))
                    }
                    is ApiParserEmptyResponse -> {
                        resourceFlow.value = Success(saveCallResult(null))
                    }
                    is ApiParserErrorResponse -> {
                        onFetchFailed(HttpProcessingException(parserResponse.errors.toString()))
                        resourceFlow.value = Error(null, errors = parserResponse.errors)
                    }
                }
            } else {
                Timber.e(error)
                onFetchFailed(error)
                resourceFlow.value = Error(null, exception = error)
            }
        } catch (e: Exception) {
            Timber.e(e)
            onFetchFailed(e)
            resourceFlow.value = Error(null, exception = e)
        }
    }

    // Called to save the result of the API response into the database
    protected abstract suspend fun saveCallResult(item: RequestType?): ResultType?

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the cache
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    // Called to getMachine the cached data from the database.
    protected abstract suspend fun loadFromCache(): ResultType

    // Called to create the API call.
    protected abstract suspend fun createCall(loading: (progress: Float) -> Unit): Pair<ApiResponseInterface<RequestType?>?, Exception?>

    // Called when the fetch fails. The child class may want to reset components
    // like rate limiter.
    protected open fun onFetchFailed(exception: Exception) {}

    protected open fun processResponse(response: ApiParserSuccessResponse<RequestType?, Int>) =
        response.body
}
