package com.example.m2tmdbapp2025

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.GONE
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import com.example.m2tmdbapp2025.databinding.ActivityMainBinding
import com.example.m2tmdbapp2025.model.Person
import com.example.m2tmdbapp2025.model.PersonPopularResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val LOGTAG = MainActivity::class.simpleName
    private lateinit var binding: ActivityMainBinding
    private lateinit var personPopularAdapter: PersonPopularAdapter
    private var persons = arrayListOf<Person>()
    private var curPage = 1
    private var totalResults = 0
    private var totalPage = Int.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init recycler view
        binding.popularPersonRv.setHasFixedSize(true)
        binding.popularPersonRv.layoutManager = LinearLayoutManager(this)
        personPopularAdapter = PersonPopularAdapter(persons, this)
        binding.popularPersonRv.adapter = personPopularAdapter
        binding.popularPersonRv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE && !recyclerView.canScrollVertically(1)) {
                    if (curPage < totalPage) {
                        curPage++
                        loadPage(curPage)
                    }
                }
            }
        })

        // load 1st page
        loadPage(curPage)

}

    private fun loadPage(page: Int ) {
        val tmdbapi = ApiClient.instance.create(ITmdbApi::class.java)
        val call: Call<PersonPopularResponse> = tmdbapi.getPopularPerson(TMDB_API_KEY, page)
        binding.progressWheel.visibility = VISIBLE

        call.enqueue(object : Callback<PersonPopularResponse> {
            override fun onResponse(
                call: Call<PersonPopularResponse>,
                response: Response<PersonPopularResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Page $page loaded", Toast.LENGTH_SHORT).show()
                    persons.addAll(response.body()!!.results)
                    totalResults = response.body()?.totalResults!!
                    Log.d(LOGTAG, "got ${persons.size}/${totalResults} elements")
                    personPopularAdapter.notifyDataSetChanged()
                    binding.totalResultsTv.text = getString(R.string.total_results_text, persons.size, totalResults)

                } else {
                    Log.e(
                        LOGTAG,
                        "Call to getPopularPerson failed with error code $response.code()"
                    )
                }
                binding.progressWheel.visibility= GONE
            }

            override fun onFailure(call: Call<PersonPopularResponse>, t: Throwable) {
                Log.e(LOGTAG, "Call to TMDB API failed")
                binding.progressWheel.visibility= GONE
            }

        })
    }

}