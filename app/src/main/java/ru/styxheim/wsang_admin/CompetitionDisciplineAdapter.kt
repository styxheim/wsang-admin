package ru.styxheim.wsang_admin

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import ru.styxheim.wsang_admin.databinding.CompetitionDisciplineItemBinding
import ru.styxheim.wsang_admin.databinding.DisciplineGateItemBinding

class CompetitionDisciplineAdapter(
  private val disciplines: MutableList<AdminAPI.Discipline>,
  private val competitionGates: MutableList<Int>
) : RecyclerView.Adapter<CompetitionDisciplineItemHolder>() {
  private var onClickNameListener: ((id: Int) -> Unit)? = null
  private var onClickGateListener: ((disciplineId: Int, gateId: Int) -> Unit)? = null

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CompetitionDisciplineItemHolder {
    val inflater = LayoutInflater.from(parent.context)
    val holderBinding = CompetitionDisciplineItemBinding.inflate(inflater, parent, false)

    return CompetitionDisciplineItemHolder(
      holderBinding,
      onClickName = { id -> onClickNameListener?.invoke(id) },
      onClickGate = { disciplineId, gateId ->
        onClickGateListener?.invoke(disciplineId, gateId)
      }
    )
  }

  override fun getItemCount(): Int = disciplines.size

  override fun onBindViewHolder(holder: CompetitionDisciplineItemHolder, position: Int) {
    holder.bind(competitionGates, disciplines[position])
  }

  fun setOnClickName(onClickName: (id: Int) -> Unit) {
    onClickNameListener = onClickName
  }

  fun setOnClickGate(onClickGate: (disciplineId: Int, gateId: Int) -> Unit) {
    onClickGateListener = onClickGate
  }
}

class CompetitionDisciplineItemHolder(
  private val binding: CompetitionDisciplineItemBinding,
  private val onClickName: (id: Int) -> Unit,
  private val onClickGate: (disciplineId: Int, gateId: Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

  @SuppressLint("InflateParams")
  fun bind(competitionGates: List<Int>, discipline: AdminAPI.Discipline) {
    val colorGateSelected =
      ResourcesCompat.getColor(
        binding.root.context.resources,
        R.color.gate_selected,
        binding.root.context.theme
      )
    val colorGateUnselected =
      ResourcesCompat.getColor(
        binding.root.context.resources,
        R.color.gate_unselected,
        binding.root.context.theme
      )

    binding.disciplineName.text = discipline.Name
    binding.disciplineName.setOnClickListener { onClickName(discipline.Id) }
    binding.disciplinePlate.setOnClickListener { onClickName(discipline.Id) }
    binding.gatesList.removeAllViews()
    competitionGates.forEach { competitionGateNo ->
      val inflater = LayoutInflater.from(itemView.context)
      val view_binding = DisciplineGateItemBinding.inflate(inflater, null, false)
      val gateView: TextView = view_binding.gate

      discipline.Gates.find { disciplineGateNo -> disciplineGateNo == competitionGateNo }?.let {
        gateView.setBackgroundColor(colorGateSelected)
      } ?: run {
        gateView.setBackgroundColor(colorGateUnselected)
      }

      (gateView.parent as ViewGroup).removeView(gateView)
      gateView.text = competitionGateNo.toString()
      gateView.setOnClickListener { onClickGate(discipline.Id, competitionGateNo) }
      binding.gatesList.addView(gateView)
    }
  }
}