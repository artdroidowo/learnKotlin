package com.example.karol.learnkotlin

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ListAdapter
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val wikiApiServe by lazy {
        WikiApiService.create()
    }
    private var disposable: Disposable? = null
    var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et_search_querry.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchData(et_search_querry.text.toString())
                true
            } else {
                false
            }
        }
        btn_search.setOnClickListener {
            searchData(et_search_querry.text.toString())
        }
    }

    private fun searchData(srsearch: String) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(et_search_querry.windowToken, 0)

        rv_results.layoutManager = LinearLayoutManager(this)
        val manager = rv_results.layoutManager as LinearLayoutManager
        pb_fetch.visibility = View.VISIBLE
        disposable =
                wikiApiServe.getRecordsByQuerry("query", "json", "search", srsearch)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    var continueS = result._continue._continue
                                    var offset = result._continue.sroffset
                                    var searchS = srsearch

                                    pb_fetch.visibility = View.GONE
                                    rv_results.adapter = ResultsAdapter(result.query.search, this)
                                    tv_search_results.text =
                                            String.format(resources.getQuantityText(R.plurals.results_found,
                                                    result.query.searchinfo.totalhits).toString(), result.query.searchinfo.totalhits)
                                    rv_results.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                                            super.onScrolled(recyclerView, dx, dy)
                                            if (!isLoading) {
                                                if (manager.findLastCompletelyVisibleItemPosition()
                                                        >= rv_results.adapter.itemCount - 1) {
                                                    isLoading = true
                                                    pb_fetch.visibility = View.VISIBLE
//                                                    disposable?.dispose()
                                                    disposable = wikiApiServe.getMoreRecords("query", "json", "search", continueS, offset, searchS)
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(AndroidSchedulers.mainThread())
                                                            .subscribe({ result ->
                                                                isLoading = false
                                                                        continueS = result._continue._continue
                                                                        offset = result._continue.sroffset
                                                                        pb_fetch.visibility = View.GONE
                                                                        (rv_results.adapter as ResultsAdapter).addMore(result.query.search)
                                                                    }, { _ ->
                                                                isLoading = false
                                                                pb_fetch.visibility = View.GONE
                                                                Toast.makeText(this@MainActivity,
                                                                        "Something went wrong try again",
                                                                        Toast.LENGTH_SHORT).show()
                                                            })
                                                }
                                            }
                                        }
                                    })
                                },
                                { _ ->
                                    pb_fetch.visibility = View.GONE
                                    Toast.makeText(this,
                                            "Something went wrong try again",
                                            Toast.LENGTH_SHORT).show()
                                }
                        )
    }

    override fun onPause() {
//        disposable?.dispose()
        super.onPause()
    }
}