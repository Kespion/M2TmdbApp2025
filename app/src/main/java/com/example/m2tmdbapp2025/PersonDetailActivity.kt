package com.example.m2tmdbapp2025

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.m2tmdbapp2025.databinding.ActivityPersonDetailBinding
import com.example.m2tmdbapp2025.model.PersonDetail
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PersonDetailActivity : AppCompatActivity() {

    companion object {
        const val PERSON_ID_EXTRA_KEY = "person_id"
        const val SBFC_PERSON_ID_ARG = "sbfc_person_id"
    }

    private lateinit var binding: ActivityPersonDetailBinding
    private var personId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        personId = intent.getStringExtra(PERSON_ID_EXTRA_KEY)?.toIntOrNull()
            ?: return finish()

        fetchPersonDetail(personId)
    }

    private fun fetchPersonDetail(personId: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            runCatching {
                ApiClient.tmdbService.getPersonDetail(personId)
            }.onSuccess { detail ->
                bindDetail(detail)
                attachSocialBarFragment()
            }.onFailure { err ->
                binding.nameTv.text = "Erreur de chargement"
                Log.e("PersonDetail", "API failure", err)
            }
        }
    }

    private fun bindDetail(detail: PersonDetail) {
        binding.nameTv.text       = detail.name
        binding.birthDateTv.text  = detail.birthDate  ?: "Date inconnue"
        binding.birthPlaceTv.text = detail.birthPlace ?: "Lieu inconnu"
        binding.biographyTv.text  = detail.biography  ?: "Pas de biographie disponible"

        detail.profilePath?.let {
            Picasso.get()
                .load(ApiClient.IMAGE_BASE_URL + it)
                .into(binding.photoIv)
        }
    }

    private fun attachSocialBarFragment() {
        val args = Bundle().apply {
            putString(SBFC_PERSON_ID_ARG, personId.toString())
        }
        supportFragmentManager.commit {
            if (supportFragmentManager.findFragmentById(R.id.social_bar_fcv) == null) {
                add(R.id.social_bar_fcv, SocialBarFragment::class.java, args)
            }
        }
    }
}