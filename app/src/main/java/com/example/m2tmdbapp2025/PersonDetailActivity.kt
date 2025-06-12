package com.example.m2tmdbapp2025

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.m2tmdbapp2025.databinding.ActivityPersonDetailBinding
import com.example.m2tmdbapp2025.model.PersonDetail
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PersonDetailActivity : AppCompatActivity() {

    companion object {
        const val PERSON_ID_EXTRA_KEY = "person_id"
    }

    private lateinit var binding: ActivityPersonDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupère l’ID depuis l’intent
        val id = intent.getStringExtra(PERSON_ID_EXTRA_KEY)?.toIntOrNull()
        if (id == null) {
            finish() // pas d'id → on ferme
            return
        }

        // Lance l’appel réseau
        fetchPersonDetail(id)
    }

    private fun fetchPersonDetail(personId: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            runCatching {
                ApiClient.tmdbService.getPersonDetail(personId)
            }.onSuccess { detail ->
                bindDetail(detail)
            }.onFailure { err ->
                binding.nameTv.text = "Erreur de chargement"
                Log.e("PersonDetail", "API failure", err)
            }
        }
    }

    private fun bindDetail(detail: PersonDetail) {
        // Nom
        binding.nameTv.text = detail.name

        // Photo (si dispo)
        detail.profilePath?.let {
            Picasso.get()
                .load(ApiClient.IMAGE_BASE_URL + it)
                .into(binding.photoIv)
        }

        // Date et lieu
        binding.birthDateTv.text  = detail.birthDate ?: "Date inconnue"
        binding.birthPlaceTv.text = detail.birthPlace ?: "Lieu inconnu"

        // Biographie
        binding.biographyTv.text  = detail.biography ?: "Pas de biographie disponible"

        // Affiche le contenu
        binding.biographySv.visibility = View.VISIBLE
    }
}