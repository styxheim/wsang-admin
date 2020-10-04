package ru.styxheim.wsang_admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.fragment_competition.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CompetitionFragment : Fragment() {

  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionJsonAdapter:
      JsonAdapter<AdminAPI.RaceStatus> = moshi.adapter(AdminAPI.RaceStatus::class.java)
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.getString("competition_json", null)?.let {
      competition = competitionJsonAdapter.fromJson(it)!!
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_competition, container, false)
  }

  private fun setupDisciplineAdapter() {
    val competitionDisciplineAdapter =
      CompetitionDisciplineAdapter(competition.Disciplines!!, requireContext())

    disciplines.apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = competitionDisciplineAdapter
    }

    disciplineAdd.setOnClickListener {
      competition.Disciplines?.add(AdminAPI.Discipline(Id = 1, Name = "1"))
      competitionDisciplineAdapter.notifyDataSetChanged()
      saveCompetitionToBundle()
    }
  }

  private fun saveCompetitionToBundle() {
    val competitionJson = competitionJsonAdapter.toJson(competition)
    val bundle = Bundle()

    bundle.putString("competition_json", competitionJson)
    arguments = bundle
  }

  private fun setupCompetitionSaveFab() {
    competitionSave.setOnClickListener {
      Toast.makeText(
        context,
        "Save stub",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    /* competition id */
    competition_id.text = competition.CompetitionId.toString()
    /* timestamp */
    timestamp.text = competition.TimeStamp.toString()
    /* crews count */
    crews_count.text = getString(R.string.crews_count_null)
    competition.Crews?.let {
      crews_count.text = it.size.toString()
    }

    /* penalties */
    penalties.text = getString(R.string.penalties_null)
    if (competition.Penalties!!.isNotEmpty()) {
      penalties.text = competition.Penalties!!.joinToString()
    }

    /* gates */
    gates.text = getString(R.string.gates_null)
    if (competition.Gates!!.isNotEmpty()) {
      gates.text = competition.Gates!!.joinToString()
    }

    setupDisciplineAdapter()
    setupCompetitionSaveFab()

    activity?.title = getString(R.string.competition_unknown_value)
    if (competition.CompetitionName?.compareTo("") ?: 0 != 0) {
      activity?.title = competition.CompetitionName
    }
  }
}