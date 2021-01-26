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
  private val competitionResponseJsonAdapter =
    moshi.adapter(AdminAPI.Response.Competition::class.java)
  private var isNewCompetition: Boolean = false
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)
  private var terminalList: MutableList<AdminAPI.TerminalStatus> = mutableListOf()
  private var competitionDisciplineAdapter: CompetitionDisciplineAdapter? = null

  /* 'true' if discipline is added or removed. Need for sync terminal's gates to discipline list */
  private var isDisciplinesChanged: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.getString("competition_json", null)?.let {
      competition = competitionJsonAdapter.fromJson(it)!!
    }

    arguments?.getBoolean("is_new_competition", false)?.let {
      isNewCompetition = it
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
        setDiscipline = { discipline ->
          competition.Disciplines?.add(discipline)
          isDisciplinesChanged = true
        }
      )
    }

    competitionDisciplineAdapter!!.setOnClickName { id ->
      showDisciplineNameDialog(
        getDiscipline = {
          competition.Disciplines?.find { it.Id == id }!!
        },
        setDiscipline = { },
        delDiscipline = { discipline ->
          competition.Disciplines?.remove(discipline)
          isDisciplinesChanged = true
        }
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

  private fun loadCompetition() {
    val loadingDialogBuilder = AlertDialog.Builder(requireContext()).setTitle(R.string.updating)
    var loadingDialog: AlertDialog? = null

    loadingDialogBuilder.setCancelable(false)
    loadingDialogBuilder.setMessage(R.string.competition_loading)
    loadingDialogBuilder.setNeutralButton(R.string.accept) { _, _ -> }

    transport?.getCompetition(
      competition.CompetitionId,
      onBegin = { activity?.runOnUiThread { loadingDialog = loadingDialogBuilder.show() } },
      onEnd = { activity?.runOnUiThread { loadingDialog?.dismiss() } },
      onFail = { message ->
        activity?.runOnUiThread {
          Utils.showInfoDialog(
            requireContext(),
            R.string.competition_loading_error,
            message
          )
        }
      },
      onResult = { competitionResponse ->
        activity?.runOnUiThread {
          competition = competitionResponse.Competition
          terminalList.clear()
          terminalList.addAll(competitionResponse.TerminalList)
          binding!!.terminals.text = terminalList.count().toString()
          isNewCompetition = false
          saveCompetitionToBundle()
          updateView()
        }
      }
    )
  }

  /* Sync competition gates to terminals permissions:
   * Protocol allows set different permissions to each discipline in terminals,
   * but this ability do not need now and all terminal's disciplines must
   * have equal set of gates
   */
  private fun syncCompetitionGatesToTerminals() {
    terminalList.forEach { terminal ->
      var terminalGates = if (terminal.Disciplines.isEmpty()) {
        mutableListOf()
      } else {
        terminal.Disciplines[0].Gates.toMutableList()
      }

      /* remove gate number from terminal if gate removed from competition */
      terminalGates = terminalGates.filter { gateNo ->
        competition.Gates?.any { competitionGateNo -> gateNo == competitionGateNo } == true
      }.toMutableList()

      /* copy one set of gates to all disciplines */
      terminal.Disciplines.clear()
      competition.Disciplines?.forEach { discipline ->
        val terminalDiscipline =
          AdminAPI.TerminalDiscipline(Id = discipline.Id, Gates = terminalGates)

        terminal.Disciplines.add(terminalDiscipline)
      }
    }
  }

  private fun saveCompetitionTerminals(onEnd: () -> Unit, onSuccess: () -> Unit) {
    val savingDialogBuilder = AlertDialog.Builder(requireContext())
    var savingDialog: AlertDialog? = null

    savingDialogBuilder.setCancelable(false)
    savingDialogBuilder.setTitle(R.string.updating)
    savingDialogBuilder.setMessage(R.string.terminals_saving)
    savingDialogBuilder.setNeutralButton(R.string.accept) { _, _ -> }

    transport?.setCompetitionTerminals(competition.CompetitionId,
      terminalList,
      onBegin = { activity?.runOnUiThread { savingDialog = savingDialogBuilder.show() } },
      onEnd = { activity?.runOnUiThread { savingDialog?.dismiss(); onEnd() } },
      onFail = { message ->
        activity?.runOnUiThread {
          Utils.showInfoDialog(requireContext(), R.string.updating, message)
        }
      },
      onResult = {
        activity?.runOnUiThread {
          onSuccess()
          Toast.makeText(requireContext(), R.string.terminals_saving_success, Toast.LENGTH_SHORT)
            .show()
        }
      }
    )
  }

  private fun saveCompetition() {
    val savingDialogBuilder = AlertDialog.Builder(requireContext())
    var savingDialog: AlertDialog? = null

    savingDialogBuilder.setCancelable(false)
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
          savingDialog?.dismiss()
        }
      },
      onFail = { message ->
        activity?.runOnUiThread {
          binding!!.competitionSave.isEnabled = true
          Utils.showInfoDialog(requireContext(), R.string.competition_saving_error, message)
        }
      },
      onResult = {
        activity?.runOnUiThread {
          Toast.makeText(requireContext(), R.string.competition_saving_success, Toast.LENGTH_SHORT)
            .show()
          if (isDisciplinesChanged) {
            syncCompetitionGatesToTerminals()
            isDisciplinesChanged = false
            saveCompetitionTerminals(
              onEnd = { binding!!.competitionSave.isEnabled = true },
              onSuccess = { loadCompetition() })
          } else {
            binding!!.competitionSave.isEnabled = true
            loadCompetition()
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

  private fun updateStringWithDialog(
    getEditString: () -> String,
    setEditString: (message: String) -> Unit,
    @StringRes title: Int
  ) {
    val editTextView = EditText(requireContext())
    val dialogBuilder = AlertDialog.Builder(requireContext()).setTitle(title)
      .setView(editTextView)

    editTextView.isSingleLine = false
    editTextView.setText(getEditString())

    dialogBuilder.setPositiveButton(R.string.save) { _, _ ->
      activity?.runOnUiThread {
        setEditString(editTextView.text.toString())
      }
    }

    dialogBuilder.setNegativeButton(R.string.cancel) { _, _ ->
    }

    dialogBuilder.show()
  }

  private fun annoyingDialog(title: String, onYes: () -> Unit, onNo: () -> Unit) {
    val dialogBuilder = AlertDialog.Builder(requireContext())

    dialogBuilder.setTitle(title)
    dialogBuilder.setMessage(R.string.are_you_sure)
    dialogBuilder.setNegativeButton(R.string.no) { _, _ -> onNo() }
    dialogBuilder.setPositiveButton(R.string.yes) { _, _ -> onYes() }
    dialogBuilder.show()
  }

  private fun wipeWithDialog() {
    val dialogBuilder = AlertDialog.Builder(requireContext())
    val savingDialogBuilder = AlertDialog.Builder(requireContext())
    var savingDialog: AlertDialog? = null

    savingDialogBuilder.setCancelable(false)
    savingDialogBuilder.setTitle(R.string.updating)
    savingDialogBuilder.setMessage(R.string.competition_saving)

    dialogBuilder.setTitle(R.string.competition_wipe_dialog_title)
    dialogBuilder.setMessage(R.string.competition_wipe_dialog_message)
    dialogBuilder.setNegativeButton(R.string.no) { _, _ -> }
    dialogBuilder.setPositiveButton(R.string.yes) { _, _ ->
      binding!!.wipeButton.isEnabled = false
      annoyingDialog(
        getString(R.string.competition_wipe_dialog_title),
        onYes = {
          transport!!.wipeCompetition(competition.CompetitionId,
            onBegin = { activity?.runOnUiThread { savingDialog = savingDialogBuilder.show() } },
            onEnd = {
              activity?.runOnUiThread {
                binding!!.wipeButton.isEnabled = true
                savingDialog?.dismiss()
              }
            },
            onFail = { message ->
              activity?.runOnUiThread {
                Utils.showInfoDialog(requireContext(), R.string.competition_saving_error, message)
              }
            },
            onResult = { activity?.runOnUiThread { loadCompetition() } })
        },
        onNo = { binding!!.wipeButton.isEnabled = true })
    }
    dialogBuilder.show()
  }

  private fun setDefaultWithDialog() {
    val dialogBuilder = AlertDialog.Builder(requireContext())
    val savingDialogBuilder = AlertDialog.Builder(requireContext())
    var savingDialog: AlertDialog? = null

    savingDialogBuilder.setCancelable(false)
    savingDialogBuilder.setTitle(R.string.updating)
    savingDialogBuilder.setMessage(R.string.competition_saving)

    dialogBuilder.setTitle(R.string.competition_set_default_dialog_title)
    dialogBuilder.setMessage(R.string.competition_set_default_dialog_message)
    dialogBuilder.setNegativeButton(R.string.no) { _, _ -> }
    dialogBuilder.setPositiveButton(R.string.yes) { _, _ ->
      binding!!.setDefaultButton.isEnabled = false
      transport!!.setDefaultCompetition(competition.CompetitionId,
        onBegin = { activity?.runOnUiThread { savingDialog = savingDialogBuilder.show() } },
        onEnd = {
          activity?.runOnUiThread {
            binding!!.setDefaultButton.isEnabled = true
            savingDialog?.dismiss()
          }
        },
        onFail = { message ->
          activity?.runOnUiThread {
            Utils.showInfoDialog(requireContext(), R.string.competition_saving_error, message)
          }
        },
        onResult = { activity?.runOnUiThread { loadCompetition() } })
    }
    dialogBuilder.show()
  }

  private fun updateView() {
    if (isNewCompetition) {
      binding!!.terminalsPlate.visibility = View.GONE
      binding!!.competitionControlPlate.visibility = View.GONE
    } else {
      binding!!.terminalsPlate.visibility = View.VISIBLE
      binding!!.terminalsPlate.setOnClickListener {
        val competitionResponse = AdminAPI.Response.Competition(
          Competition = competition,
          TerminalList = terminalList
        )
        val competitionResponseJson = competitionResponseJsonAdapter.toJson(competitionResponse)
        val action =
          CompetitionFragmentDirections.actionToTerminalsFragment(competitionResponseJson)

        findNavController().navigate(action)
      }

      binding!!.wipeButton.setOnClickListener { wipeWithDialog() }
      binding!!.setDefaultButton.setOnClickListener { setDefaultWithDialog() }
      setupDisciplineAdapter()
    }

    /* competition name */
    activity?.title = getString(R.string.competition_unknown_value)
    binding!!.competitionName.text = getString(R.string.competition_unknown_value)
    if (competition.CompetitionName?.compareTo("") ?: 0 != 0) {
      activity?.title = competition.CompetitionName
      binding!!.competitionName.text = competition.CompetitionName
    }
    /* competition id */
    binding!!.competitionNameId.text =
      getString(R.string.competition_id_title, competition.CompetitionId)
    /* timestamp */
    binding!!.timestamp.text = competition.TimeStamp.toString()
    /* crews count */
    binding!!.crewsCount.text = getString(R.string.crews_count_null)
    competition.Crews?.let {
      binding?.crewsCount?.text = it.size.toString()
    }

    binding!!.penalties.text = getString(R.string.penalties_null)
    if (competition.Penalties!!.isNotEmpty()) {
      binding!!.penalties.text = competition.Penalties!!.joinToString()
    }

    binding!!.gates.text = getString(R.string.gates_null)
    if (competition.Gates!!.isNotEmpty()) {
      binding!!.gates.text = competition.Gates!!.joinToString()
    }

    competitionDisciplineAdapter?.notifyDataSetChanged()
  }

  private fun setupCompetitionNameOnClick() {
    val defaultCompetitionName =
      getString(R.string.competition_name_default, competition.CompetitionId)

    /* competition name */
    val competitionNameOnClick = {
      updateStringWithDialog(
        {
          (competition.CompetitionName ?: "").let {
            if (it.isEmpty()) {
              defaultCompetitionName
            } else {
              it
            }
          }
        },
        { message ->
          if (message.isEmpty()) {
            binding?.competitionName?.text = defaultCompetitionName
            competition.CompetitionName = defaultCompetitionName
          } else {
            binding?.competitionName?.text = message
            competition.CompetitionName = message
          }
          saveCompetitionToBundle()
          updateView()
        },
        R.string.competition_name_edit
      )
    }
    binding!!.competitionName.setOnClickListener { competitionNameOnClick() }
    binding!!.competitionNameId.setOnClickListener { competitionNameOnClick() }
    binding!!.competitionNamePlate.setOnClickListener { competitionNameOnClick() }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupCompetitionNameOnClick()

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

    /* gates */
    val gatesOnClick = {
      updateListWithDialog(
        competition.Gates!!,
        binding!!.gates,
        R.string.edit_gates
      )
      isDisciplinesChanged = true
    }
    binding!!.gates.setOnClickListener { gatesOnClick() }
    binding!!.gatesPlate.setOnClickListener { gatesOnClick() }

    setupDisciplineAdapter()
    setupCompetitionSaveFab()

    binding!!.terminals.text = terminalList.count().toString()
    if (!isNewCompetition)
      loadCompetition()
    updateView()
  }
}