package ru.styxheim.wsang_admin

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.Moshi
import ru.styxheim.wsang_admin.databinding.FragmentCompetitionTerminalsBinding

class CompetitionTerminalsFragment : Fragment() {
  private var binding: FragmentCompetitionTerminalsBinding? = null
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)
  private val terminalList: MutableList<AdminAPI.TerminalStatus> = mutableListOf()
  private var transport: Transport? = null
  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionRequestJsonAdapter =
    moshi.adapter(AdminAPI.Response.Competition::class.java)

  private var terminalsAdapter: TerminalsAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    terminalList.clear()
    arguments?.getString("competition_response_json", null)?.let {
      val competitionResponse = competitionRequestJsonAdapter.fromJson(it)

      competition = competitionResponse!!.Competition
      terminalList.addAll(competitionResponse.TerminalList)
    }

    transport = Transport(PreferenceManager.getDefaultSharedPreferences(context)!!)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentCompetitionTerminalsBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }

  private fun saveTerminals() {
    val dialogBuilder = AlertDialog.Builder(requireContext())
    var dialog: AlertDialog? = null

    dialogBuilder.setCancelable(false)
    dialogBuilder.setTitle(R.string.updating)
    dialogBuilder.setMessage(R.string.terminals_saving)

    transport?.setCompetitionTerminals(competition.CompetitionId, terminalList,
      onBegin = { activity?.runOnUiThread { dialog = dialogBuilder.show() } },
      onEnd = { activity?.runOnUiThread { dialog?.dismiss() } },
      onFail = { message ->
        activity?.runOnUiThread {
          Utils.showInfoDialog(
            requireContext(),
            R.string.terminals_saving_error,
            message
          )
        }
      },
      onResult = {
        activity?.runOnUiThread {
          Toast.makeText(requireContext(), R.string.terminals_saving_success, Toast.LENGTH_SHORT)
            .show()
        }
      })
  }

  private fun selectTerminalsFromList() {
    val dialogBuilder = AlertDialog.Builder(requireContext())
    var dialog: AlertDialog? = null

    dialogBuilder.setCancelable(false)
    dialogBuilder.setTitle(R.string.updating)
    dialogBuilder.setMessage(R.string.terminals_loading)

    transport?.getTerminalsActivities(
      onBegin = { activity?.runOnUiThread { dialog = dialogBuilder.show() } },
      onEnd = { activity?.runOnUiThread { dialog?.dismiss() } },
      onFail = { message ->
        activity?.runOnUiThread {
          Utils.showInfoDialog(
            requireContext(),
            R.string.terminals_loading_error,
            message
          )
        }
      },
      onResult = { terminalActivityList: AdminAPI.Response.TerminalActivityList ->
        activity?.runOnUiThread {
          val terminalChooseBuilder = AlertDialog.Builder(requireContext())
          var terminalStatusList = terminalActivityList.TerminalList

          terminalStatusList =
            terminalStatusList.filter { terminalStatus ->
              terminalList.find { terminal -> terminal.TerminalId == terminalStatus.TerminalId } == null
            }
          terminalStatusList = terminalStatusList.sortedByDescending { it.TimeStamp }

          terminalChooseBuilder.setTitle(R.string.terminals_add)
          if (terminalActivityList.TerminalList.isEmpty()) {
            terminalChooseBuilder.setMessage(R.string.terminal_list_is_empty)
            terminalChooseBuilder.setNeutralButton(R.string.accept) { _, _ -> }
          } else if (terminalStatusList.isEmpty()) {
            terminalChooseBuilder.setMessage(R.string.terminal_list_all_registered)
            terminalChooseBuilder.setNeutralButton(R.string.accept) { _, _ -> }
          } else {
            val nameList = terminalStatusList.map { it.TerminalId }.toTypedArray()
            val isCheckedList = BooleanArray(nameList.size) { false }

            terminalChooseBuilder.setMultiChoiceItems(
              nameList,
              isCheckedList
            ) { _, which, isChecked ->
              isCheckedList[which] = isChecked
            }

            terminalChooseBuilder.setNegativeButton(R.string.cancel) { _, _ -> }
            terminalChooseBuilder.setPositiveButton(R.string.save) { _, _ ->
              for ((index, terminalName) in nameList.withIndex()) {
                if (!isCheckedList[index]) continue
                terminalList.add(
                  AdminAPI.TerminalStatus(
                    TimeStamp = 0,
                    TerminalId = terminalName,
                    Disciplines = mutableListOf()
                  )
                )
              }
              terminalsAdapter?.notifyDataSetChanged()
            }
          }
          terminalChooseBuilder.show()
        }
      }
    )
  }

  private fun setupTerminalsAdapter() {
    terminalsAdapter = TerminalsAdapter(competition, terminalList)

    terminalsAdapter!!.onDisciplineGatesChange = { terminalString, disciplineGates ->
      val terminal = terminalList.find { it.TerminalId == terminalString }!!

      terminal.Disciplines.clear()
      competition.Disciplines?.forEach { competitionDiscipline ->
        terminal.Disciplines.add(
          AdminAPI.TerminalDiscipline(
            Id = competitionDiscipline.Id,
            disciplineGates.toMutableList()
          )
        )
      }
      terminalsAdapter!!.notifyDataSetChanged()
    }

    binding?.terminals?.apply {
      layoutManager = LinearLayoutManager(activity)
      adapter = terminalsAdapter
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding!!.terminalsAdd.setOnClickListener {
      selectTerminalsFromList()
    }
    binding!!.terminalsSave.setOnClickListener {
      saveTerminals()
    }
    setupTerminalsAdapter()
  }
}