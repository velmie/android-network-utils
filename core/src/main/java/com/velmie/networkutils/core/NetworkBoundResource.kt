package com.velmie.networkutils.core

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.velmie.parser.ApiParser
import com.velmie.parser.entity.apiResponse.interfaces.ApiResponseInterface
import com.velmie.parser.entity.parserResponse.ApiParserEmptyResponse
import com.velmie.parser.entity.parserResponse.ApiParserErrorResponse
import com.velmie.parser.entity.parserResponse.ApiParserSuccessResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

// ResultType: Type for the Resource data.
// RequestType: Type for the API response.
abstract class NetworkBoundResource<ResultType, RequestType>(private val apiParser: ApiParser<Int>) {

    private val resourceLiveData = ExclusiveHashMediator<Resource<ResultType>>()

    init {
        resourceLiveData.setValue(Loading(null))
        @Suppress("LeakingThis")
        loadFromCache().apply {
            resourceLiveData.addSource(this) { data ->
                resourceLiveData.removeSource(this)
                if (shouldFetch(data)) {
                    fetchFromNetwork()
                } else {
                    resourceLiveData.addSource(this) { newData ->
                        resourceLiveData.setValue(Success(newData))
                    }
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
                            addCacheSource(saveCallResult(processResponse(parserResponse)))
                        }
                        is ApiParserEmptyResponse -> {
                            addCacheSource(saveCallResult(null))
                        }
                        is ApiParserErrorResponse -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                onFetchFailed()
                                resourceLiveData.setValue(
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
                        onFetchFailed()
                        resourceLiveData.setValue(Error(null, exception = error))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                CoroutineScope(Dispatchers.Main).launch {
                    onFetchFailed()
                    resourceLiveData.setValue(Error(null, exception = e))
                }
            }
        }
    }

    @MainThread
    private fun addCacheSource(result: ResultType?) {
        CoroutineScope(Dispatchers.Main).launch {
            resourceLiveData.addSource(loadFromCache()) {
                resourceLiveData.setValue(Success(it ?: result))
            }
        }
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
    protected abstract fun loadFromCache(): LiveData<ResultType>

    // Called to create the API call.
    @MainThread
    protected abstract fun createCall(): Pair<ApiResponseInterface<RequestType?>?, Exception?>

    // Called when the fetch fails. The child class may want to reset components
    // like rate limiter.
    protected open fun onFetchFailed() {}

    // Returns a LiveData object that represents the resource that's implemented
    // in the base class.
    fun asLiveData(): LiveData<Resource<ResultType>> = resourceLiveData

    @WorkerThread
    protected open fun processResponse(response: ApiParserSuccessResponse<RequestType?, Int>) =
        response.body
}
