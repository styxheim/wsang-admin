package ru.styxheim.wsang_admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import ru.styxheim.wsang_admin.databinding.DisciplineGateItemBinding
import ru.styxheim.wsang_admin.databinding.TerminalsItemBinding

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
    })
  }

  override fun onBindViewHolder(holder: TerminalsItemHolder, position: Int) {
    holder.bind(competition, terminals[position])
  }

  override fun getItemCount(): Int = terminals.size
}

class TerminalsItemHolder(
  private val binding: TerminalsItemBinding,
  private val onClickGate: (terminalString: String, disciplineGates: List<Int>) -> Unit,
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

  fun bind(competition: AdminAPI.RaceStatus, terminal: AdminAPI.TerminalStatus) {
    val inflater = LayoutInflater.from(itemView.context)
    val competitionGates = competition.Gates?.toList() ?: listOf()
    val terminalGates = if (terminal.Disciplines.isEmpty()) {
      mutableListOf()
    } else {
      terminal.Disciplines[0].Gates.toMutableList()
    }

    binding.gatesList.removeAllViews()

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

      binding.terminalName.text = terminal.TerminalId
      (gateViewBinding.gate.parent as ViewGroup).removeView(gateViewBinding.gate)
      gateViewBinding.gate.text = gateNo.toString()
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