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
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import ru.styxheim.wsang_admin.databinding.FragmentCompetitionBinding
import java.lang.NumberFormatException

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CompetitionFragment : Fragment() {
  private var binding: FragmentCompetitionBinding? = null
  private var transport: Transport? = null

  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionJsonAdapter:
      JsonAdapter<AdminAPI.RaceStatus> = moshi.adapter(AdminAPI.RaceStatus::class.java)
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)
  private var terminalList: MutableList<AdminAPI.TerminalStatus> = mutableListOf()
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
    binding = FragmentCompetitionBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
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

    binding?.disciplines?.apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = competitionDisciplineAdapter
    }

    binding?.disciplineAdd?.setOnClickListener {
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
          discipline.Gates.find { it == gateId }?.let {
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

  private fun loadCompetitionTerminals() {
    val loadingDialogBuilder = AlertDialog.Builder(requireContext()).setTitle(R.string.updating)
    var loadingDialog: AlertDialog? = null

    loadingDialogBuilder.setMessage(R.string.competition_loading)
    loadingDialogBuilder.setNeutralButton(R.string.accept) { _, _ -> }

    transport?.getCompetitionTerminals(
      competition.CompetitionId,
      onBegin = { activity?.runOnUiThread { loadingDialog = loadingDialogBuilder.show() } },
      onEnd = { activity?.runOnUiThread { loadingDialog?.dismiss() } },
      onFail = { message ->
        activity?.runOnUiThread {
          val errorDialog = AlertDialog.Builder(requireContext())

          errorDialog.setTitle(R.string.competition_loading_error)
          errorDialog.setMessage(message)
          errorDialog.show()
        }
      },
      onResult = { competitionTerminalList ->
        activity?.runOnUiThread {
          terminalList.clear()
          terminalList.addAll(competitionTerminalList.TerminalList)
          binding!!.terminals.text = terminalList.count().toString()
        }
      }
    )
  }

  private fun saveCompetition() {
    val savingDialogBuilder = AlertDialog.Builder(requireContext())
    var savingDialog: AlertDialog? = null

    savingDialogBuilder.setTitle(R.string.updating)
    savingDialogBuilder.setMessage(R.string.competition_saving)
    savingDialogBuilder.setNeutralButton(R.string.accept) { _, _ -> }

    transport?.setCompetition(
      competition,
      onBegin = {
        activity?.runOnUiThread {
          binding!!.competitionSave.isEnabled = false
          savingDialog = savingDialogBuilder.show()
        }
      },
      onEnd = {
        activity?.runOnUiThread {
          binding!!.competitionSave.isEnabled = true
          savingDialog?.dismiss()
        }
      },
      onFail = { message ->
        activity?.runOnUiThread {
          val errorDialog = AlertDialog.Builder(requireContext())

          errorDialog.setTitle(R.string.competition_saving_error)
          errorDialog.setMessage(message)
          errorDialog.show()
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

  private fun setupCompetitionSaveFab() {
    binding!!.competitionSave.setOnClickListener { saveCompetition() }
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
    editTextView.setText(mutableList.joinToString())

    dialogBuilder.setPositiveButton(R.string.save) { _, _ ->
      activity?.runOnUiThread {
        try {
          val list = editTextView.text.toString().split(',').map { it.trim().toInt() }

          mutableList.clear()
          mutableList.addAll(list)
          textView.text = mutableList.joinToString()
          competitionDisciplineAdapter?.notifyDataSetChanged()
          saveCompetitionToBundle()
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
    binding!!.competitionId.text = competition.CompetitionId.toString()
    /* timestamp */
    binding!!.competitionId.text = competition.TimeStamp.toString()
    /* crews count */
    binding!!.crewsCount.text = getString(R.string.crews_count_null)
    competition.Crews?.let {
      binding?.crewsCount?.text = it.size.toString()
    }

    /* penalties */
    val penaltiesOnClick = {
      updateListWithDialog(
        competition.Penalties!!,
        binding!!.penalties,
        R.string.penalties
      )
    }
    binding!!.penalties.setOnClickListener { penaltiesOnClick() }
    binding!!.penaltiesPlate.setOnClickListener { penaltiesOnClick() }
    binding!!.penalties.text = getString(R.string.penalties_null)
    if (competition.Penalties!!.isNotEmpty()) {
      binding!!.penalties.text = competition.Penalties!!.joinToString()
    }

    /* gates */
    val gatesOnClick = {
      updateListWithDialog(
        competition.Gates!!,
        binding!!.gates,
        R.string.edit_gates
      )
    }
    binding!!.gates.setOnClickListener { gatesOnClick() }
    binding!!.gatesPlate.setOnClickListener { gatesOnClick() }
    binding!!.gates.text = getString(R.string.gates_null)
    if (competition.Gates!!.isNotEmpty()) {
      binding!!.gates.text = competition.Gates!!.joinToString()
    }

    setupDisciplineAdapter()
    setupCompetitionSaveFab()

    activity?.title = getString(R.string.competition_unknown_value)
    if (competition.CompetitionName?.compareTo("") ?: 0 != 0) {
      activity?.title = competition.CompetitionName
    }

    binding!!.competitionManageTerminals.setOnClickListener {
      val competitionJson = competitionJsonAdapter.toJson(competition)
      val action = CompetitionFragmentDirections.actionToTerminalsFragment(competitionJson)

      findNavController().navigate(action)
    }

    binding!!.terminals.text = terminalList.count().toString()
    loadCompetitionTerminals()
  }
}