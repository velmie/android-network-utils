package com.velmie.networkutils.core

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.velmie.parser.ApiParser
import com.velmie.parser.entity.apiResponse.interfaces.ApiResponseInterface
import com.velmie.parser.entity.parserResponse.ApiParserEmptyResponse
import com.velmie.parser.entity.parserResponse.ApiParserErrorResponse
import com.velmie.parser.entity.parserResponse.ApiParserSuccessResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.HttpRetryException

// ResultType: Type for the Resource data.
// RequestType: Type for the API response.
abstract class NetworkBoundResourceFlow<ResultType, RequestType>(private val apiParser: ApiParser<Int>) {

    private val resourceDataData = MutableStateFlow<Resource<ResultType>>(Loading(null))

    init {
        @Suppress("LeakingThis")
        CoroutineScope(Dispatchers.IO).launch {
            if (loadFromCache().count() == 0) {
                fetchFromNetwork()
            } else {
                loadFromCache().collect { data ->
                    if (shouldFetch(data)) {
                        fetchFromNetwork()
                    } else {
                        pushValue(Success(data))
                    }
                    cancel()
                }
            }
        }
    }

    private fun fetchFromNetwork() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (apiResponse, error) = createCall()
                if (error == null) {
                    when (val parserResponse = apiParser.parse(apiResponse)) {
                        is ApiParserSuccessResponse -> {
                            pushValue(Success(saveCallResult(processResponse(parserResponse))))
                        }
                        is ApiParserEmptyResponse -> {
                            pushValue(Success(saveCallResult(null)))
                        }
                        is ApiParserErrorResponse -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                onFetchFailed(HttpProcessingException(parserResponse.errors.toString()))
                                pushValue(
                                    Error(
                                        null,
                                        errors = parserResponse.errors
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Timber.e(error)
                    CoroutineScope(Dispatchers.Main).launch {
                        onFetchFailed(error)
                        pushValue(Error(null, exception = error))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                CoroutineScope(Dispatchers.Main).launch {
                    onFetchFailed(e)
                    pushValue(Error(null, exception = e))
                }
            }
        }
    }

    private suspend fun pushValue(resource: Resource<ResultType>) {
        resourceDataData.emit(resource)
    }

    // Called to save the result of the API response into the database
    @WorkerThread
    protected abstract fun saveCallResult(item: RequestType?): ResultType?

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the com.neonetic.core.network.
    @MainThread
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    // Called to getMachine the cached data from the database.
    @MainThread
    protected abstract fun loadFromCache(): Flow<ResultType?>

    // Called to create the API call.
    @WorkerThread
    protected abstract suspend fun createCall(): Pair<ApiResponseInterface<RequestType?>?, Exception?>

    // Called when the fetch fails. The child class may want to reset components
    // like rate limiter.
    protected open fun onFetchFailed(exception: Exception) {}

    // Returns a LiveData object that represents the resource that's implemented
    // in the base class.
    fun asFlow(): Flow<Resource<ResultType>> = resourceDataData

    @WorkerThread
    protected open fun processResponse(response: ApiParserSuccessResponse<RequestType?, Int>) =
        response.body
}
