package com.example.testapplication.dictionary

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.afterTextChangeEvents
import com.example.testapplication.R
import com.example.testapplication.core.exception.AppException
import com.example.testapplication.core.extensions.context.gone
import com.example.testapplication.core.extensions.context.observe
import com.example.testapplication.core.extensions.context.visible
import com.example.testapplication.core.platform.BaseActivity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_dictionary.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class DictionaryActivity : BaseActivity<DictionaryViewModel>() {


    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val dictionaryViewModel: DictionaryViewModel by viewModel()
    private val suggestionsAdapter = ResultAdapter(mutableListOf())


    override fun getViewModel(): DictionaryViewModel {
        return dictionaryViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary)
        attachObservers()
        setViews()

    }

    private fun attachObservers() {
        rvSuggestions.layoutManager = LinearLayoutManager(this@DictionaryActivity)
        rvSuggestions.adapter = suggestionsAdapter

        observe(dictionaryViewModel.resultLiveData) {
            it?.apply {
                if (this.isEmpty()) {
                    groupError.visible()
                    tvError.text = "No data found try other word!"

                } else {
                    rvSuggestions.visible()
                    groupError.gone()
                }
                suggestionsAdapter.updateList(this.toMutableList())
            } ?: kotlin.run {
                suggestionsAdapter.updateList(mutableListOf())
            }
        }

        observe(dictionaryViewModel.loadingLiveData) {
            it?.apply {
                if (this) {
                    progress.visible()
                } else {
                    progress.gone()
                }
            }
        }


        handleExceptions()
    }

    private fun handleExceptions() {

        observe(dictionaryViewModel.exceptionLiveData) {
            dictionaryViewModel.loadingLiveData.value = false
            it?.apply {
                if (it is AppException) {
                    when (it) {
                        is AppException.NetworkError -> {
                            rvSuggestions.gone()
                            groupError.visible()
                            tvError.text = getString(R.string.connection_error)
                        }
                        is AppException.HttpError -> {//will not occur
                            Log.d("Http Error", it.throwable.localizedMessage.orEmpty())
                        }
                    }

                } else {
                    Log.e("Error", it.localizedMessage, it)
                }
                dictionaryViewModel.exceptionLiveData.value = null
            }
        }
    }

    private fun setViews() {
        etSearch.afterTextChangeEvents()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribe({
                dictionaryViewModel.getSuggestions(etSearch.text.toString())
            }, {
                it.printStackTrace()
            }).apply {
                compositeDisposable.add(this)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sort, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionSortUp -> {
                sortByThumbsUp()
                true
            }
            R.id.actionSortDown -> {
                sortByThumbsDown()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sortByThumbsUp() {
        suggestionsAdapter.updateList(dictionaryViewModel.getSortedLastData(false))
        scrollToZero()
    }

    private fun scrollToZero() {
        rvSuggestions.post {
            rvSuggestions.smoothScrollToPosition(0)
        }
    }

    private fun sortByThumbsDown() {
        suggestionsAdapter.updateList(dictionaryViewModel.getSortedLastData(true))
        scrollToZero()
    }

    override fun onPause() {
        dictionaryViewModel.exceptionLiveData.value = null
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

}
