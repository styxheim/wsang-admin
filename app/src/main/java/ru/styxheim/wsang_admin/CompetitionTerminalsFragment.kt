package ru.styxheim.wsang_admin

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.squareup.moshi.Moshi
import ru.styxheim.wsang_admin.databinding.FragmentCompetitionTerminalsBinding

class CompetitionTerminalsFragment : Fragment() {
  private var binding: FragmentCompetitionTerminalsBinding? = null
  private var competition: AdminAPI.RaceStatus = AdminAPI.RaceStatus(SyncPoint = 0)
  private val terminalList: MutableList<AdminAPI.TerminalStatus> = mutableListOf()
  private var transport: Transport? = null
  private val moshi: Moshi = Moshi.Builder().build()
  private val competitionRequestJsonAdapter =
    moshi.adapter(AdminAPI.CompetitionResponse::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    terminalList.clear()
    arguments?.getString("competition_response_json", null)?.let {
      val competitionResponse = competitionRequestJsonAdapter.fromJson(it)

      competition = competitionResponse!!.Competition
      terminalList.addAll(competitionResponse.TerminalList)
    }

    arguments?.getString("terminals_json", null)?.let {
      competitionRequestJsonAdapter.fromJson(it)?.let { jsonTerminalList ->
        terminalList.addAll(jsonTerminalList.TerminalList)
      }
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

  private fun selectTerminalsFromList() {
    val dialogBuilder = AlertDialog.Builder(requireContext())
    var dialog: AlertDialog? = null

    dialogBuilder.setTitle(R.string.updating)
    dialogBuilder.setMessage(R.string.terminals_loading)

    transport?.getTerminalsActivities(
      onBegin = { dialog = dialogBuilder.show() },
      onEnd = { dialog?.dismiss() },
      onFail = { message ->
        activity?.runOnUiThread {
          val failDialogBuilder = AlertDialog.Builder(requireContext())

          failDialogBuilder.setTitle(R.string.terminals_loading_error)
          failDialogBuilder.setMessage(message)
          failDialogBuilder.setNeutralButton(R.string.accept) { _, _ -> }
          failDialogBuilder.show()
        }
      },
      onResult = { terminalActivityList: AdminAPI.TerminalActivityList ->
        activity?.runOnUiThread {
          val terminalChooseBuilder = AlertDialog.Builder(requireContext())
          var terminalStatusList = terminalActivityList.TerminalList

          terminalStatusList =
            terminalStatusList.filter { terminalStatus ->
              terminalList.find { terminal -> terminal.TerminalString == terminalStatus.TerminalId } == null
            }
          terminalStatusList = terminalStatusList.sortedByDescending { it.TimeStamp }

          terminalChooseBuilder.setTitle(R.string.terminals_add)
          if (terminalActivityList.TerminalList.isEmpty()) {
            terminalChooseBuilder.setMessage(R.string.terminal_list_is_empty)
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
            terminalChooseBuilder.setPositiveButton(R.string.save) { _, _ -> /* TODO */ }
          }
          terminalChooseBuilder.show()
        }
      }
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding!!.terminalsAdd.setOnClickListener {
      selectTerminalsFromList()
    }
  }
}