package ru.styxheim.wsang_admin

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
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
    binding.disciplineName.text = discipline.Name
    binding.disciplineName.setOnClickListener { onClickName(discipline.Id) }
    binding.disciplinePlate.setOnClickListener { onClickName(discipline.Id) }
    binding.gatesList.removeAllViews()
    competitionGates.forEach { competitionGateNo ->
      val inflater = LayoutInflater.from(itemView.context)
      val view_binding = DisciplineGateItemBinding.inflate(inflater, null, false)
      var gateView: TextView? = null

      /* TODO: replace to style instead two buttons */
      discipline.Gates.find { disciplineGateNo -> disciplineGateNo == competitionGateNo }?.let {
        gateView = view_binding.gateSelected
      } ?: run {
        gateView = view_binding.gateUnselected
      }

      (gateView?.parent as ViewGroup).removeView(gateView)
      gateView?.text = competitionGateNo.toString()
      gateView?.setOnClickListener { onClickGate(discipline.Id, competitionGateNo) }
      binding.gatesList.addView(gateView)
    }
  }
}