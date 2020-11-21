package ru.styxheim.wsang_admin

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.fragment_competition.*
import java.lang.NumberFormatException

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CompetitionFragment : Fragment() {
  private var transport: Transport? = null

  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionJsonAdapter:
      JsonAdapter<AdminAPI.RaceStatus> = moshi.adapter(AdminAPI.RaceStatus::class.java)
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)
  private var competitionDisciplineAdapter: CompetitionDisciplineAdapter? = null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.getString("competition_json", null)?.let {
      competition = competitionJsonAdapter.fromJson(it)!!
    }

    transport = Transport(PreferenceManager.getDefaultSharedPreferences(context)!!)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_competition, container, false)
  }

  private fun showDisciplineNameDialog(
    getDiscipline: () -> AdminAPI.Discipline,
    setDiscipline: (discipline: AdminAPI.Discipline) -> Unit,
    delDiscipline: ((discipline: AdminAPI.Discipline) -> Unit)? = null
  ) {
    val discipline = getDiscipline()
    val disciplineNameEdit = EditText(requireContext())
    val dialogBuilder = AlertDialog.Builder(requireContext()).setTitle(R.string.discipline_name)
      .setView(disciplineNameEdit)

    disciplineNameEdit.isSingleLine = true
    disciplineNameEdit.hint = getString(R.string.set_discipline_name)
    disciplineNameEdit.setText(discipline.Name)
    dialogBuilder.setPositiveButton(R.string.save) { _, _ ->
      discipline.Name = disciplineNameEdit.text.toString()
      setDiscipline(discipline)
      competitionDisciplineAdapter?.notifyDataSetChanged()
      saveCompetitionToBundle()
    }
    dialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> }
    delDiscipline?.let {
      dialogBuilder.setNeutralButton(R.string.delete) { _, _ ->
        it(discipline)
        competitionDisciplineAdapter?.notifyDataSetChanged()
        saveCompetitionToBundle()
      }
    }
    dialogBuilder.show()
  }

  private fun setupDisciplineAdapter() {
    competitionDisciplineAdapter =
      CompetitionDisciplineAdapter(
        competition.Disciplines!!,
        competition.Gates!!
      )

    disciplines.apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = competitionDisciplineAdapter
    }

    disciplineAdd.setOnClickListener {
      showDisciplineNameDialog(
        getDiscipline = {
          AdminAPI.Discipline(
            Id = ((competition.Disciplines?.maxByOrNull { it -> it.Id })?.Id ?: 0) + 1,
          )
        },
        setDiscipline = { discipline -> competition.Disciplines?.add(discipline) }
      )
    }

    competitionDisciplineAdapter!!.setOnClickName { id ->
      showDisciplineNameDialog(
        getDiscipline = {
          competition.Disciplines?.find { it.Id == id }!!
        },
        setDiscipline = { },
        delDiscipline = { discipline -> competition.Disciplines?.remove(discipline) }
      )
    }
    competitionDisciplineAdapter!!.setOnClickGate { disciplineId, gateId ->
      competition.Disciplines?.find { discipline -> discipline.Id == disciplineId }
        ?.let { discipline ->
          discipline.Gates?.find { it == gateId }?.let {
            discipline.Gates.remove(gateId)
          } ?: run {
            discipline.Gates.add(gateId)
          }
          competitionDisciplineAdapter?.notifyDataSetChanged()
        }
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
      transport?.setCompetition(
        competition,
        onBegin = { activity?.runOnUiThread { competitionSave.isEnabled = false } },
        onEnd = { activity?.runOnUiThread { competitionSave.isEnabled = true } },
        onFail = { message ->
          activity?.runOnUiThread {
            Toast.makeText(
              context,
              "Save failed: {$message}",
              Toast.LENGTH_SHORT
            ).show()
          }
        },
        onResult = {
          activity?.runOnUiThread {
            activity?.runOnUiThread {
              Toast.makeText(
                context,
                "Successfull save",
                Toast.LENGTH_SHORT
              ).show()
            }
          }
        }
      )
    }
  }

  private fun updateListWithDialog(
    mutableList: MutableList<Int>,
    textView: TextView,
    @StringRes title: Int
  ) {
    val editTextView = EditText(requireContext())
    val dialogBuilder = AlertDialog.Builder(requireContext()).setTitle(title)
      .setView(editTextView)

    editTextView.isSingleLine = true
    editTextView.hint = getString(R.string.edit_list_hint)
    editTextView.setText(textView.text)

    dialogBuilder.setPositiveButton(R.string.save) { _, _ ->
      activity?.runOnUiThread {
        try {
          val list = editTextView.text.toString().split(',').map { it.trim().toInt() }

          mutableList.clear()
          mutableList.addAll(list)
          textView.text = mutableList.joinToString()
          competitionDisciplineAdapter?.notifyDataSetChanged()
        } catch (e: NumberFormatException) {
          Toast.makeText(requireContext(), R.string.edit_list_invalid_format, Toast.LENGTH_SHORT)
            .show()
        }
      }
    }

    dialogBuilder.setNegativeButton(R.string.cancel) { _, _ ->
    }

    dialogBuilder.show()
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
    penalties.setOnClickListener {
      updateListWithDialog(
        competition.Penalties!!,
        penalties,
        R.string.penalties
      )
    }
    penalties.text = getString(R.string.penalties_null)
    if (competition.Penalties!!.isNotEmpty()) {
      penalties.text = competition.Penalties!!.joinToString()
    }

    /* gates */
    gates.setOnClickListener {
      updateListWithDialog(
        competition.Gates!!,
        gates,
        R.string.edit_gates
      )
    }
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