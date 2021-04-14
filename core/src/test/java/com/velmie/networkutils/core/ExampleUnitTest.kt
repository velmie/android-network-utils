package com.velmie.networkutils.core

import com.velmie.parser.ApiParser
import com.velmie.parser.entity.apiResponse.interfaces.ApiResponseInterface
import com.velmie.parser.entity.parserResponse.ApiParserSuccessResponse
import kotlinx.coroutines.flow.Flow
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

        object : CoroutineNetworkBoundResource<String, String>(ApiParser(mapOf(), 0)) {
            override fun saveCallResult(item: String?): String? {
                TODO("Not yet implemented")
            }

            override fun shouldFetch(data: String?): Boolean {
                TODO("Not yet implemented")
            }

            override fun loadFromCache(): Flow<String> {
                TODO("Not yet implemented")
            }

            override fun createCall(loading: (progress: Float) -> Unit): Pair<ApiResponseInterface<String?>?, Exception?> {
                TODO("Not yet implemented")
            }
        }
    }
}
