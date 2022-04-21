package com.velmie.networkutils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.kittinunf.fuel.httpPost
import com.velmie.networkutils.core.AbsentLiveData
import com.velmie.networkutils.core.NetworkBoundResource
import com.velmie.networkutils.core.Resource
import com.velmie.parser.ApiParser
import com.velmie.parser.entity.apiResponse.ApiResponseEntity
import com.velmie.parser.entity.apiResponse.interfaces.ApiResponseInterface
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client1 = Client(1, "client1")
        val client2 = Client(1, "client2")

        val list1 = listOf(client1, client2)

       /* activationInfo(list1).observe(this, Observer {
            Timber.d(it.toString())
        })*/
    }

    /*fun activationInfo(list: List<Client>): LiveData<Resource<List<Client>>> {
        return object :
            NetworkBoundResource<List<Client>, List<Client>>(ApiParser(mapOf(), 1)) {

            override fun saveCallResult(item: List<Client>?) {
            }

            override fun shouldFetch(data: List<Client>?): Boolean {
                return true
            }

            override fun loadFromCache(): LiveData<List<Client>> {
                return AbsentLiveData.create()
            }

            override fun createCall(): Pair<ApiResponseInterface<List<Client>?>?, Exception?> {
                val hash1 = list.hashCode()
                list[0].name = "fds"
                val hash2 = list.hashCode()
                    forcePushData(list)
                return "https://mechano-machine-app.api.dev.dom.neonetic.com/machine/activate/".httpPost()
                    .body("{\n" +
                            "\"hashId\": \"x1h\",\n" +
                            "\"secureToken\": \"x1s\"\n" +
                            "}")
                    .responseResult<ApiResponseEntity<List<Client>?>>()
            }

            override fun onFetchFailed() {
            }
        }.asLiveData()
    }*/
}
