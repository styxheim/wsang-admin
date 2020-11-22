package ru.styxheim.wsang_admin

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.competition_discipline_item.view.*
import kotlinx.android.synthetic.main.discipline_gate_item.view.*

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

    return CompetitionDisciplineItemHolder(
      inflater,
      parent,
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
  inflater: LayoutInflater, parent: ViewGroup,
  private val onClickName: (id: Int) -> Unit,
  private val onClickGate: (disciplineId: Int, gateId: Int) -> Unit
) :
  RecyclerView.ViewHolder(inflater.inflate(R.layout.competition_discipline_item, parent, false)) {

  init {
  }

  @SuppressLint("InflateParams")
  fun bind(competitionGates: List<Int>, discipline: AdminAPI.Discipline) {
    itemView.disciplineName?.let {
      it.text = discipline.Name
      it.setOnClickListener { onClickName(discipline.Id) }
    }
    itemView.discipline_plate?.setOnClickListener { onClickName(discipline.Id) }
    itemView.gates_list?.let { gateListView ->
      gateListView.removeAllViews()
      competitionGates.forEach { competitionGateNo ->
        val inflater = LayoutInflater.from(itemView.context)
        val view = inflater.inflate(R.layout.discipline_gate_item, null) as ViewGroup
        var gateView: TextView? = null

        discipline.Gates.find { disciplineGateNo -> disciplineGateNo == competitionGateNo }?.let {
          gateView = view.gate_selected
        } ?: run {
          gateView = view.gate_unselected
        }

        (gateView?.parent as ViewGroup).removeView(gateView)
        gateView?.text = competitionGateNo.toString()
        gateView?.setOnClickListener { onClickGate(discipline.Id, competitionGateNo) }
        gateListView.addView(gateView)
      }
    }
  }
}