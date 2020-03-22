package com.example.testapplication.dictionary

import androidx.lifecycle.MutableLiveData
import com.example.testapplication.core.functional.map
import com.example.testapplication.dictionary.model.ResultView
import com.example.testapplication.dictionary.usecases.GetResult
import com.example.testapplication.core.platform.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DictionaryViewModel constructor(private val getResult: GetResult) : BaseViewModel() {

    var resultLiveData: MutableLiveData<List<ResultView>> = MutableLiveData()

    var ascending = true


    val loadingLiveData by lazy { MutableLiveData<Boolean>() }
    val exceptionLiveData by lazy { MutableLiveData<Exception>() }


    /**
     * Get data from api using use case.
     */
    fun getSuggestions(term: String) {
        loadingLiveData.postValue(true)
        launch(Dispatchers.IO) {

            getResult.run(GetResult.Param(term))
                .map { list ->
                    list.map { it.toResultView() }
                }.map {
                    if (ascending) {
                        it.sortedByDescending { resultView -> resultView.thumbsUp }
                    } else {
                        it.sortedByDescending { resultView -> resultView.thumbsDown }
                    }
                }
                .either({
                    loadingLiveData.postValue(false)
                    exceptionLiveData.postValue(it)
                }, {
                    resultLiveData.postValue(it)
                    loadingLiveData.postValue(false)
                })

        }

    }

    fun getSortedLastData(ascending: Boolean): MutableList<ResultView> {
        this.ascending = ascending
        return if (this.ascending) {
            resultLiveData.value.orEmpty().sortedBy { it.thumbsUp }.toMutableList()
        } else {
            resultLiveData.value.orEmpty().sortedByDescending { it.thumbsUp }.toMutableList()
        }
    }


}