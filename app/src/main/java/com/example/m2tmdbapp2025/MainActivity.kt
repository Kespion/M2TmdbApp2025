package com.example.m2tmdbapp2025

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.GONE
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.m2tmdbapp2025.databinding.ActivityMainBinding
import com.example.m2tmdbapp2025.model.Person
import com.example.m2tmdbapp2025.model.PersonPopularResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val NOTIFICATION_CHANNEL_ID = "popular_person_notification_channel_id"
const val TMDB_WORK_REQUEST_TAG = "tmdb-popular-person"
const val PERSON_ID_EXTRA_KEY = "person_id"

class MainActivity : AppCompatActivity(), View.OnClickListener, OnPersonItemClickListener {

    private val LOGTAG = MainActivity::class.simpleName
    private lateinit var binding: ActivityMainBinding
    private lateinit var personPopularAdapter: PersonPopularAdapter
    private var isNotifPermGranted = false // TODO: replace with SHARED PREFERENCE
    private var persons = arrayListOf<Person>()
    private var curPage = 1
    private var totalResults = 0
    private var totalPage = Int.MAX_VALUE
    private val socialBarViewModel: SocialBarViewModel by viewModels {
        SocialBarViewModelFactory((application as TmdbApplication).socialBarDao)
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
    // or a lateinit var in your onAttach() or onCreate() method.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            isNotifPermGranted = isGranted
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                Log.i(LOGTAG,"Permission was granted")
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Log.i(LOGTAG, "Permission was denied" )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // person popular notification channel creation
        createNotificationChannel()
        initWorkManager()

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
        /*Demo fragment dynamic attachment
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.setReorderingAllowed(true)
        ft.add(R.id.social_bar_fcv, SocialBarFragment.newInstance())
        ft.commit() */
        // Init bt listener
        binding.sensorDemoBt.setOnClickListener(this)

        addLivedataObservers()
        check4NotificationPermission()
        showHighScore()

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
                    personPopularAdapter.setMaxPopularity()
                    // TODO : uncomment for demo purpose only
                    /*if (isNotifPermGranted && curPage == 1) {
                        TmdbNotifications.createPopularPersonNotification(applicationContext, persons[0])
                    }*/
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun check4NotificationPermission():Any = when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED -> {
            Log.i(LOGTAG,"notification permission already granted")
            isNotifPermGranted = true
        }
        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected, and what
            // features are disabled if it's declined. In this UI, include a
            // "cancel" or "no thanks" button that lets the user continue
            // using your app without granting the permission.
            Log.i(LOGTAG, "show a permission rationale explanation dialog")
        }
        else -> {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun initWorkManager() {
        // compute delay between now and wished work request start
        val currentTime = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 2)
            if (before(currentTime)) {
                add(Calendar.DATE, 1)
            }
        }
        val initialDelay = scheduledTime.timeInMillis - currentTime.timeInMillis

        // only need to be connected to any network
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Build work request
        val tmdbWorkRequest = PeriodicWorkRequestBuilder<TmdbDailyWorker>(1, TimeUnit.DAYS)
            .addTag(TMDB_WORK_REQUEST_TAG)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay,TimeUnit.MILLISECONDS)
            .setBackoffCriteria( // wait one hour before retrying
            BackoffPolicy.LINEAR,
            1,
            TimeUnit.HOURS)
            .build()

        // enqueue request
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TMDB_WORK_REQUEST_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            tmdbWorkRequest)

    }

    fun showHighScore() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val highscore = sharedPref.getFloat(getString(R.string.saved_high_score_key), 0f)
        Log.i(LOGTAG, "person popular high score = $highscore")
    }

    private fun addLivedataObservers() {
        // Add observers on the LiveData returned by getAllFavorites and getAllLikes
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        socialBarViewModel.nbLikes.observe(this) { map ->
            Log.d(LOGTAG, "nblike map size = ${map.size}")
        }
        socialBarViewModel.isFavorite.observe(this) { map ->
            Log.d(LOGTAG, "is favorite map size = ${map.size}")
        }
    }

    override fun onClick(v: View?) {
        Log.i(LOGTAG,"Button $v.id clicked")
       /* val intent = Intent()
        intent.setClass(this,PersonDetailActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(PERSON_ID_EXTRA_KEY, person.id.toString())
        */
        val intent = Intent().apply {
            setClass(this@MainActivity, SensorDemoActivity::class.java)
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onPersonItemClicked(position: Int) {
        val person = persons[position]
        Log.d(LOGTAG, "${person.name} clicked !")
        val intent = Intent().apply {
            setClass(this@MainActivity, PersonDetailActivity::class.java)
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(PERSON_ID_EXTRA_KEY, person.id.toString())
        }
        startActivity(intent)
    }

    /* deprecated way to handle button click from xml layout
    fun showPersoDetail(view: View) {
        Log.d(LOGTAG,"Bt clicked")
        val intent = Intent()
        intent.setClass(this,PersonDetailActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        //intent.putExtra(PERSON_ID_EXTRA_KEY, person.id.toString())
        startActivity(intent)
    } */
}