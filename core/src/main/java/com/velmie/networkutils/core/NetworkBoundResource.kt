package com.velmie.networkutils.core

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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

    private val result = MediatorLiveData<Resource<ResultType>>()

    init {
        result.value = Resource.loading(null)
        @Suppress("LeakingThis")
        loadFromCache().apply {
            result.addSource(this) { data ->
                if (shouldFetch(data)) {
                    fetchFromNetwork()
                } else {
                    setValue(Resource.success(data))
                }
            }
            if (value == null) {
                fetchFromNetwork()
            }
        }
    }

    @MainThread
    private fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    private fun fetchFromNetwork() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (apiResponse, error) = createCall()
                if (error == null) {
                    when (val parserResponse = apiParser.parse(apiResponse)) {
                        is ApiParserSuccessResponse -> {
                            saveCallResult(processResponse(parserResponse))
                        }
                        is ApiParserEmptyResponse -> {
                            saveCallResult(null)
                        }
                        is ApiParserErrorResponse -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                onFetchFailed()
                                setValue(
                                    Resource.error(
                                        parserResponse.errors,
                                        null
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Timber.e(error)
                    CoroutineScope(Dispatchers.Main).launch {
                        onFetchFailed()
                        setValue(Resource.error(error, null))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                CoroutineScope(Dispatchers.Main).launch {
                    onFetchFailed()
                    setValue(Resource.error(e, null))
                }
            }
        }
    }

   // @WorkerThread
    protected fun forcePushData(data: ResultType?) {
        CoroutineScope(Dispatchers.Main).launch {
            setValue(Resource.success(data = data))
        }
    }

    // Called to save the result of the API response into the database
    @WorkerThread
    protected abstract fun saveCallResult(item: RequestType?)

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
    fun asLiveData(): LiveData<Resource<ResultType>> = result

    @WorkerThread
    protected open fun processResponse(response: ApiParserSuccessResponse<RequestType?, Int>) =
        response.body
}
