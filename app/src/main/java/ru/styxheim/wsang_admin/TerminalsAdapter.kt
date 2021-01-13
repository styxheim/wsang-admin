package ru.styxheim.wsang_admin

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import ru.styxheim.wsang_admin.databinding.DisciplineGateItemBinding
import ru.styxheim.wsang_admin.databinding.TerminalsItemBinding

const val GATE_START: Int = -2
const val GATE_FINISH: Int = -3

class TerminalsAdapter(
  private val competition: AdminAPI.RaceStatus,
  private val terminals: MutableList<AdminAPI.TerminalStatus>
) :
  RecyclerView.Adapter<TerminalsItemHolder>() {
  var onDisciplineGatesChange: ((terminalString: String, disciplineGates: List<Int>) -> Unit)? =
    null

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): TerminalsItemHolder {
    val inflater = LayoutInflater.from(parent.context)
    val holderBinding = TerminalsItemBinding.inflate(inflater, parent, false)

    return TerminalsItemHolder(holderBinding, onClickGate = { terminalString, disciplineGates ->
      onDisciplineGatesChange?.invoke(terminalString, disciplineGates)
    },
      onItemRemove = { position ->
        terminals.removeAt(position)
        notifyItemRemoved(position)
      })
  }

  override fun onBindViewHolder(holder: TerminalsItemHolder, position: Int) {
    holder.bind(position, competition, terminals[position])
  }

  override fun getItemCount(): Int = terminals.size
}

class TerminalsItemHolder(
  private val binding: TerminalsItemBinding,
  private val onClickGate: (terminalString: String, disciplineGates: List<Int>) -> Unit,
  private val onItemRemove: (position: Int) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
  private var colorGateSelected: Int? = null
  private var colorGateUnselected: Int? = null

  init {
    binding.root.context.let { context ->
      colorGateSelected =
        ResourcesCompat.getColor(context.resources, R.color.gate_selected, context.theme)
      colorGateUnselected =
        ResourcesCompat.getColor(context.resources, R.color.gate_unselected, context.theme)
    }
  }

  private fun removeTerminalWithDialog(terminalString: String, onRemove: () -> Unit) {
    val dialogBuilder = AlertDialog.Builder(binding.root.context)

    dialogBuilder.setTitle(R.string.terminal_remove_title)
    dialogBuilder.setMessage(
      binding.root.context.resources.getString(
        R.string.terminal_remove_message,
        terminalString
      )
    )

    dialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> }
    dialogBuilder.setPositiveButton(R.string.accept) { _, _ -> onRemove() }

    dialogBuilder.show()
  }

  private fun getGateName(gateNo: Int): String {
    if (gateNo == GATE_FINISH) {
      return binding.root.resources.getString(R.string.gate_finish)
    } else if (gateNo == GATE_START) {
      return binding.root.resources.getString(R.string.gate_start)
    }
    return gateNo.toString()
  }

  fun bind(position: Int, competition: AdminAPI.RaceStatus, terminal: AdminAPI.TerminalStatus) {
    val inflater = LayoutInflater.from(itemView.context)
    val competitionGates = competition.Gates?.toMutableList() ?: mutableListOf()
    val terminalGates = if (terminal.Disciplines.isEmpty()) {
      mutableListOf()
    } else {
      terminal.Disciplines[0].Gates.toMutableList()
    }

    competitionGates.add(0, GATE_START)
    competitionGates.add(GATE_FINISH)
    binding.gatesList.removeAllViews()
    binding.terminalName.text = terminal.TerminalId
    binding.terminalPlate.setOnClickListener {
      removeTerminalWithDialog(terminal.TerminalId, onRemove = { onItemRemove(position) })
    }

    if (competition.Disciplines?.isEmpty() == true) {
      /* do not display gates if competition contain no disciplines */
      return
    }

    competitionGates.forEach { gateNo ->
      val gateViewBinding = DisciplineGateItemBinding.inflate(inflater, null, false)

      terminalGates.find { disciplineGateNo -> disciplineGateNo == gateNo }
        ?.let {
          gateViewBinding.gate.setBackgroundColor(colorGateSelected!!)
        } ?: run {
        gateViewBinding.gate.setBackgroundColor(colorGateUnselected!!)
      }

      (gateViewBinding.gate.parent as ViewGroup).removeView(gateViewBinding.gate)
      gateViewBinding.gate.text = getGateName(gateNo)
      gateViewBinding.gate.setOnClickListener {
        if (terminalGates.any { it == gateNo }) {
          terminalGates.remove(gateNo)
        } else {
          terminalGates.add(gateNo)
        }

        onClickGate(terminal.TerminalId, terminalGates)
      }
      binding.gatesList.addView(gateViewBinding.gate)
    }
  }
}