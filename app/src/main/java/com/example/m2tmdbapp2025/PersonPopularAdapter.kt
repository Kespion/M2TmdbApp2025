package com.example.m2tmdbapp2025

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbapp2025.databinding.PersonItemBinding
import com.example.m2tmdbapp2025.model.Person
import com.squareup.picasso.Picasso

class PersonPopularAdapter(private val persons: ArrayList<Person>, private val appCompatActivity: AppCompatActivity) : RecyclerView.Adapter<PersonPopularAdapter.PersonPopularViewHolder>() {
    private val LOGTAG= PersonPopularViewHolder::class.simpleName
    private var maxPopularity : Double = 0.0
    private val scoreRatings: Array<String> = appCompatActivity.resources.getStringArray(R.array.score_rating)
    private val ratingColors: Array<String> = appCompatActivity.resources.getStringArray(R.array.rating_colors)

    init {
        setMaxPopularity()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonPopularViewHolder {
        val binding = PersonItemBinding.inflate((LayoutInflater.from(parent.context)), parent, false)
        return PersonPopularViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return persons.size
    }

    override fun onBindViewHolder(holder: PersonPopularViewHolder, position: Int) {
        val curItem = persons[position]

        val genderRes = when (curItem.gender) {
            1    -> R.drawable.ic_gender_female
            2    -> R.drawable.ic_gender_male
            3    -> R.drawable.ic_gender_trans
            else -> null
        }

        if (genderRes != null) {
            holder.binding.ivGender.apply {
                setImageResource(genderRes)
                visibility = View.VISIBLE
                contentDescription = if (curItem.gender == 1) "Femme" else if (curItem.gender == 2) "Homme" else "Transgenre"
            }
        } else {
            holder.binding.ivGender.visibility = View.INVISIBLE
        }

        holder.binding.nameTv.text = curItem.name
        holder.binding.knownForTv.text = curItem.knownForDepartment
        holder.binding.popularityTv.text = curItem.popularity.toString()
        Picasso.get()
            .load(ApiClient.IMAGE_BASE_URL + curItem.profilePath)
            .placeholder(android.R.drawable.progress_horizontal)
            .error(android.R.drawable.stat_notify_error)
            .into(holder.binding.photoIv)

        // set score gauge view
        holder.binding.scoreGaugeView.updateScore(
            getRating(curItem.popularity, maxPopularity),
            getScoreColor(curItem.popularity,maxPopularity),
            curItem.popularity!!.toFloat(),
            maxPopularity.toFloat()
        )

        // set social bar fragment container view tag with unique person id
        holder.binding.socialBarFcv.tag = curItem.id.toString()

        /* demo only : no the best place to set the listener
        holder.binding.personItemCl.setOnClickListener {
            val intent = Intent()
            intent.setClass(appCompatActivity,PersonDetailActivity::class.java)
            intent.putExtra(PERSON_ID_EXTRA_KEY, curItem.id.toString())
            appCompatActivity.startActivity(intent)
        } */
    }

    override fun onViewAttachedToWindow(holder: PersonPopularViewHolder) {
        super.onViewAttachedToWindow(holder)
        //Log.d(LOGTAG,"onViewAttachedToWindow("+holder.binding.nameTv.text+")")
        //Log.d(LOGTAG,"sbfcid="+holder.binding.socialBarFcv.id)

        val sbfcv = holder.binding.socialBarFcv
        val bundle = bundleOf("sbfc_view_tag" to sbfcv.tag)
        appCompatActivity.supportFragmentManager.commitNow {
            // use the default Fragment Factory to instanciate a new fragment
            // (best choice to benefit from the life cycle management)
            add(sbfcv.id,SocialBarFragment::class.java, bundle)
        }
    }

    override fun onViewDetachedFromWindow(holder: PersonPopularViewHolder) {
        super.onViewDetachedFromWindow(holder)
        //Log.d(LOGTAG,"onViewDetachedToWindow("+holder.binding.nameTv.text+")")

        appCompatActivity.supportFragmentManager.findFragmentById(holder.binding.socialBarFcv.id)?.let {
            appCompatActivity.supportFragmentManager.commitNow {
                Log.d(LOGTAG, "remove ${it.tag}")
                // Acording to Google documentation fragment is also freed when removed
                remove(it)
            }
        }
    }

    private fun getRating(value: Double?, max: Double): String {
        val index = mapValueToIndex(value, max, scoreRatings.size)
        return scoreRatings[index]
    }

    private fun getScoreColor(value: Double?, max: Double): Int {
        val index = mapValueToIndex(value, max, ratingColors.size)
        return ratingColors[index].toInt()
    }

    private fun mapValueToIndex(value: Double?, max: Double, size: Int): Int =
        if (value != null && max > 0.0) ((size * value) / max).toInt().coerceAtMost(size - 1) else 0


    fun setMaxPopularity() {
        maxPopularity = persons
            .mapNotNull { it.popularity }
            .maxOrNull() ?: 0.0

        val sharedPref = appCompatActivity.getPreferences(Context.MODE_PRIVATE)
        val highscore = sharedPref.getFloat(appCompatActivity.getString(R.string.saved_high_score_key), 0f)
        if (maxPopularity > highscore) {
            with (sharedPref.edit()) {
                putFloat(appCompatActivity.getString(R.string.saved_high_score_key), maxPopularity.toFloat())
                // apply() // synchrone
                commit() // asynchrone
            }
        }
    }


    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    inner class PersonPopularViewHolder(var binding: PersonItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // set unique view id for Fragment Container so that
            // fragment with the same id can be attached
            binding.socialBarFcv.id= View.generateViewId()

            // set onClick listener
            binding.personItemCl.setOnClickListener {
                (appCompatActivity as OnPersonItemClickListener).onPersonItemClicked(adapterPosition)
            }
        }
    }


    /* ==========================================================================================
     * | Same implementation done with 'findViewById' to understand the role of the view holder |
     * ==========================================================================================

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonPopularAdapter.PersonPopularViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.person_item, parent, false)
        return PersonPopularViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: PersonPopularAdapter.PersonPopularViewHolder, position: Int) {
        // Get element from your dataset at this position
        val curItem = persons.get(position)

        // and replace the contents of the view with that element
        holder.nameTv.text = curItem.name
        holder.knownForTv.text = curItem.knownForDepartment
        holder.popularityTv.text = curItem.popularity.toString()
        Picasso.get()
            .load(IMAGE_BASE_URL + curItem.profilePath)
            .placeholder(android.R.drawable.progress_horizontal)
            .error(android.R.drawable.stat_notify_error)
            .into(holder.photoIv)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return persons.size
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class PersonPopularViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView
        val knownForTv: TextView
        val popularityTv: TextView
        val photoIv: ImageView
        init {
            nameTv = view.findViewById(R.id.name_tv)
            knownForTv = view.findViewById(R.id.known_for_tv)
            popularityTv = view.findViewById(R.id.popularity_tv)
            photoIv = view.findViewById(R.id.photo_iv)
        }
    }
     */
}

